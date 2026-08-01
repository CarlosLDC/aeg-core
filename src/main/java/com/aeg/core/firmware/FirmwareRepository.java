package com.aeg.core.firmware;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FirmwareRepository extends JpaRepository<Firmware, Long> {

	boolean existsByFileName(String fileName);

	boolean existsByVersionAndPrinterModel_Id(String version, Long printerModelId);

	boolean existsByVersionAndPrinterModelIsNull(String version);

	boolean existsByVersionAndPrinterModel_IdAndIdNot(String version, Long printerModelId, Long id);

	boolean existsByVersionAndPrinterModelIsNullAndIdNot(String version, Long id);

	List<Firmware> findByPrinterModel_IdOrderByCreatedAtDesc(Long printerModelId);

	List<Firmware> findAllByOrderByCreatedAtDesc();
}
