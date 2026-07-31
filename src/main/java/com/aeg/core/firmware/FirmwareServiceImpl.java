package com.aeg.core.firmware;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.aeg.core.firmware.dto.FirmwareResponse;
import com.aeg.core.firmware.storage.FirmwareStorage;
import com.aeg.core.printermodel.PrinterModel;
import com.aeg.core.printermodel.PrinterModelRepository;
import com.aeg.core.servicecenter.ResourceNotFoundException;

import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
public class FirmwareServiceImpl implements FirmwareService {

	private static final Pattern VERSION_PATTERN = Pattern.compile("^[0-9]+\\.[0-9]+\\.[0-9]+$");

	private final FirmwareRepository repository;
	private final PrinterModelRepository printerModelRepository;
	private final FirmwareStorage storage;
	private final String publicBaseUrl;

	public FirmwareServiceImpl(
			FirmwareRepository repository,
			PrinterModelRepository printerModelRepository,
			FirmwareStorage storage,
			@Value("${app.firmware.public-base-url}") String publicBaseUrl) {
		this.repository = repository;
		this.printerModelRepository = printerModelRepository;
		this.storage = storage;
		this.publicBaseUrl = publicBaseUrl;
	}

	@Override
	@Transactional(readOnly = true)
	public List<FirmwareResponse> findAll(Long printerModelId) {
		List<Firmware> items = printerModelId == null
				? repository.findAllByOrderByCreatedAtDesc()
				: repository.findByPrinterModel_IdOrderByCreatedAtDesc(printerModelId);
		return items.stream().map(this::toResponse).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public FirmwareResponse findById(Long id) {
		return toResponse(findEntityById(id));
	}

	@Override
	@Transactional(readOnly = true)
	public PreparedFirmwareUpload prepareCreate(
			MultipartFile file,
			String version,
			Long printerModelId,
			String notes) {
		if (file == null || file.isEmpty()) {
			throw new IllegalArgumentException("file is required");
		}
		if (!StringUtils.hasText(version) || !VERSION_PATTERN.matcher(version.trim()).matches()) {
			throw new IllegalArgumentException("version must match N.N.N (e.g. 1.2.3)");
		}
		String normalizedVersion = version.trim();
		String fileName = FirmwareFileNames.sanitize(file.getOriginalFilename());

		if (repository.existsByFileName(fileName)) {
			throw new IllegalArgumentException("Firmware file name already exists: " + fileName);
		}
		assertVersionAvailable(normalizedVersion, printerModelId);

		if (printerModelId != null) {
			printerModelRepository.findById(printerModelId)
					.orElseThrow(() -> new ResourceNotFoundException(
							"Printer model not found with id: " + printerModelId));
		}

		byte[] bytes;
		try {
			bytes = file.getBytes();
		} catch (IOException e) {
			throw new IllegalArgumentException("Could not read uploaded file", e);
		}
		if (bytes.length == 0) {
			throw new IllegalArgumentException("file is required");
		}

		return new PreparedFirmwareUpload(
				bytes,
				fileName,
				normalizedVersion,
				printerModelId,
				StringUtils.hasText(notes) ? notes.trim() : null,
				sha256Hex(bytes));
	}

	@Override
	public FirmwareResponse completeCreate(PreparedFirmwareUpload prepared) {
		PrinterModel model = null;
		if (prepared.printerModelId() != null) {
			model = printerModelRepository.findById(prepared.printerModelId())
					.orElseThrow(() -> new ResourceNotFoundException(
							"Printer model not found with id: " + prepared.printerModelId()));
		}

		// Re-check uniqueness in case of concurrent uploads.
		if (repository.existsByFileName(prepared.fileName())) {
			throw new IllegalArgumentException("Firmware file name already exists: " + prepared.fileName());
		}
		assertVersionAvailable(prepared.version(), prepared.printerModelId());

		boolean uploaded = false;
		try {
			storage.upload(
					prepared.fileName(),
					new java.io.ByteArrayInputStream(prepared.bytes()),
					prepared.bytes().length);
			uploaded = true;

			Firmware entity = new Firmware();
			entity.setVersion(prepared.version());
			entity.setFileName(prepared.fileName());
			entity.setSizeBytes((long) prepared.bytes().length);
			entity.setChecksumSha256(prepared.checksumSha256());
			entity.setPrinterModel(model);
			entity.setNotes(prepared.notes());
			return toResponse(repository.save(entity));
		} catch (RuntimeException e) {
			if (uploaded) {
				try {
					storage.delete(prepared.fileName());
				} catch (RuntimeException cleanup) {
					log.warn("Failed to clean up remote firmware after DB error: {}", prepared.fileName(), cleanup);
				}
			}
			throw e;
		}
	}

	@Override
	public FirmwareResponse create(MultipartFile file, String version, Long printerModelId, String notes) {
		return completeCreate(prepareCreate(file, version, printerModelId, notes));
	}

	@Override
	public void delete(Long id) {
		Firmware entity = findEntityById(id);
		String fileName = entity.getFileName();
		repository.delete(entity);
		try {
			storage.delete(fileName);
		} catch (RuntimeException e) {
			log.warn("Firmware DB row deleted but remote file cleanup failed: {}", fileName, e);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<Resource> download(Long id) {
		Firmware entity = findEntityById(id);
		byte[] bytes = storage.download(entity.getFileName());
		ByteArrayResource resource = new ByteArrayResource(bytes) {
			@Override
			public String getFilename() {
				return entity.getFileName();
			}
		};
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + entity.getFileName() + "\"")
				.contentType(MediaType.APPLICATION_OCTET_STREAM)
				.contentLength(bytes.length)
				.body(resource);
	}

	private void assertVersionAvailable(String version, Long printerModelId) {
		boolean exists = printerModelId == null
				? repository.existsByVersionAndPrinterModelIsNull(version)
				: repository.existsByVersionAndPrinterModel_Id(version, printerModelId);
		if (exists) {
			throw new IllegalArgumentException(
					"Firmware version already exists for the given printer model: " + version);
		}
	}

	private Firmware findEntityById(Long id) {
		return repository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Firmware not found with id: " + id));
	}

	private FirmwareResponse toResponse(Firmware entity) {
		Long modelId = entity.getPrinterModel() == null ? null : entity.getPrinterModel().getId();
		return new FirmwareResponse(
				entity.getId(),
				entity.getVersion(),
				entity.getFileName(),
				entity.getSizeBytes(),
				entity.getChecksumSha256(),
				modelId,
				entity.getNotes(),
				FirmwareFileNames.buildPublicUrl(publicBaseUrl, entity.getFileName()),
				entity.getCreatedAt());
	}

	static String sha256Hex(byte[] bytes) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(bytes));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 not available", e);
		}
	}
}
