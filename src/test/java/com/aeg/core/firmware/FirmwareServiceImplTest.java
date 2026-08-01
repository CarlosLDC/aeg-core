package com.aeg.core.firmware;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import com.aeg.core.firmware.dto.FirmwareResponse;
import com.aeg.core.firmware.dto.FirmwareUpdateRequest;
import com.aeg.core.firmware.storage.FirmwareStorage;
import com.aeg.core.printermodel.PrinterModel;
import com.aeg.core.printermodel.PrinterModelRepository;
import com.aeg.core.servicecenter.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
class FirmwareServiceImplTest {

	@Mock
	FirmwareRepository repository;
	@Mock
	PrinterModelRepository printerModelRepository;
	@Mock
	FirmwareStorage storage;

	FirmwareServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new FirmwareServiceImpl(
				repository,
				printerModelRepository,
				storage,
				"http://206.189.231.128/downloads");
	}

	@Test
	void createUploadsThenPersistsMetadata() throws Exception {
		byte[] payload = "firmware-bytes".getBytes(StandardCharsets.UTF_8);
		MockMultipartFile file = new MockMultipartFile("file", "aeg-1.0.0.bin", "application/octet-stream", payload);

		when(repository.existsByFileName("aeg-1.0.0.bin")).thenReturn(false);
		when(repository.existsByVersionAndPrinterModelIsNull("1.0.0")).thenReturn(false);
		when(repository.save(any(Firmware.class))).thenAnswer(invocation -> {
			Firmware f = invocation.getArgument(0);
			f.setId(42L);
			return f;
		});

		FirmwareResponse response = service.create(file, "1.0.0", null, "first release");

		verify(storage).upload(eq("aeg-1.0.0.bin"), any(InputStream.class), eq((long) payload.length));
		ArgumentCaptor<Firmware> captor = ArgumentCaptor.forClass(Firmware.class);
		verify(repository).save(captor.capture());
		assertThat(captor.getValue().getChecksumSha256()).isEqualTo(FirmwareServiceImpl.sha256Hex(payload));
		assertThat(response.id()).isEqualTo(42L);
		assertThat(response.downloadUrl()).isEqualTo("http://206.189.231.128/downloads/aeg-1.0.0.bin");
		assertThat(response.version()).isEqualTo("1.0.0");
		assertThat(response.notes()).isEqualTo("first release");
	}

	@Test
	void createRejectsDuplicateVersion() {
		MockMultipartFile file = new MockMultipartFile("file", "aeg-1.0.0.bin", "application/octet-stream", new byte[] {1});
		when(repository.existsByFileName("aeg-1.0.0.bin")).thenReturn(false);
		when(repository.existsByVersionAndPrinterModelIsNull("1.0.0")).thenReturn(true);

		assertThatThrownBy(() -> service.create(file, "1.0.0", null, null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("already exists");
		verify(storage, never()).upload(anyString(), any(), anyLong());
	}

	@Test
	void createCleansRemoteWhenPersistFails() {
		byte[] payload = new byte[] {9, 8, 7};
		MockMultipartFile file = new MockMultipartFile("file", "aeg-2.0.0.bin", "application/octet-stream", payload);
		when(repository.existsByFileName("aeg-2.0.0.bin")).thenReturn(false);
		when(repository.existsByVersionAndPrinterModelIsNull("2.0.0")).thenReturn(false);
		when(repository.save(any(Firmware.class))).thenThrow(new RuntimeException("db down"));

		assertThatThrownBy(() -> service.create(file, "2.0.0", null, null))
				.isInstanceOf(RuntimeException.class)
				.hasMessageContaining("db down");
		verify(storage).delete("aeg-2.0.0.bin");
	}

	@Test
	void createDoesNotPersistWhenUploadFails() {
		byte[] payload = new byte[] {1, 2, 3};
		MockMultipartFile file = new MockMultipartFile(
				"file", "fail-upload.bin", "application/octet-stream", payload);
		when(repository.existsByFileName("fail-upload.bin")).thenReturn(false);
		when(repository.existsByVersionAndPrinterModelIsNull("4.0.0")).thenReturn(false);
		doThrow(new IllegalStateException("sftp upload down"))
				.when(storage)
				.upload(eq("fail-upload.bin"), any(InputStream.class), eq((long) payload.length));

		assertThatThrownBy(() -> service.create(file, "4.0.0", null, null))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("sftp upload down");
		verify(repository, never()).save(any(Firmware.class));
		verify(storage, never()).delete(anyString());
	}

	@Test
	void createResolvesPrinterModel() {
		byte[] payload = new byte[] {1};
		MockMultipartFile file = new MockMultipartFile("file", "model-fw.bin", "application/octet-stream", payload);
		PrinterModel model = new PrinterModel();
		model.setId(7L);

		when(repository.existsByFileName("model-fw.bin")).thenReturn(false);
		when(repository.existsByVersionAndPrinterModel_Id("3.0.0", 7L)).thenReturn(false);
		when(printerModelRepository.findById(7L)).thenReturn(Optional.of(model));
		when(repository.save(any(Firmware.class))).thenAnswer(invocation -> {
			Firmware f = invocation.getArgument(0);
			f.setId(1L);
			return f;
		});

		FirmwareResponse response = service.create(file, "3.0.0", 7L, null);
		assertThat(response.printerModelId()).isEqualTo(7L);
	}

	@Test
	void downloadReturnsOctetStream() {
		Firmware entity = new Firmware();
		entity.setId(5L);
		entity.setFileName("x.bin");
		entity.setVersion("1.0.0");
		entity.setSizeBytes(3L);
		entity.setChecksumSha256("abc");
		when(repository.findById(5L)).thenReturn(Optional.of(entity));
		when(storage.download("x.bin")).thenReturn(new byte[] {1, 2, 3});

		ResponseEntity<Resource> response = service.download(5L);
		assertThat(response.getStatusCode().value()).isEqualTo(200);
		assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
				.contains("x.bin");
		assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_OCTET_STREAM);
		assertThat(response.getHeaders().getContentLength()).isEqualTo(3L);
	}

	@Test
	void deleteRemovesRemoteThenPersistedRow() {
		Firmware entity = new Firmware();
		entity.setId(8L);
		entity.setFileName("keep-sync.bin");
		when(repository.findById(8L)).thenReturn(Optional.of(entity));

		service.delete(8L);

		var inOrder = org.mockito.Mockito.inOrder(storage, repository);
		inOrder.verify(storage).delete("keep-sync.bin");
		inOrder.verify(repository).delete(entity);
	}

	@Test
	void deleteDoesNotRemoveRowWhenRemoteFails() {
		Firmware entity = new Firmware();
		entity.setId(9L);
		entity.setFileName("gone.bin");
		when(repository.findById(9L)).thenReturn(Optional.of(entity));
		doThrow(new IllegalStateException("sftp down")).when(storage).delete("gone.bin");

		assertThatThrownBy(() -> service.delete(9L))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("sftp down");
		verify(repository, never()).delete(any());
	}

	@Test
	void findByIdMissingThrows() {
		when(repository.findById(99L)).thenReturn(Optional.empty());
		assertThatThrownBy(() -> service.findById(99L))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void updateChangesMetadataWithoutTouchingStorage() {
		Firmware entity = new Firmware();
		entity.setId(11L);
		entity.setVersion("1.0.0");
		entity.setFileName("aeg-1.0.0.bin");
		entity.setSizeBytes(10L);
		entity.setChecksumSha256("abc");
		entity.setNotes("old");

		PrinterModel model = new PrinterModel();
		model.setId(7L);

		when(repository.findById(11L)).thenReturn(Optional.of(entity));
		when(repository.existsByVersionAndPrinterModel_IdAndIdNot("2.0.0", 7L, 11L)).thenReturn(false);
		when(printerModelRepository.findById(7L)).thenReturn(Optional.of(model));
		when(repository.save(any(Firmware.class))).thenAnswer(invocation -> invocation.getArgument(0));

		FirmwareResponse response = service.update(
				11L,
				new FirmwareUpdateRequest("2.0.0", 7L, "release notes"));

		assertThat(response.version()).isEqualTo("2.0.0");
		assertThat(response.printerModelId()).isEqualTo(7L);
		assertThat(response.notes()).isEqualTo("release notes");
		assertThat(response.fileName()).isEqualTo("aeg-1.0.0.bin");
		verify(storage, never()).upload(anyString(), any(), anyLong());
		verify(storage, never()).delete(anyString());
	}

	@Test
	void updateRejectsDuplicateVersionForOtherRow() {
		Firmware entity = new Firmware();
		entity.setId(12L);
		entity.setVersion("1.0.0");
		entity.setFileName("a.bin");
		entity.setSizeBytes(1L);
		entity.setChecksumSha256("x");

		when(repository.findById(12L)).thenReturn(Optional.of(entity));
		when(repository.existsByVersionAndPrinterModelIsNullAndIdNot("1.1.0", 12L)).thenReturn(true);

		assertThatThrownBy(() -> service.update(12L, new FirmwareUpdateRequest("1.1.0", null, null)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("already exists");
		verify(repository, never()).save(any(Firmware.class));
	}

	@Test
	void findAllFiltersByModel() {
		when(repository.findByPrinterModel_IdOrderByCreatedAtDesc(3L)).thenReturn(List.of());
		assertThat(service.findAll(3L)).isEmpty();
		verify(repository).findByPrinterModel_IdOrderByCreatedAtDesc(3L);
	}
}
