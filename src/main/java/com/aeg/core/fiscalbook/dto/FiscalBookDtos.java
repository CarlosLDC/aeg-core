package com.aeg.core.fiscalbook.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public final class FiscalBookDtos {

	private FiscalBookDtos() {
	}

	public record FiscalBookSearchResponse(
			List<FiscalBookSummaryResponse> items,
			long total,
			int page,
			int pageSize) {
	}

	public record FiscalBookSummaryResponse(
			Long id,
			String fiscalSerial,
			String businessName,
			String rif,
			String status,
			Long distributorId) {
	}

	public record FiscalBookDetailResponse(
			Long id,
			String fiscalSerial,
			String status,
			String deviceType,
			BigDecimal finalSalePrice,
			Boolean paid,
			String versionFirmware,
			String macAddress,
			OffsetDateTime createdAt,
			OffsetDateTime installationDate,
			Long modelId,
			Long softwareId,
			Long clientId,
			Long distributorId,
			String businessName,
			String rif,
			String taxpayerType,
			String address,
			FiscalBookBranchResponse branch,
			FiscalBookModelResponse model,
			FiscalBookSoftwareResponse software,
			FiscalBookDistributorResponse distributor,
			List<FiscalBookSealResponse> seals,
			List<FiscalBookTechnicalServiceResponse> technicalServices,
			List<FiscalBookAnnualInspectionResponse> annualInspections) {
	}

	public record FiscalBookBranchResponse(
			Long id,
			Long companyId,
			String city,
			String state,
			String address,
			String phone,
			String email,
			Boolean isClient,
			Boolean isDistributor,
			Boolean isServiceCenter,
			FiscalBookCompanyResponse company) {
	}

	public record FiscalBookCompanyResponse(
			Long id,
			String businessName,
			String rif,
			String contributorType) {
	}

	public record FiscalBookModelResponse(
			Long id,
			String brand,
			String modelCode,
			String providencia,
			LocalDate approvalDate,
			BigDecimal price) {
	}

	public record FiscalBookSoftwareResponse(
			Long id,
			String name,
			String version,
			OffsetDateTime createdAt) {
	}

	public record FiscalBookDistributorResponse(
			Long id,
			FiscalBookBranchResponse branch) {
	}

	public record FiscalBookSealResponse(
			Long id,
			Long printerId,
			String serial,
			String color,
			String status,
			OffsetDateTime createdAt,
			OffsetDateTime installationDate,
			OffsetDateTime removalDate) {
	}

	public record FiscalBookTechnicalServiceResponse(
			Long id,
			OffsetDateTime createdAt,
			LocalDate requestDate,
			String serviceCenter,
			String centerRif,
			String technician,
			String technicianNationalId,
			String reportedFailure,
			OffsetDateTime startAt,
			OffsetDateTime endAt,
			Integer initialZReport,
			Integer finalZReport,
			OffsetDateTime initialZDate,
			OffsetDateTime finalZDate,
			Boolean sealTampered,
			Long installedSealId,
			Long removedSealId,
			String installedSealSerial,
			String removedSealSerial,
			String notes,
			BigDecimal cost,
			List<String> photoUrls) {
	}

	public record FiscalBookAnnualInspectionResponse(
			Long id,
			OffsetDateTime createdAt,
			LocalDate inspectionDate,
			String serviceCenter,
			String centerRif,
			String inspector,
			Boolean sealTampered,
			String notes,
			List<String> photoUrls,
			String mqttRegistroImpresora,
			Long mqttSetDateRevOAt,
			Integer mqttNumeroFacturaPrueba) {
	}
}
