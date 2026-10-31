# Protocolo MQTT / USB — Periféricos y personalización del ticket

> **Estado:** PROPUESTA (borrador para revisión con firmware). Nada de esto está implementado en firmware ni en Core.
> El panel admin (`/tools/printers/{serial}/perifericos` y `/personalizacion-ticket`) funciona hoy con **datos simulados**.

Cubre cuatro funcionalidades solicitadas por distribuidores:

1. Configuración de la **gaveta de dinero** (cash drawer).
2. Configuración del **visor de precios** (customer display / VFD).
3. **Logo** en el ticket (subir, cambiar, quitar).
4. **Estilos de fuente** en líneas libres del ticket (negrita, tamaño, subrayado, alineación).

Se sigue la misma convención que el resto de Tools (`cmd` + `data`, respuesta con `code`, `dataS` / `dataD`).
Los mismos JSON viajan por **USB serial** (mismo framing que hoy usa `tools-command-builder`).

---

## Topics

Idénticos al resto de Tools:

| Dirección | Topic |
|-----------|-------|
| Servidor → impresora | `/{MacAddress}/AEG_Fiscal/Integracion/Comando` |
| Impresora → servidor | `/{MacAddress}/AEG_Fiscal/Integracion/Respuesta` |

## Principios de diseño

- **Reutilizar `wFileSPIFF`** para persistir configuración: el firmware ya sabe escribir archivos SPIFFS con `Access`. Solo debe *leerlos y aplicarlos*.
- **Retrocompatible:** las claves nuevas son adicionales. Un firmware viejo ignora claves desconocidas y sigue imprimiendo igual.
- **Cuerpo fiscal intocable:** los estilos solo se aplican a líneas libres (encabezado fijo y pie fijo). Ítems, impuestos, totales, serial fiscal y MH los sigue formateando el firmware según norma.
- **Detección de capacidades:** Core debe saber qué soporta cada firmware antes de mostrar las opciones (ver §0).

---

## 0. Capacidades del firmware

Petición:

```json
{ "cmd": "StaInf", "data": { "status": "staCapac" } }
```

Respuesta:

```json
{
  "cmd": "StaInf",
  "code": 0,
  "dataS": "{\"gaveta\":1,\"visor\":1,\"logo\":1,\"estilos\":1,\"anchoPapelPx\":384,\"logoMaxAlto\":200}"
}
```

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `gaveta` / `visor` / `logo` / `estilos` | 0/1 | Funcionalidad soportada por este firmware/hardware |
| `anchoPapelPx` | int | Ancho imprimible en puntos (384 = 58 mm, 576 = 80 mm) |
| `logoMaxAlto` | int | Alto máximo del logo en puntos |

Si la impresora responde con error o no conoce `staCapac`, Core asume **todo en 0** y la UI oculta o deshabilita las secciones.

---

## 1. Periféricos (gaveta + visor)

### 1.1 Leer configuración

```json
{ "cmd": "StaInf", "data": { "status": "staPerif" } }
```

Respuesta (`dataS` = contenido de `perifSPIFF.json`, o `"SIN CONFIGURACION"` si no existe):

```json
{
  "cmd": "StaInf",
  "code": 0,
  "dataS": "{\"gaveta\":{...},\"visor\":{...}}"
}
```

### 1.2 Escribir configuración

```json
{
  "cmd": "wFileSPIFF",
  "data": {
    "Access": "AeG-1968-2024",
    "nameFile": "perifSPIFF.json",
    "contenido": {
      "gaveta": {
        "activo": 1,
        "pin": 2,
        "tOn": 120,
        "tOff": 240,
        "modo": "FP",
        "fpAbre": [1, 11, 12]
      },
      "visor": {
        "activo": 1,
        "proto": "ESCPOS",
        "baud": 9600,
        "bienv": ["  BIENVENIDO A", "  MI COMERCIO"],
        "verItem": 1,
        "verTotal": 1,
        "verVuelto": 1,
        "brillo": 4
      }
    }
  }
}
```

Respuesta: igual que el `wFileSPIFF` actual (`code: 0` = OK). **El firmware debe aplicar los cambios sin reiniciar** (o indicarlo en `dataS: "REINICIO REQUERIDO"`).

