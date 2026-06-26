package com.aeg.core.enajenacion.mqtt;

import java.math.BigDecimal;

import com.aeg.core.branch.Branch;
import com.aeg.core.branch.BranchRepository;
import com.aeg.core.client.Client;
import com.aeg.core.client.ClientRepository;
import com.aeg.core.company.Company;
import com.aeg.core.company.CompanyRepository;
import com.aeg.core.company.ContributorType;
import com.aeg.core.enajenacion.EnajenacionTicketExtractor;
import com.aeg.core.printer.DeviceType;
import com.aeg.core.printer.Printer;
import com.aeg.core.printer.PrinterRepository;
import com.aeg.core.printer.PrinterStatus;
import com.aeg.core.printermodel.PrinterModel;
import com.aeg.core.printermodel.PrinterModelRepository;
import com.aeg.core.software.Software;
import com.aeg.core.software.SoftwareRepository;

final class EnajenacionTestData {

    record AssignedPrinterFixture(
            Printer printer,
            String compactMac,
            String colonMac,
            String fiscalSerial,
            String comandoTopic) {
    }

    private EnajenacionTestData() {
    }

    static AssignedPrinterFixture seedAssignedPrinter(
            CompanyRepository companyRepository,
            BranchRepository branchRepository,
            ClientRepository clientRepository,
            PrinterModelRepository modelRepository,
            SoftwareRepository softwareRepository,
            PrinterRepository printerRepository,
            String fiscalSerial,
            String colonMac,
            PrinterStatus status) {
        Company company = new Company();
        company.setBusinessName("INVERSIONES SHOP COMPUTER 2020, C.A.");
        company.setRif(rifFromFiscalSerial(fiscalSerial));
        company.setContributorType(ContributorType.ORDINARIO);
        company = companyRepository.save(company);

        Branch branch = new Branch();
        branch.setCompany(company);
        branch.setCity("CARACAS");
        branch.setState("DISTRITO CAPITAL");
        branch.setAddress("AV. URDANETA EDIF. CASA BERA PISO PB LOCAL -005-C");
        branch = branchRepository.save(branch);

        Client client = new Client();
        client.setBranch(branch);
        client = clientRepository.save(client);

        PrinterModel model = new PrinterModel();
        model.setBrand("AEG");
        model.setModelCode("TEST-MQTT");
        model.setPrice(BigDecimal.ZERO);
        model = modelRepository.save(model);

        Software software = new Software();
        software.setName("AEG Fiscal");
        software.setVersion("1.0");
        software = softwareRepository.save(software);

        Printer printer = new Printer();
        printer.setModel(model);
        printer.setSoftware(software);
        printer.setClient(client);
        printer.setFiscalSerial(fiscalSerial);
        printer.setMacAddress(colonMac);
        printer.setStatus(status);
        printer.setDeviceType(DeviceType.INTERNO);
        printer.setPaid(true);
        printer.setFinalSalePrice(BigDecimal.ZERO);
        printer.setHeader(EnajenacionTicketExtractor.buildDefaultHeader(branch, company));
        printer.setTrailer(EnajenacionTicketExtractor.buildDefaultTrailer());
        printer = printerRepository.save(printer);

        String compactMac = MacAddressNormalizer.toCompactForm(colonMac);
        return new AssignedPrinterFixture(
                printer,
                compactMac,
                colonMac,
                fiscalSerial,
                FiscalMqttTopics.comandoTopic(compactMac));
    }

    private static String rifFromFiscalSerial(String fiscalSerial) {
        String digits = fiscalSerial.length() > 3 ? fiscalSerial.substring(3) : fiscalSerial;
        return "J" + digits;
    }
}
