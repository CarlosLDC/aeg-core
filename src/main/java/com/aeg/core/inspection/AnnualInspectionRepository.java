package com.aeg.core.inspection;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AnnualInspectionRepository extends JpaRepository<AnnualInspection, Long> {

	List<AnnualInspection> findByPrinter_IdIn(Collection<Long> printerIds);

	List<AnnualInspection> findByPrinter_IdOrderByCreatedAtAsc(Long printerId);

	boolean existsByInspectorUser_Id(Long userId);

	Optional<AnnualInspection> findByPrinter_IdAndMqttQrCodigo(Long printerId, String mqttQrCodigo);

	List<AnnualInspection> findByPrinter_IdAndMqttRegistroImpresoraIgnoreCase(
			Long printerId,
			String mqttRegistroImpresora);
}
