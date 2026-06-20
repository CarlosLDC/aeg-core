package com.aeg.core.fiscalbook;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aeg.core.branch.Branch;
import com.aeg.core.client.Client;
import com.aeg.core.company.Company;
import com.aeg.core.company.ContributorType;
import com.aeg.core.distributor.Distributor;
import com.aeg.core.fiscalbook.dto.FiscalBookDtos.FiscalBookAnnualInspectionResponse;
import com.aeg.core.fiscalbook.dto.FiscalBookDtos.FiscalBookBranchResponse;
import com.aeg.core.fiscalbook.dto.FiscalBookDtos.FiscalBookCompanyResponse;
import com.aeg.core.fiscalbook.dto.FiscalBookDtos.FiscalBookDetailResponse;
import com.aeg.core.fiscalbook.dto.FiscalBookDtos.FiscalBookDistributorResponse;
import com.aeg.core.fiscalbook.dto.FiscalBookDtos.FiscalBookModelResponse;
import com.aeg.core.fiscalbook.dto.FiscalBookDtos.FiscalBookSearchResponse;
import com.aeg.core.fiscalbook.dto.FiscalBookDtos.FiscalBookSealResponse;
import com.aeg.core.fiscalbook.dto.FiscalBookDtos.FiscalBookSoftwareResponse;
import com.aeg.core.fiscalbook.dto.FiscalBookDtos.FiscalBookSummaryResponse;
import com.aeg.core.fiscalbook.dto.FiscalBookDtos.FiscalBookTechnicalServiceResponse;
import com.aeg.core.inspection.AnnualInspection;
import com.aeg.core.inspection.AnnualInspectionRepository;
import com.aeg.core.printer.Printer;
import com.aeg.core.printer.PrinterStatus;
import com.aeg.core.printermodel.PrinterModel;
import com.aeg.core.seal.Seal;
import com.aeg.core.seal.SealRepository;
import com.aeg.core.security.SecurityScopeService;
import com.aeg.core.security.User;
import com.aeg.core.servicecenter.ResourceNotFoundException;
import com.aeg.core.servicecenter.ServiceCenter;
import com.aeg.core.software.Software;
import com.aeg.core.technicalservice.TechnicalServiceVisit;
import com.aeg.core.technicalservice.TechnicalServiceVisitRepository;

@Service
@Transactional(readOnly = true)
public class FiscalBookServiceImpl implements FiscalBookService {

	private static final Pattern SERIAL_PATTERN = Pattern.compile("^[A-Z]{3}[0-9]{7}$", Pattern.CASE_INSENSITIVE);
	private static final Pattern RIF_PATTERN = Pattern.compile("^[VEJPG][0-9]{7,9}$", Pattern.CASE_INSENSITIVE);

	private final SecurityScopeService securityScope;
	private final TechnicalServiceVisitRepository technicalServiceRepository;
	private final AnnualInspectionRepository annualInspectionRepository;
	private final SealRepository sealRepository;

	public FiscalBookServiceImpl(
			SecurityScopeService securityScope,
			TechnicalServiceVisitRepository technicalServiceRepository,
			AnnualInspectionRepository annualInspectionRepository,
			SealRepository sealRepository) {
		this.securityScope = securityScope;
		this.technicalServiceRepository = technicalServiceRepository;
		this.annualInspectionRepository = annualInspectionRepository;
		this.sealRepository = sealRepository;
	}

	@Override
	public FiscalBookSearchResponse search(String query, int page, int pageSize) {
		int safePage = Math.max(page, 1);
		int safeSize = Math.max(Math.min(pageSize, 100), 1);

		List<Printer> visible = securityScope.findVisiblePrinters().stream()
				.sorted(Comparator.comparing(Printer::getFiscalSerial, String.CASE_INSENSITIVE_ORDER))
				.toList();

		List<Printer> filtered;
		if (query == null || query.isBlank()) {
			filtered = visible;
		} else {
			String trimmed = query.trim().toUpperCase(Locale.ROOT);
			if (SERIAL_PATTERN.matcher(trimmed).matches()) {
				filtered = visible.stream()
						.filter(p -> p.getFiscalSerial() != null
								&& p.getFiscalSerial().equalsIgnoreCase(trimmed))
						.toList();
			} else if (RIF_PATTERN.matcher(trimmed).matches()) {
				String normalizedRif = normalizeRif(trimmed);
				filtered = visible.stream()
						.filter(p -> companyRif(p).map(r -> r.contains(normalizedRif) || normalizedRif.contains(r))
								.orElse(false))
						.toList();
			} else {
				filtered = List.of();
			}
		}

		long total = filtered.size();
		int from = (safePage - 1) * safeSize;
		List<FiscalBookSummaryResponse> items = filtered.stream()
				.skip(from)
				.limit(safeSize)
				.map(this::toSummary)
				.toList();

		return new FiscalBookSearchResponse(items, total, safePage, safeSize);
	}

