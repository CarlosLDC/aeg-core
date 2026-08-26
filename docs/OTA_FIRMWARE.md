# Guía Postman: Consulta y Descarga OTA de Firmware

**URL Base:** `https://core-xgfvw.ondigitalocean.app`

Esta guía describe los pasos en Postman para iniciar sesión, consultar la información de la última versión de firmware (con su hash MD5 para verificación) y descargar el binario correspondente.

---

## 1. Iniciar Sesión (Login)

1. En Postman, abre una nueva pestaña (**`+`**).
2. Configura la petición:
   - **Método:** `POST`
   - **URL:** 
     ```text
     https://core-xgfvw.ondigitalocean.app/api/auth/login
     ```
3. Ve a la pestaña **Body** → selecciona **raw** → formato **JSON**.
4. Ingresa tus credenciales:
   ```json
   {
     "username": "tu_usuario_o_email",
     "password": "tu_password"
   }
   ```
5. Haz clic en **Send**.
6. De la respuesta recibida, copia el valor de `"token"`:
   ```json
   {
     "token": "eyJhbGciOiJIUzI1NiIsIn..."
   }
   ```

---

## 2. Consultar Última Versión de Firmware (Metadata y Checksum)

1. Abre una nueva pestaña (**`+`**).
2. Configura la petición:
   - **Método:** `GET`
   - **URL:** 
     ```text
     https://core-xgfvw.ondigitalocean.app/ota/latest?model=AEG-R1
     ```
     *(Reemplaza `AEG-R1` por el código del modelo correspondiente).*
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
   - **`firmVer`**: Versión actual disponible del firmware.
   - **`checksum`**: Hash MD5 (32 caracteres hexadecimales) que valida la integridad del binario tras la descarga.
   - **`fileSize`**: Tamaño en bytes del archivo binario.

---

## 3. Descargar Binario de Firmware (.bin)

1. Abre una nueva pestaña (**`+`**).
2. Configura la petición:
   - **Método:** `GET`
   - **URL:** 
     ```text
     https://core-xgfvw.ondigitalocean.app/ota/download?model=AEG-R1&token=<TU_OTA_DOWNLOAD_TOKEN>
     ```
     - Parámetro `model`: Código del modelo (ej. `AEG-R1`).
     - Parámetro `token`: Token estático de descarga configurado en el servidor.
3. Haz clic en **Send**.
4. Para guardar el binario en tu computadora: en el panel de respuesta de Postman, haz clic en **Save response → Save to a file** (nombrarlo por ejemplo `firmware.bin`).
