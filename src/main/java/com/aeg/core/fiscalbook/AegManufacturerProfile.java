package com.aeg.core.fiscalbook;

import com.aeg.core.fiscalbook.dto.FiscalBookDtos.FiscalBookBranchResponse;
import com.aeg.core.fiscalbook.dto.FiscalBookDtos.FiscalBookCompanyResponse;

/**
 * Datos fiscales del fabricante (AEG) cuando la impresora fue enajenada directamente
 * por el administrador y no hay distribuidor ni centro de servicio registrado.
 */
public final class AegManufacturerProfile {

	public static final String BUSINESS_NAME = "ALPHA ENGINEER GROUP, C.A.";
	public static final String RIF = "J504594369";
	public static final String STATE = "MIRANDA";
	public static final String CITY = "LOS TEQUES";
	public static final String ADDRESS =
			"AVENIDA BICENTENARIO, REDOMA DEL TAMBOR, EDIFICIO VERACRUZ, PISO 1, LOCAL N° 3";
	public static final String PHONE = "584242913038";
	public static final String EMAIL = "soportealphavzla@gmail.com";

	private AegManufacturerProfile() {
	}

	public static FiscalBookBranchResponse toBranchResponse() {
		return new FiscalBookBranchResponse(
				0L,
				0L,
				CITY,
				STATE,
				ADDRESS,
				PHONE,
				EMAIL,
				false,
				false,
				false,
				new FiscalBookCompanyResponse(0L, BUSINESS_NAME, RIF, "ordinario"));
	}
}
