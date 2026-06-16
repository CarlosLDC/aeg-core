package com.aeg.core.enajenacion.mqtt;

import org.springframework.stereotype.Component;

import com.aeg.core.branch.Branch;
import com.aeg.core.client.Client;
import com.aeg.core.company.Company;
import com.aeg.core.enajenacion.mqtt.EnajenacionPayloadBuilder.AddressLines;
import com.aeg.core.printer.Printer;
import com.aeg.core.printer.PrinterRepository;
import com.aeg.core.printer.PrinterStatus;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class EnajenacionPreconditionValidator {

    private final PrinterRepository printerRepository;

    public EnajenacionContext validateAndBuildContext(String ptrReg, String topicMac, String payloadMac) {
        Printer printer = printerRepository.findEnajenacionCandidateByFiscalSerial(ptrReg)
                .orElseThrow(() -> new EnajenacionProtocolException("Printer not found for ptrReg " + ptrReg));

        if (printer.getStatus() == PrinterStatus.ENAJENADA) {
            throw new EnajenacionAlreadyCompletedException("Printer already enajenada");
        }
        if (!printer.getStatus().isEligibleForMqttEnajenacion()) {
            throw new EnajenacionProtocolException("Printer status must be ASIGNADA or LABORATORIO");
        }
        if (!Boolean.TRUE.equals(printer.getPaid())) {
            throw new EnajenacionProtocolException(
                    "Solo se pueden enajenar impresoras con estatus de pago Pagada.");
        }
        if (printer.getClient() == null) {
            throw new EnajenacionProtocolException("Printer has no assigned client");
        }
        if (!MacAddressNormalizer.sameMac(printer.getMacAddress(), payloadMac)) {
            throw new EnajenacionProtocolException("MAC mismatch between payload and printer record");
        }
        if (!MacAddressNormalizer.sameMac(printer.getMacAddress(), topicMac)) {
            throw new EnajenacionProtocolException("MAC mismatch between topic and printer record");
        }

        Client client = printer.getClient();
        Branch branch = client.getBranch();
        if (branch == null) {
            throw new EnajenacionProtocolException("Client branch is missing");
        }
        Company company = branch.getCompany();
        if (company == null) {
            throw new EnajenacionProtocolException("Client company is missing");
        }
        if (company.getRif() == null || company.getRif().isBlank()) {
            throw new EnajenacionProtocolException("Company RIF is required");
        }
        if (company.getBusinessName() == null || company.getBusinessName().isBlank()) {
            throw new EnajenacionProtocolException("Company business name is required");
        }
        if (branch.getAddress() == null || branch.getAddress().isBlank()
                || branch.getCity() == null || branch.getCity().isBlank()
                || branch.getState() == null || branch.getState().isBlank()) {
            throw new EnajenacionProtocolException("Branch address, city and state are required");
        }

        AddressLines addressLines = EnajenacionPayloadBuilder.splitAddress(branch.getAddress());
        String cityStateLine = branch.getCity().trim() + ", " + branch.getState().trim();

        return new EnajenacionContext(
                printer.getFiscalSerial(),
                MacAddressNormalizer.toColonForm(printer.getMacAddress()),
                client.getId(),
                company.getRif(),
                company.getBusinessName(),
                ContributorTypeFiscalText.toEncFacLine(company.getContributorType()),
                addressLines.line1(),
                addressLines.line2(),
                cityStateLine);
    }

    public Long resolvePrinterId(String ptrReg) {
        return printerRepository.findEnajenacionCandidateByFiscalSerial(ptrReg)
                .map(Printer::getId)
                .orElse(null);
    }
}