#### Gaveta

| Campo | Tipo | Rango | Descripción |
|-------|------|-------|-------------|
| `activo` | 0/1 | | Habilita el pulso de apertura |
| `pin` | int | 2 \| 5 | Pin del conector RJ11/RJ12 (`ESC p m`: m=0 → pin 2, m=1 → pin 5) |
| `tOn` | int (ms) | 10–510, múltiplo de 2 | Duración del pulso (`ESC p` t1 × 2 ms) |
| `tOff` | int (ms) | 10–510, múltiplo de 2 | Pausa posterior (`ESC p` t2 × 2 ms) |
| `modo` | string | `NUNCA` \| `SIEMPRE` \| `FP` | Cuándo abrir al cerrar un documento fiscal |
| `fpAbre` | int[] | 1–16 | Formas de pago (`descFP` nro) que disparan la apertura cuando `modo = FP` |

Reglas:

- Con `modo = SIEMPRE` se abre al finalizar factura (`endFac`) y nota de crédito (`endNC`).
- Con `modo = FP` se abre si **alguno** de los pagos del documento está en `fpAbre`.
- **Nunca** abrir en documentos de prueba ni en reimpresiones.

#### Visor

| Campo | Tipo | Rango | Descripción |
|-------|------|-------|-------------|
| `activo` | 0/1 | | |
| `proto` | string | `ESCPOS` \| `CD5220` \| `EPSON_DMD` | Juego de comandos del visor |
| `baud` | int | 2400 \| 4800 \| 9600 \| 19200 | Velocidad del puerto del visor |
| `bienv` | string[2] | máx. 20 caracteres c/u | Mensaje en reposo (2×20) |
| `verItem` | 0/1 | | Mostrar descripción + precio del último ítem |
| `verTotal` | 0/1 | | Mostrar total al iniciar el pago |
| `verVuelto` | 0/1 | | Mostrar vuelto al cerrar |
| `brillo` | int | 1–4 | Si el protocolo lo soporta |

> [!NOTE]
> Si el visor va conectado al **PC del punto de venta** (y no a la impresora), esta configuración no aplica: es responsabilidad del software POS. Confirmar con los distribuidores qué topología usan.

### 1.3 Probar la gaveta

```json
{ "cmd": "abrirGav", "data": { "pin": 2, "tOn": 120, "tOff": 240 } }
```

`data` es opcional; sin `data` usa la configuración guardada. Respuesta:

```json
{ "cmd": "abrirGav", "code": 0, "dataS": "GAVETA ABIERTA" }
```

Si el hardware tiene sensor de estado de gaveta: `dataD: 1` = abierta, `0` = cerrada (opcional).

### 1.4 Probar el visor

```json
{ "cmd": "visorMsj", "data": { "lin1": "PRUEBA DE VISOR", "lin2": "TOTAL      123,45" } }
```

Respuesta: `{ "cmd": "visorMsj", "code": 0 }`. Pasados 10 s el visor vuelve al mensaje `bienv`.

---

## 2. Estilos de fuente en encabezado y pie

### 2.1 Modelo de estilo por línea

```json
{ "neg": 1, "alto": 2, "ancho": 1, "sub": 0, "alin": "C" }
```

| Campo | Valores | ESC/POS |
|-------|---------|---------|
| `neg` | 0/1 | `ESC E n` |
| `alto` | 1/2 | `GS ! n` (bits 0–3) |
| `ancho` | 1/2 | `GS ! n` (bits 4–7) |
| `sub` | 0/1/2 | `ESC - n` (2 = doble) |
| `alin` | `L` \| `C` \| `R` | `ESC a n` |

El firmware debe **restablecer el estilo** (`ESC @` o `GS ! 0` + `ESC E 0` + `ESC - 0` + `ESC a 0`) tras cada línea libre, para que no se filtre al cuerpo fiscal.

**Validación:** con `ancho = 2` el máximo por línea pasa de 50 a **25 caracteres**. Core y la UI validan; el firmware debe truncar como defensa.

### 2.2 Escribir estilos

Arrays **paralelos** a las líneas existentes, en `paramFacSPIFF.json` (mismo archivo que ya usa `encFacFijo`):