	@Override
	public FiscalBookDetailResponse findByPrinterId(Long printerId) {
		Printer printer = securityScope.findVisiblePrinters().stream()
				.filter(p -> p.getId().equals(printerId))
				.findFirst()
				.orElseThrow(() -> new ResourceNotFoundException("Printer not found with id: " + printerId));

		List<Seal> seals = sealRepository.findByPrinter_Id(printerId);
		List<TechnicalServiceVisit> services = technicalServiceRepository
				.findByPrinter_IdOrderByCreatedAtAsc(printerId);
		List<AnnualInspection> inspections = annualInspectionRepository
				.findByPrinter_IdOrderByCreatedAtAsc(printerId);

		return toDetail(printer, seals, services, inspections);
	}

	private FiscalBookSummaryResponse toSummary(Printer printer) {
		Client client = printer.getClient();
		Branch branch = client != null ? client.getBranch() : null;
		Company company = branch != null ? branch.getCompany() : null;
		return new FiscalBookSummaryResponse(
				printer.getId(),
				printer.getFiscalSerial(),
				company != null ? company.getBusinessName() : null,
				company != null ? company.getRif() : null,
				statusValue(printer.getStatus()),
				printer.getDistributorId());
	}

	private FiscalBookDetailResponse toDetail(
			Printer printer,
			List<Seal> seals,
			List<TechnicalServiceVisit> services,
			List<AnnualInspection> inspections) {
		Client client = printer.getClient();
		Branch clientBranch = client != null ? client.getBranch() : null;
		Company company = clientBranch != null ? clientBranch.getCompany() : null;

		String address = joinAddress(clientBranch);
		PrinterModel model = printer.getModel();
		Software software = printer.getSoftware();
		Distributor distributor = printer.getDistributor();

		return new FiscalBookDetailResponse(
				printer.getId(),
				printer.getFiscalSerial(),
				statusValue(printer.getStatus()),
				printer.getDeviceType() != null ? printer.getDeviceType().getValue() : "interno",
				printer.getFinalSalePrice(),
				printer.getPaid(),
				printer.getVersionFirmware(),
				printer.getMacAddress(),
				printer.getCreatedAt(),
				printer.getInstallationDate(),
				printer.getModelId(),
				printer.getSoftwareId(),
				printer.getClientId(),
				printer.getDistributorId(),
				company != null ? company.getBusinessName() : null,
				company != null ? company.getRif() : null,
				contributorTypeValue(company != null ? company.getContributorType() : null),
				address,
				toBranch(clientBranch),
				toModel(model),
				toSoftware(software),
				toDistributor(distributor),
				seals.stream().map(this::toSeal).toList(),
				services.stream().map(s -> toTechnicalService(s, seals)).toList(),
				inspections.stream().map(this::toAnnualInspection).toList());
	}

	private FiscalBookBranchResponse toBranch(Branch branch) {
		if (branch == null) {
			return null;
		}
		Company company = branch.getCompany();
		return new FiscalBookBranchResponse(
				branch.getId(),
				branch.getCompanyId(),
				branch.getCity(),
				branch.getState(),
				branch.getAddress(),
				branch.getPhone(),
				branch.getEmail(),
				branch.getIsClient(),
				branch.getIsDistributor(),
				branch.getIsServiceCenter(),
				company != null
						? new FiscalBookCompanyResponse(
								company.getId(),
								company.getBusinessName(),
								company.getRif(),
								contributorTypeValue(company.getContributorType()))
						: null);
	}

	private FiscalBookModelResponse toModel(PrinterModel model) {
		if (model == null) {
			return null;
		}
		return new FiscalBookModelResponse(
				model.getId(),
				model.getBrand(),
				model.getModelCode(),
				model.getProvidencia(),
				model.getApprovalDate(),
				model.getPrice());
	}

	private FiscalBookSoftwareResponse toSoftware(Software software) {
		if (software == null) {
			return null;
		}
		return new FiscalBookSoftwareResponse(
				software.getId(),
				software.getName(),
				software.getVersion(),
				software.getCreatedAt());
	}

	private FiscalBookDistributorResponse toDistributor(Distributor distributor) {
		if (distributor == null) {
			return null;
		}
		return new FiscalBookDistributorResponse(distributor.getId(), toBranch(distributor.getBranch()));
	}

