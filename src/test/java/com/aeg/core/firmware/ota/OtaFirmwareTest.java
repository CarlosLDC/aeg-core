package com.aeg.core.firmware.ota;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import com.aeg.core.firmware.Firmware;
import com.aeg.core.firmware.FirmwareRepository;
import com.aeg.core.firmware.storage.FirmwareStorage;
import com.aeg.core.printermodel.PrinterModel;
import com.aeg.core.servicecenter.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
class OtaFirmwareTest {

    private static final String SECRET_TOKEN = "secret-token-12345";
    private static final String MODEL_CODE = "AEG-R1";

    @Mock
    private FirmwareRepository repository;

    @Mock
    private FirmwareStorage storage;

    private OtaFirmwareServiceImpl service;
    private OtaFirmwareController controller;

    @BeforeEach
    void setUp() {
        service = new OtaFirmwareServiceImpl(repository, storage, SECRET_TOKEN);
        controller = new OtaFirmwareController(service);
    }

    private Firmware createSampleFirmware() {
        PrinterModel model = new PrinterModel();
        model.setModelCode(MODEL_CODE);

        Firmware fw = new Firmware();
        fw.setId(1L);
        fw.setVersion("1.1.0");
        fw.setFileName("aeg_r1_1.1.0.bin");
        fw.setSizeBytes(1024L);
        fw.setChecksumMd5("56951bfa64338b2eeb5a68b7c64e272a");
        fw.setChecksumSha256("abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890");
        fw.setPrinterModel(model);
        fw.setCreatedAt(OffsetDateTime.parse("2026-08-28T10:00:00Z"));
        return fw;
    }

    @Nested
    @DisplayName("OtaFirmwareController tests")
    class ControllerTests {

        @Test
        @DisplayName("Controller getLatest delegates to service")
        void getLatestDelegatesToService() {
            OtaFirmwareService mockService = mock(OtaFirmwareService.class);
            OtaFirmwareController c = new OtaFirmwareController(mockService);
            OtaMetadataResponse expected = new OtaMetadataResponse(MODEL_CODE, null, "1.1.0", 12345L, "md5", 100L);
            when(mockService.getLatestMetadata(MODEL_CODE)).thenReturn(expected);

            OtaMetadataResponse result = c.getLatest(MODEL_CODE);

            assertThat(result).isSameAs(expected);
            verify(mockService).getLatestMetadata(MODEL_CODE);
        }

        @Test
        @DisplayName("Controller download passes header token to service")
        void downloadPassesHeaderTokenToService() {
            OtaFirmwareService mockService = mock(OtaFirmwareService.class);
            OtaFirmwareController c = new OtaFirmwareController(mockService);
            ResponseEntity<Resource> expected = ResponseEntity.ok().build();
            when(mockService.downloadLatest(MODEL_CODE, SECRET_TOKEN)).thenReturn(expected);

            ResponseEntity<Resource> result = c.download(MODEL_CODE, SECRET_TOKEN);

            assertThat(result).isSameAs(expected);
            verify(mockService).downloadLatest(MODEL_CODE, SECRET_TOKEN);
        }
    }

    @Nested
    @DisplayName("OtaFirmwareServiceImpl tests")
    class ServiceTests {

        @Test
        @DisplayName("getLatestMetadata returns metadata for existing firmware")
        void getLatestMetadataSuccess() {
            Firmware fw = createSampleFirmware();
            when(repository.findFirstByPrinterModel_ModelCodeIgnoreCaseOrderByCreatedAtDesc(MODEL_CODE))
                    .thenReturn(Optional.of(fw));

            OtaMetadataResponse metadata = service.getLatestMetadata(MODEL_CODE);

            assertThat(metadata.modelPtr()).isEqualTo(MODEL_CODE);
            assertThat(metadata.firmVer()).isEqualTo("1.1.0");
            assertThat(metadata.checksum()).isEqualTo("56951bfa64338b2eeb5a68b7c64e272a");
            assertThat(metadata.fileSize()).isEqualTo(1024L);
            assertThat(metadata.buildDate()).isEqualTo(fw.getCreatedAt().toEpochSecond());
        }

        @Test
        @DisplayName("getLatestMetadata throws 404 when no firmware is found")
        void getLatestMetadataNotFound() {
            when(repository.findFirstByPrinterModel_ModelCodeIgnoreCaseOrderByCreatedAtDesc(MODEL_CODE))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getLatestMetadata(MODEL_CODE))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("No se encontró firmware para el modelo: " + MODEL_CODE);
        }

        @Test
        @DisplayName("downloadLatest succeeds with valid secret token")
        void downloadLatestSuccess() {
            Firmware fw = createSampleFirmware();
            byte[] binaryContent = "fake binary firmware content".getBytes(StandardCharsets.UTF_8);

            when(repository.findFirstByPrinterModel_ModelCodeIgnoreCaseOrderByCreatedAtDesc(MODEL_CODE))
                    .thenReturn(Optional.of(fw));
            when(storage.download(fw.getFileName())).thenReturn(binaryContent);

            ResponseEntity<Resource> response = service.downloadLatest(MODEL_CODE, SECRET_TOKEN);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_OCTET_STREAM);
            assertThat(response.getHeaders().getFirst("Content-Disposition"))
                    .isEqualTo("attachment; filename=\"aeg_r1_1.1.0.bin\"");
            assertThat(response.getHeaders().getContentLength()).isEqualTo(binaryContent.length);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getFilename()).isEqualTo("aeg_r1_1.1.0.bin");
        }

        @Test
        @DisplayName("downloadLatest throws 401 Unauthorized when token is invalid")
        void downloadLatestInvalidToken() {
            assertThatThrownBy(() -> service.downloadLatest(MODEL_CODE, "wrong-token"))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                        assertThat(rse.getReason()).isEqualTo("Token inválido");
                    });
        }

        @Test
        @DisplayName("downloadLatest throws 401 Unauthorized when token is null")
        void downloadLatestNullToken() {
            assertThatThrownBy(() -> service.downloadLatest(MODEL_CODE, null))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                        assertThat(rse.getReason()).isEqualTo("Token inválido");
                    });
        }

        @Test
        @DisplayName("downloadLatest throws 401 Unauthorized when token is blank")
        void downloadLatestBlankToken() {
            assertThatThrownBy(() -> service.downloadLatest(MODEL_CODE, "   "))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                        assertThat(rse.getReason()).isEqualTo("Token inválido");
                    });
        }

        @Test
        @DisplayName("downloadLatest throws 404 when token is valid but firmware does not exist")
        void downloadLatestModelNotFound() {
            when(repository.findFirstByPrinterModel_ModelCodeIgnoreCaseOrderByCreatedAtDesc(MODEL_CODE))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.downloadLatest(MODEL_CODE, SECRET_TOKEN))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("No se encontró firmware para el modelo: " + MODEL_CODE);
        }
    }
}
