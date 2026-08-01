# Protocolo MQTT — Fiscalización remota AEG

Guía de integración para fiscalizar una impresora fiscal AEG mediante MQTT entre la **impresora**, el **broker** y **AEG Core**.

Tras un proceso exitoso, Core crea la impresora en estado **`SIN_ASIGNAR`** y asigna el precinto (`EN_IMPRESORA`).

Las secciones de configuración fiscal, facturación y Reporte Z (2–5) **no** forman parte del ritual automático de fiscalización; se operan después vía **Tools** / Remoto.

---

## Topics

En todos los topics, `206EF1884C68` es la MAC plana (también aceptada como `20:6E:F1:88:4C:68` en payloads).

| Dirección | Topic |
|-----------|--------|
| Servidor → impresora | `/{MacAddress}/AEG_Fiscal/Integracion/Comando` |
| Impresora → servidor (respuestas) | `/{MacAddress}/AEG_Fiscal/Integracion/Respuesta` |
| Impresora → servidor (inicio) | `/{MacAddress}/AEG_Fiscal/Integracion/CmdServer` |

---

## 1. Proceso de fiscalización automática

Requiere Internet y broker MQTT. **Inicio real:** la impresora publica `ptrFiscalizar` en CmdServer (tras un disparo local USB/HTTP/`ptrFiscalizarRemoto`). El panel Remoto puede **simular** `ptrFiscalizar` para debug.

### Paso 1 — Disparo hacia la impresora (opcional / hardware)

Cliente (USB, HTTP o MQTT Comando):

```json
{
  "cmd": "ptrFiscalizarRemoto",
  "data": {
    "nroRegistro": "GRA0000017",
    "PrecintoNro": "G1B0033",
    "PrecintoColor": "Azul",
    "NroMemFis": 1,
    "Access": "AA "
  }
}
```

### Paso 2 — Solicitud al servidor (impresora → CmdServer)

```json
{
  "cmd": "ptrFiscalizar",
  "data": {
    "ptrReg": "GRA0000017",
    "macAddr": "20:6E:F1:88:4C:68",
    "PrecintoNro": "G1B0033",
    "PrecintoColor": "Azul",
    "firmwareVersion": "1.1.0",
    "model": "AEG-R1"
  }
}
```

### Paso 3 — Validación y ACK (servidor → Comando)

Core verifica:

1. `ptrReg` no existe en impresoras → `"Registro de Impresora ya Existe"`
2. MAC no existe → `"Mac Address de Impresora ya Existe"`
3. Existe precinto con serial `PrecintoNro` → `"Precinto de Impresora no Existe"`
4. Precinto `DISPONIBLE` y sin impresora → `"Precinto de Impresora ya está Asignado"`

**Extensiones AEG Core (documentadas):**

- Color del payload debe coincidir con el color del precinto en BD (mapeo `Azul` → `azul`, etc.); si no, se trata como precinto no válido / no existe.
- `model` se resuelve por `PrinterModel.codigo_modelo`. Si no hay modelo: ACK `code=1` con `"Modelo de Impresora no Existe"`.

#### Error

```json
{
  "cmd": "RxPtrFiscalizarRemoto",
  "code": 1,
  "data": { "msj": "Mensaje de error correspondiente" }
}
```

#### Éxito

```json
{
  "cmd": "RxPtrFiscalizarRemoto",
  "code": 0,
  "data": { "msj": "Impresora Lista a Fiscalizar" }
}
```

### Paso 4 — Resultado (impresora → Respuesta)

Puede tardar **más de 1 minuto**. Timeout configurado: `app.mqtt.fiscalizacion.timeout.result-seconds` (default 180).

**Éxito** — Core crea la impresora `SIN_ASIGNAR` y asigna el precinto:

```json
{
  "cmd": "RxPtrFiscalizarRemoto",
  "code": 0,
  "dataS": { "error": "Impresora Fiscalizando" }
}
```

**Error:**

```json
{
  "cmd": "RxPtrFiscalizarRemoto",
  "code": 1,
  "dataS": { "error": "ERROR Fiscalizando" }
}
```

---

## 2–5. Operaciones posteriores (Tools)

Tras el alta, usar Tools / Remoto para:

2. `wFileSPIFF` (`configSPIFFS.json`) — impuestos y formas de pago  
3. `StaInf` — consulta de registro  
4. Factura de prueba (`proF` / `subToF` / `fpaF` / `endFac`)  
5. `genImpRepZ` — Reporte Z  

No se orquestan en el ritual de fiscalización.

---

## Implementación en AEG Core

| Capacidad | Detalle |
|-----------|---------|
| Flag | `app.mqtt.fiscalizacion.enabled` / `MQTT_FISCALIZACION_ENABLED` |
| Timeout resultado | `app.mqtt.fiscalizacion.timeout.result-seconds` (default 180) |
| Admin API | `/api/mqtt/fiscalizacion/sessions`, `/activity`, `/stream` |
| Panel | Remoto → pestaña Fiscalización |
| Simulador | `scripts/fiscalizacion_printer_simulator.py` |

Estado final al éxito: `PrinterStatus.SIN_ASIGNAR`, precinto `SealStatus.EN_IMPRESORA`.