	private FiscalBookSealResponse toSeal(Seal seal) {
		return new FiscalBookSealResponse(
				seal.getId(),
				seal.getPrinterId(),
				seal.getSerial(),
				seal.getColor() != null ? seal.getColor().getValue() : null,
				seal.getStatus() != null ? seal.getStatus().getValue() : null,
				seal.getCreatedAt(),
				seal.getInstallationDate(),
				seal.getRemovalDate());
	}

	private FiscalBookTechnicalServiceResponse toTechnicalService(
			TechnicalServiceVisit visit,
			List<Seal> printerSeals) {
		User reviewer = visit.getReviewedByUser();
		Distributor distributor = visit.getDistributor();
		Branch distributorBranch = distributor != null ? distributor.getBranch() : null;
		Company distributorCompany = distributorBranch != null ? distributorBranch.getCompany() : null;

		String serviceCenter = resolveServiceCenterLabel(visit);
		String centerRif = distributorCompany != null ? distributorCompany.getRif() : null;

		Seal removed = visit.getRemovedSeal();
		Seal installed = visit.getInstalledSeal();
		if (removed == null && visit.getRemovedSealId() != null) {
			removed = printerSeals.stream()
					.filter(s -> s.getId().equals(visit.getRemovedSealId()))
					.findFirst()
					.orElse(null);
		}
		if (installed == null && visit.getInstalledSealId() != null) {
			installed = printerSeals.stream()
					.filter(s -> s.getId().equals(visit.getInstalledSealId()))
					.findFirst()
					.orElse(null);
		}

		return new FiscalBookTechnicalServiceResponse(
				visit.getId(),
				visit.getCreatedAt(),
				visit.getRequestDate(),
				serviceCenter,
				centerRif,
				reviewer != null ? reviewer.getName() : null,
				reviewer != null ? reviewer.getNationalId() : null,
				visit.getReportedFailure(),
				visit.getStartAt(),
				visit.getEndAt(),
				visit.getInitialZReport(),
				visit.getFinalZReport(),
				visit.getInitialZDate(),
				visit.getFinalZDate(),
				visit.getSealTampered(),
				visit.getInstalledSealId(),
				visit.getRemovedSealId(),
				installed != null ? installed.getSerial() : null,
				removed != null ? removed.getSerial() : null,
				visit.getNotes(),
				visit.getCost(),
				visit.getPhotoUrls() == null ? List.of() : Arrays.asList(visit.getPhotoUrls()));
	}

	private String resolveServiceCenterLabel(TechnicalServiceVisit visit) {
		ServiceCenter center = visit.getServiceCenter();
		if (center != null && center.getBranch() != null && center.getBranch().getCompany() != null) {
			return center.getBranch().getCompany().getBusinessName();
		}
		if (visit.getDistributorId() != null) {
			return "Distribuidora (id " + visit.getDistributorId() + ")";
		}
		return null;
	}

	private FiscalBookAnnualInspectionResponse toAnnualInspection(AnnualInspection inspection) {
		User inspector = inspection.getInspectorUser();
		Printer printer = inspection.getPrinter();
		Client client = printer != null ? printer.getClient() : null;
		Branch branch = client != null ? client.getBranch() : null;
		Company company = branch != null ? branch.getCompany() : null;
		return new FiscalBookAnnualInspectionResponse(
				inspection.getId(),
				inspection.getCreatedAt(),
				inspection.getInspectionDate(),
				company != null ? company.getBusinessName() : null,
				company != null ? company.getRif() : null,
				inspector != null ? inspector.getName() : null,
				inspection.getSealTampered(),
				inspection.getNotes(),
				inspection.getPhotoUrls() == null ? List.of() : Arrays.asList(inspection.getPhotoUrls()));
	}

	private static String statusValue(PrinterStatus status) {
		return status != null ? status.getValue() : null;
	}

	private static String contributorTypeValue(ContributorType type) {
		return type != null ? type.getValue() : null;
	}

	private static String joinAddress(Branch branch) {
		if (branch == null) {
			return null;
		}
		return java.util.stream.Stream.of(branch.getAddress(), branch.getCity(), branch.getState())
				.filter(s -> s != null && !s.isBlank())
				.reduce((a, b) -> a + ", " + b)
				.orElse(null);
	}

	private static java.util.Optional<String> companyRif(Printer printer) {
		Client client = printer.getClient();
		if (client == null || client.getBranch() == null || client.getBranch().getCompany() == null) {
			return java.util.Optional.empty();
		}
		String rif = client.getBranch().getCompany().getRif();
		if (rif == null || rif.isBlank()) {
			return java.util.Optional.empty();
		}
		return java.util.Optional.of(normalizeRif(rif));
	}

	private static String normalizeRif(String rif) {
		return rif.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
	}
}
