package com.aeg.core.enajenacion;

import java.util.List;
import java.util.stream.Collectors;

import com.aeg.core.branch.Branch;
import com.aeg.core.company.Company;
import com.aeg.core.company.ContributorType;
import com.aeg.core.enajenacion.mqtt.ContributorTypeFiscalText;
import com.aeg.core.enajenacion.mqtt.EnajenacionPayloadBuilder;
import com.aeg.core.enajenacion.mqtt.EnajenacionPayloadBuilder.AddressLines;
import com.aeg.core.printer.PrinterTicketSection;

public final class EnajenacionTicketExtractor {

    public static final int FIXED_ENCABEZADO_PREFIX_LINES = 3;

    private EnajenacionTicketExtractor() {
    }

    public static List<String> extractHeaderLines(List<String> encabezadoLineas, ContributorType contributorType) {
        if (encabezadoLineas == null || encabezadoLineas.size() <= FIXED_ENCABEZADO_PREFIX_LINES) {
            throw new IllegalArgumentException(
                    "Encabezado must include address lines after SENIAT, RIF and business name");
        }
        List<String> tail = encabezadoLineas.subList(FIXED_ENCABEZADO_PREFIX_LINES, encabezadoLineas.size())
                .stream()
                .map(line -> line == null ? "" : line.trim())
                .collect(Collectors.toList());
        String contributorLine = ContributorTypeFiscalText.toEncFacLine(contributorType);
        if (!tail.isEmpty() && contributorLine.equalsIgnoreCase(tail.get(tail.size() - 1))) {
            tail = tail.subList(0, tail.size() - 1);
        }
        if (tail.isEmpty()) {
            throw new IllegalArgumentException("Encabezado must include address and location lines");
        }

        String addressLine1 = tail.get(0);
        String addressLine2 = "";
        String cityStateLine;
        if (tail.size() == 1) {
            cityStateLine = "";
        } else if (tail.size() == 2) {
            cityStateLine = tail.get(1);
        } else {
            addressLine2 = tail.get(1);
            cityStateLine = tail.get(2);
        }
        return EnajenacionPayloadBuilder.buildEncFacFijo(
                addressLine1, addressLine2, cityStateLine, contributorLine);
    }

    public static List<String> extractTrailerLines(List<String> pieMensajes) {
        if (pieMensajes == null || pieMensajes.isEmpty()) {
            return List.of();
        }
        return pieMensajes.stream()
                .map(line -> line == null ? "" : line.trim())
                .filter(line -> !line.isBlank())
                .toList();
    }

    public static PrinterTicketSection buildDefaultHeader(Branch branch, Company company) {
        AddressLines addressLines = EnajenacionPayloadBuilder.splitAddress(branch.getAddress());
        String cityStateLine = branch.getCity().trim() + ", " + branch.getState().trim();
        List<String> encFacFijo = EnajenacionPayloadBuilder.buildEncFacFijo(
                addressLines.line1(),
                addressLines.line2(),
                cityStateLine,
                ContributorTypeFiscalText.toEncFacLine(company.getContributorType()));
        return new PrinterTicketSection(encFacFijo);
    }

    public static PrinterTicketSection buildDefaultTrailer() {
        return new PrinterTicketSection(List.of());
    }
}