```json
{
  "cmd": "wFileSPIFF",
  "data": {
    "Access": "AeG-1968-2024",
    "nameFile": "paramFacSPIFF.json",
    "contenido": {
      "encFacFijoEst": [
        { "neg": 1, "alto": 2, "ancho": 2, "sub": 0, "alin": "C" },
        { "neg": 0, "alto": 1, "ancho": 1, "sub": 0, "alin": "C" }
      ],
      "pieFijoEst": [
        { "neg": 1, "alto": 1, "ancho": 1, "sub": 0, "alin": "C" }
      ]
    }
  }
}
```

- `encFacFijoEst[i]` aplica a `encFacFijo[i]`; `pieFijoEst[i]` a la línea `i` del pie (`pieTiF`).
- Faltan entradas o el array no existe → estilo por defecto `{neg:0, alto:1, ancho:1, sub:0, alin:"L"}`.
- **Pregunta para firmware:** ¿`wFileSPIFF` sobre `paramFacSPIFF.json` hace *merge* de claves o reemplaza el archivo completo? Si reemplaza, Core debe enviar también `encFacFijo` en el mismo mensaje (hoy solo envía `encFacFijo`, lo que sugiere merge).

### 2.3 Leer estilos

```json
{ "cmd": "StaInf", "data": { "status": "staEstTicket" } }
```

```json
{
  "cmd": "StaInf",
  "code": 0,
  "dataS": "{\"enc\":[{\"neg\":1,\"alto\":2,\"ancho\":2,\"sub\":0,\"alin\":\"C\"}],\"pie\":[]}"
}
```

---

## 3. Logo

### 3.1 Formato

- Raster **1 bit por punto**, filas de arriba a abajo, **MSB primero**, `1` = punto negro (idéntico al formato de `GS v 0`).
- `ancho` múltiplo de 8 y ≤ `anchoPapelPx`; `alto` ≤ `logoMaxAlto`.
- Tamaño en bytes = `(ancho / 8) × alto`. Ejemplo: 384 × 120 = 5.760 bytes.
- La conversión (escala, escala de grises, dithering Floyd–Steinberg o umbral) la hace **el cliente** (admin). El firmware solo recibe el raster listo.
- El firmware lo guarda en SPIFFS como `logo.bin` con una cabecera propia (ancho, alto, crc32).

### 3.2 Subir logo (fragmentado)

Para no exceder el buffer MQTT / serial, se envía en bloques de **1024 bytes** de raster (≈1.368 caracteres en base64):

```json
{
  "cmd": "wLogo",
  "data": {
    "Access": "AeG-1968-2024",
    "seq": 0,
    "total": 6,
    "ancho": 384,
    "alto": 120,
    "crc32": "9A3F12C4",
    "datos": "AAAA//8A..."
  }
}
```

| Campo | Descripción |
|-------|-------------|
| `seq` | Índice de bloque, 0-based |
| `total` | Número total de bloques |
| `ancho`, `alto`, `crc32` | Se envían en **todos** los bloques (permite validar desde el primero). CRC-32 IEEE del raster completo, hex en mayúsculas |
| `datos` | Bloque en base64 |

ACK por bloque (Core espera el ACK antes de enviar el siguiente):

```json
{ "cmd": "wLogo", "code": 0, "dataD": 0 }
```

ACK final (tras `seq = total - 1`, el firmware verifica el CRC y persiste de forma atómica):

```json
{ "cmd": "wLogo", "code": 0, "dataD": 5, "dataS": "LOGO GUARDADO" }
```

Errores (`code ≠ 0`): ver §4. Ante cualquier error el firmware **descarta** el buffer parcial y conserva el logo anterior.

### 3.3 Activar / posicionar

En `paramFacSPIFF.json`:

```json
{ "logo": { "activo": 1, "alin": "C" } }
```

Se imprime **antes** de la primera línea de `encFacFijo`.

> [!IMPORTANT]
> Antes de habilitar en producción, validar con el área legal que la normativa SENIAT vigente para máquinas fiscales permite el logo en documentos fiscales y en qué posición. Si solo se permite en documentos no fiscales, se añadiría `logo.soloNoFiscal: 1`.

