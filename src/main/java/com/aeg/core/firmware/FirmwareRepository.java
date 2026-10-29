package com.aeg.core.firmware;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FirmwareRepository extends JpaRepository<Firmware, Long> {

	boolean existsByFileName(String fileName);

	boolean existsByVersionAndPrinterModel_Id(String version, Long printerModelId);

	boolean existsByVersionAndPrinterModelIsNull(String version);

	boolean existsByVersionAndPrinterModel_IdAndIdNot(String version, Long printerModelId, Long id);

	boolean existsByVersionAndPrinterModelIsNullAndIdNot(String version, Long id);

	List<Firmware> findByPrinterModel_IdOrderByCreatedAtDesc(Long printerModelId);

	List<Firmware> findAllByOrderByCreatedAtDesc();

	Optional<Firmware> findFirstByPrinterModel_ModelCodeIgnoreCaseOrderByCreatedAtDesc(String modelCode);

	Optional<Firmware> findFirstByVersionAndPrinterModel_IdOrderByCreatedAtDesc(String version, Long printerModelId);

	Optional<Firmware> findFirstByVersionAndPrinterModelIsNullOrderByCreatedAtDesc(String version);

	Optional<Firmware> findFirstByVersionOrderByCreatedAtDesc(String version);

	List<Firmware> findByChecksumMd5IsNull();
}
