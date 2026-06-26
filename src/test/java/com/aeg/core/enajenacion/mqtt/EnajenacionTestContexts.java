package com.aeg.core.enajenacion.mqtt;

import java.util.List;

public final class EnajenacionTestContexts {

    private EnajenacionTestContexts() {
    }

    public static EnajenacionContext shopComputerContext() {
        return new EnajenacionContext(
                "GRA0000017",
                "20:6E:F1:88:4C:68",
                1L,
                "J500662998",
                "INVERSIONES SHOP COMPUTER 2020, C.A.",
                "CONTRIBUYENTE ORDINARIO",
                "AV. URDANETA EDIF. CASA BERA",
                "PISO PB LOCAL -005-C URB. LA CANDELARIA",
                "CARACAS, DISTRITO CAPITAL",
                List.of(
                        "AV. URDANETA EDIF. CASA BERA",
                        "PISO PB LOCAL -005-C URB. LA CANDELARIA",
                        "CARACAS, DISTRITO CAPITAL",
                        "CONTRIBUYENTE ORDINARIO"),
                List.of());
    }

    public static EnajenacionContext acmeContext() {
        return new EnajenacionContext(
                "GRA0000017",
                "20:6E:F1:88:4C:68",
                1L,
                "J-12345678-9",
                "ACME",
                "CONTRIBUYENTE ORDINARIO",
                "Address",
                "Line 2",
                "Caracas, DC",
                List.of("Address", "Line 2", "Caracas, DC", "CONTRIBUYENTE ORDINARIO"),
                List.of());
    }
}
