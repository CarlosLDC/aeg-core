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
2. MAC no existe (única por impresora; índice `impresoras_direccion_mac_compact_uq`) → `"Mac Address de Impresora ya Existe"`
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
  "data": { "msj": "Impresora Lista a Fiscalizar", "Access": "config" }
}
```

### Paso 4 — Resultado físico (impresora → Respuesta)

Puede tardar **más de 1 minuto** (~90s). Timeout configurado: `app.mqtt.fiscalizacion.timeout.result-seconds` (default 180).

**Éxito** — Core crea la impresora `SIN_ASIGNAR`, asigna el precinto (`EN_IMPRESORA`) y procede al Paso 5:

```json
{
  "cmd": "RxPtrFiscalizarRemoto",
  "code": 0,
  "dataD": 0
}
```
*(También se acepta `dataS: { "error": "Impresora Fiscalizando" }`).*

**Error:**

```json
{
  "cmd": "RxPtrFiscalizarRemoto",
  "code": 1,
  "dataS": { "error": "ERROR Fiscalizando" }
}
```

### Paso 5 — Configuración de impuestos y pagos (servidor → Comando / Respuesta)

Tras el resultado físico exitoso, Core envía automáticamente la plantilla fija de impuestos y medios de pago `configSPIFFS.json` a Comando:

```json
{
  "cmd": "wFileSPIFF",
  "data": {
    "nameFile": "configSPIFFS.json",
    "contenido": {
      "simMonL": "Bs",
      "impArt": {
        "desc": ["Exonerado", "IVA", "Reducido", "Lujo", "Percibido"],
        "abrev": ["(E)", "(G)", "(R)", "(A)", "(P)"],
        "valor": [0, 1600, 800, 3100, 0],
        "impMontoPtr": [
          "EXENTO (E)",
          "BI G (16.00%)",
          "BI R (8.00%)",
          "BI A (31.00%)",
          "PERCIBIDO"
        ],
        "impMontoImp": ["", "IVA G (16.00%)", "IVA R (8.00%)", "IVA A (31.00%)", ""]
      },
      "formPago": {
        "tituloFormPag": "FORMA DE PAGO",
        "desc": [
          "EFECTIVO", "T. DEBITO", "T. CREDITO", "TRANSFERENCIA", "PAGO MOVIL", "BIOPAGO",
          "EFECTIVO 7", "EFECTIVO 8", "EFECTIVO 9", "EFECTIVO 10",
          "DIVISA 1", "DIVISA 2", "DIVISA 3", "DIVISA 4", "DIVISA 5", "DIVISA 6"
        ],
        "impG": [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 300, 300, 300, 300, 300, 300],
        "impMontoPtr": [
          "", "", "", "", "", "", "", "", "", "",
          "BI IGTF (3.00%)", "BI IGTF (3.00%)", "BI IGTF (3.00%)",
          "BI IGTF (3.00%)", "BI IGTF (3.00%)", "BI IGTF (3.00%)"
        ],
        "impMontoImp": [
          "", "", "", "", "", "", "", "", "", "",
          "IGTF (3.00%)", "IGTF (3.00%)", "IGTF (3.00%)",
          "IGTF (3.00%)", "IGTF (3.00%)", "IGTF (3.00%)"
        ]
      }
    }
  }
}
```

La impresora responde en Respuesta:

```json
{
  "cmd": "wFileSPIFF",
  "code": 0,
  "dataD": 0
}
```

Timeout configurado: `app.mqtt.fiscalizacion.timeout.config-seconds` (default 60). Al recibir `code: 0`, Core marca la sesión como **`COMPLETED`**.

---

## 2–4. Operaciones posteriores (Tools)

Tras el alta y configuración inicial, usar Tools / Remoto para:

2. `StaInf` — consulta de registro
3. Factura de prueba (`proF` / `subToF` / `fpaF` / `endFac`)
4. `genImpRepZ` — Reporte Z

---

## Implementación en AEG Core

| Capacidad | Detalle |
|-----------|---------|
| Flag | `app.mqtt.fiscalizacion.enabled` / `MQTT_FISCALIZACION_ENABLED` |
| Timeout resultado físico | `app.mqtt.fiscalizacion.timeout.result-seconds` (default 180) |
| Timeout configuración SPIFFS | `app.mqtt.fiscalizacion.timeout.config-seconds` (default 60) |
| Admin API | `/api/mqtt/fiscalizacion/sessions`, `/activity`, `/stream` |
| Panel | Remoto → pestaña Fiscalización |
| Simulador | `scripts/fiscalizacion_printer_simulator.py` |

Estado final al éxito: `PrinterStatus.SIN_ASIGNAR`, precinto `SealStatus.EN_IMPRESORA`, SPIFFS cargado.
