package com.aeg.core.fiscalbook;

import java.util.List;

import com.aeg.core.branch.Branch;
import com.aeg.core.distributor.Distributor;
import com.aeg.core.printer.Printer;
import com.aeg.core.servicecenter.ServiceCenter;
import com.aeg.core.technicalservice.TechnicalServiceVisit;

/**
 * Resuelve quién enajenó la impresora: distribuidor de inventario, centro de servicio
 * (primer servicio técnico con centro) o AEG como fabricante.
 */
public final class FiscalBookEnajenadorResolver {

	private FiscalBookEnajenadorResolver() {
	}

	public static Branch resolveBranch(Printer printer, List<TechnicalServiceVisit> services) {
		Distributor distributor = printer.getDistributor();
		if (distributor != null && distributor.getBranch() != null) {
			return distributor.getBranch();
		}
		if (services != null) {
			for (TechnicalServiceVisit visit : services) {
				ServiceCenter center = visit.getServiceCenter();
				if (center != null && center.getBranch() != null) {
					return center.getBranch();
				}
			}
		}
		return null;
	}

	public static boolean isManufacturerFallback(Printer printer, List<TechnicalServiceVisit> services) {
		return resolveBranch(printer, services) == null;
	}
}
