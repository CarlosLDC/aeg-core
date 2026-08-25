package com.aeg.core.firmware.ota;

import com.aeg.core.firmware.Firmware;
import com.aeg.core.firmware.FirmwareRepository;
import com.aeg.core.firmware.storage.FirmwareStorage;
import com.aeg.core.servicecenter.ResourceNotFoundException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class OtaFirmwareServiceImpl implements OtaFirmwareService {

    private final FirmwareRepository repository;
    private final FirmwareStorage storage;
    private final String downloadToken;

    public OtaFirmwareServiceImpl(
            FirmwareRepository repository,
            FirmwareStorage storage,
            @Value("${app.ota.download-token}") String downloadToken) {
        this.repository = repository;
        this.storage = storage;
        this.downloadToken = downloadToken;
    }

    @Override
    public OtaMetadataResponse getLatestMetadata(String modelCode) {
        Firmware fw = findLatest(modelCode);
        return toMetadata(modelCode, fw);
    }

    @Override
    public ResponseEntity<Resource> downloadLatest(String modelCode, String token) {
        if (!downloadToken.equals(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token inválido");
        }
        Firmware fw = findLatest(modelCode);
        byte[] bytes = storage.download(fw.getFileName());
        ByteArrayResource resource = new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return fw.getFileName();
            }
        };
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fw.getFileName() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(bytes.length)
                .body(resource);
    }

    // -------------------------------------------------------------------------

    private Firmware findLatest(String modelCode) {
        return repository
                .findFirstByPrinterModel_ModelCodeIgnoreCaseOrderByCreatedAtDesc(modelCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró firmware para el modelo: " + modelCode));
    }

    private static OtaMetadataResponse toMetadata(String modelCode, Firmware fw) {
        long buildDate = fw.getCreatedAt().toEpochSecond();
        return new OtaMetadataResponse(
                modelCode,
                null,                       // hardVer: not yet tracked in the backend
                fw.getVersion(),
                buildDate,
                fw.getChecksumMd5(),
                fw.getSizeBytes());
    }
}
