# Guía Postman: Consulta y Descarga OTA de Firmware

**URL Base:** `https://core-xgfvw.ondigitalocean.app`

Esta guía describe los pasos en Postman para consultar la información de la última versión de firmware (con su hash MD5 para verificación) y descargar el binario correspondiente para dispositivos (ej. ESP32).

> **Nota de Autenticación:**  
> Los endpoints OTA están protegidos mediante un token secreto estático configurado en el servidor (`APP_OTA_DOWNLOAD_TOKEN`).  
> No requieren inicio de sesión con usuario/contraseña (JWT), sino que deben incluir el header **`x-auth-token`** en cada solicitud.

---

## 1. Consultar Última Versión de Firmware (Metadata y Checksum)

1. En Postman, abre una nueva pestaña (**`+`**).
2. Configura la petición:
   - **Método:** `GET`
   - **URL:** 
     ```text
     https://core-xgfvw.ondigitalocean.app/ota/latest?model=AEG-R1
     ```
     - **Params:**
       - `model`: Código del modelo de la impresora (ej. `AEG-R1`).
   - **Headers:**
     - Key: `x-auth-token`
     - Value: `<TU_OTA_DOWNLOAD_TOKEN>`
3. Haz clic en **Send**.
4. Respuesta esperada (`200 OK`):
   ```json
   {
     "modelPtr": "AEG-R1",
     "hardVer": null,
     "firmVer": "1.1.1",
     "buildDate": 1787692287,
     "checksum": "e2fc714c4727ee9395f324cd2e7f331f",
     "fileSize": 1673664
   }
   ```
   - **`modelPtr`**: Modelo para el cual se obtuvo la versión.
   - **`firmVer`**: Versión actual disponible del firmware.
   - **`checksum`**: Hash MD5 (32 caracteres hexadecimales) que valida la integridad del binario tras la descarga.
   - **`buildDate`**: Unix timestamp (segundos) de la fecha de creación/subida del firmware.
   - **`fileSize`**: Tamaño en bytes del archivo binario.

---

## 2. Descargar Binario de Firmware (.bin)

1. Abre una nueva pestaña (**`+`**).
2. Configura la petición:
   - **Método:** `GET`
   - **URL:** 
     ```text
     https://core-xgfvw.ondigitalocean.app/ota/download?model=AEG-R1
     ```
     - **Params:**
       - `model`: Código del modelo (ej. `AEG-R1`).
   - **Headers:**
     - Key: `x-auth-token`
     - Value: `<TU_OTA_DOWNLOAD_TOKEN>`
3. Haz clic en **Send**.
4. Para guardar el binario en tu computadora: en el panel de respuesta de Postman, haz clic en **Save response → Save to a file** (nombrarlo por ejemplo `firmware.bin`).

---

## Códigos de Respuesta HTTP

| Código | Descripción | Causa común |
| :--- | :--- | :--- |
| `200 OK` | Operación exitosa | Retorna el JSON de metadatos o el stream del binario `.bin`. |
| `401 Unauthorized` | Autenticación fallida | El header `x-auth-token` está ausente, vacío o no coincide con el token secreto del servidor. |
| `404 Not Found` | No encontrado | No existe ningún firmware registrado para el modelo especificado. |
