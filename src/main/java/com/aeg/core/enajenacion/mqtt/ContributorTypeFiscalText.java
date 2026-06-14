package com.aeg.core.enajenacion.mqtt;

import com.aeg.core.company.ContributorType;

public final class ContributorTypeFiscalText {

    private ContributorTypeFiscalText() {
    }

    public static String toEncFacLine(ContributorType contributorType) {
        if (contributorType == null) {
            return "CONTRIBUYENTE ORDINARIO";
        }
        return switch (contributorType) {
            case ORDINARIO -> "CONTRIBUYENTE ORDINARIO";
            case ESPECIAL -> "CONTRIBUYENTE ESPECIAL";
            case FORMAL -> "CONTRIBUYENTE FORMAL";
        };
    }
}