### 3.4 Consultar logo

```json
{ "cmd": "StaInf", "data": { "status": "staLogo" } }
```

```json
{
  "cmd": "StaInf",
  "code": 0,
  "dataS": "{\"existe\":1,\"ancho\":384,\"alto\":120,\"crc32\":\"9A3F12C4\",\"activo\":1,\"alin\":\"C\"}"
}
```

No se descarga el bitmap: Core conserva la última copia subida para la vista previa y compara el `crc32` para detectar si el logo del equipo cambió por otra vía (por ejemplo, USB).

### 3.5 Quitar logo

```json
{ "cmd": "delLogo", "data": { "Access": "AeG-1968-2024" } }
```

Respuesta `{ "cmd": "delLogo", "code": 0 }`.

---

## 4. Códigos de error propuestos

| `code` | Significado |
|--------|-------------|
| 0 | OK |
| 1 | Error genérico (`dataS` con detalle) |
| 20 | Funcionalidad no soportada por el hardware/firmware |
| 21 | Parámetro fuera de rango |
| 22 | `Access` inválido |
| 30 | Logo: bloque fuera de secuencia |
| 31 | Logo: CRC no coincide |
| 32 | Logo: dimensiones exceden el máximo |
| 33 | Logo: sin espacio en SPIFFS |
| 40 | Gaveta: periférico no detectado |
| 41 | Visor: periférico no detectado |
| 50 | Operación no permitida con documento fiscal abierto |

---

## 5. Endpoints REST en Core (a implementar)

Bajo `/api/mqtt/tools` (mismo controlador `ToolsMqttController`, mismo `ToolsPrinterRequest { printerId }`):

| Método | Ruta | Comando MQTT |
|--------|------|--------------|
| POST | `/capabilities` | `StaInf staCapac` |
| POST | `/peripherals/read` | `StaInf staPerif` |
| POST | `/peripherals/write` | `wFileSPIFF perifSPIFF.json` |
| POST | `/peripherals/drawer/test` | `abrirGav` |
| POST | `/peripherals/display/test` | `visorMsj` |
| POST | `/ticket-style/read` | `StaInf staEstTicket` |
| POST | `/ticket-style/write` | `wFileSPIFF paramFacSPIFF.json` (`encFacFijoEst`, `pieFijoEst`) |
| POST | `/logo/read` | `StaInf staLogo` |
| POST | `/logo/write` | `wLogo` ×N + `wFileSPIFF` (`logo.activo/alin`) |
| POST | `/logo/delete` | `delLogo` |

Nuevas constantes en `ToolsMqttConstants`: `STA_CAPAC`, `STA_PERIF`, `STA_EST_TICKET`, `STA_LOGO`, `CMD_ABRIR_GAV`, `CMD_VISOR_MSJ`, `CMD_W_LOGO`, `CMD_DEL_LOGO`, `PERIF_SPIFF_FILE`, `LOGO_CHUNK_BYTES = 1024`.

### Persistencia opcional en Core

Tabla `printer_ticket_settings` (1:1 con `impresoras`): JSON de periféricos, estilos, logo (`bytea` + ancho/alto/crc32), `updated_by`, `updated_at`. Sirve para auditoría, vista previa sin consultar el equipo y para volver a aplicar la configuración tras cambiar la impresora o actualizar el firmware.

---

## 6. Preguntas abiertas para firmware

1. ¿El hardware actual tiene conector de gaveta (RJ11/RJ12) y puerto para visor? ¿En qué modelos?
2. ¿`wFileSPIFF` hace merge o reemplaza el archivo? (afecta a §2.2 y §3.3)
3. Tamaño máximo de payload MQTT/serial aceptado → confirmar el bloque de 1024 bytes.
4. Espacio libre en SPIFFS para `logo.bin` (hasta ~14 KB a 576 × 200).
5. Ancho real de papel por modelo (58 / 80 mm) y si el firmware ya imprime raster con `GS v 0`.
6. ¿Los cambios se aplican en caliente o requieren reinicio?
7. ¿Hay sensor de estado de gaveta (`DLE EOT 1`)?
