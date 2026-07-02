package com.aeg.core.fiscalbook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.aeg.core.branch.Branch;
import com.aeg.core.company.Company;
import com.aeg.core.distributor.Distributor;
import com.aeg.core.printer.Printer;
import com.aeg.core.servicecenter.ServiceCenter;
import com.aeg.core.technicalservice.TechnicalServiceVisit;

class FiscalBookEnajenadorResolverTest {

	@Test
	void prefersPrinterDistributorOverServiceCenter() {
		Branch distributorBranch = branchWithCompany("Distribuidora Demo", "J111111111");
		Distributor distributor = new Distributor();
		distributor.setBranch(distributorBranch);

		Printer printer = new Printer();
		printer.setDistributor(distributor);

		TechnicalServiceVisit visit = visitWithServiceCenter(branchWithCompany("Centro Demo", "J222222222"));

		assertEquals(distributorBranch, FiscalBookEnajenadorResolver.resolveBranch(printer, List.of(visit)));
		assertFalse(FiscalBookEnajenadorResolver.isManufacturerFallback(printer, List.of(visit)));
	}

	@Test
	void usesServiceCenterWhenPrinterHasNoDistributor() {
		Printer printer = new Printer();
		Branch serviceBranch = branchWithCompany("Centro Servicio", "J333333333");
		TechnicalServiceVisit visit = visitWithServiceCenter(serviceBranch);

		assertEquals(serviceBranch, FiscalBookEnajenadorResolver.resolveBranch(printer, List.of(visit)));
		assertFalse(FiscalBookEnajenadorResolver.isManufacturerFallback(printer, List.of(visit)));
	}

	@Test
	void fallsBackToManufacturerWhenNoDistributorOrServiceCenter() {
		Printer printer = new Printer();

		assertEquals(null, FiscalBookEnajenadorResolver.resolveBranch(printer, List.of()));
		assertTrue(FiscalBookEnajenadorResolver.isManufacturerFallback(printer, List.of()));
	}

	@Test
	void manufacturerProfileMatchesFiscalBookFabricante() {
		var profile = AegManufacturerProfile.toBranchResponse();
		assertEquals(AegManufacturerProfile.BUSINESS_NAME, profile.company().businessName());
		assertEquals(AegManufacturerProfile.RIF, profile.company().rif());
		assertEquals(AegManufacturerProfile.CITY, profile.city());
		assertEquals(AegManufacturerProfile.STATE, profile.state());
	}

	private static Branch branchWithCompany(String businessName, String rif) {
		Company company = new Company();
		company.setBusinessName(businessName);
		company.setRif(rif);
		Branch branch = new Branch();
		branch.setCompany(company);
		return branch;
	}

	private static TechnicalServiceVisit visitWithServiceCenter(Branch branch) {
		ServiceCenter center = new ServiceCenter();
		center.setBranch(branch);
		TechnicalServiceVisit visit = new TechnicalServiceVisit();
		visit.setServiceCenter(center);
		return visit;
	}
}
