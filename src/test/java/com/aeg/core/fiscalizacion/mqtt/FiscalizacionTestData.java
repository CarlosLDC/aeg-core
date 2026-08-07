package com.aeg.core.fiscalizacion.mqtt;

import java.math.BigDecimal;

import com.aeg.core.enajenacion.mqtt.MacAddressNormalizer;
import com.aeg.core.printer.Printer;
import com.aeg.core.printer.PrinterRepository;
import com.aeg.core.printer.PrinterStatus;
import com.aeg.core.printer.DeviceType;
import com.aeg.core.printermodel.PrinterModel;
import com.aeg.core.printermodel.PrinterModelRepository;
import com.aeg.core.seal.Seal;
import com.aeg.core.seal.SealColor;
import com.aeg.core.seal.SealRepository;
import com.aeg.core.seal.SealStatus;

final class FiscalizacionTestData {

    record Fixture(
            PrinterModel model,
            Seal seal,
            String compactMac,
            String colonMac,
            String ptrReg,
            String precintoNro,
            String comandoTopic,
            String cmdServerTopic,
            String respuestaTopic) {
    }

    private FiscalizacionTestData() {
    }

    static Fixture seed(
            PrinterModelRepository modelRepository,
            SealRepository sealRepository,
            String ptrReg,
            String colonMac,
            String precintoNro) {
        PrinterModel model = modelRepository.findFirstByModelCodeIgnoreCaseOrderByIdAsc("AEG-R1").orElseGet(() -> {
            PrinterModel created = new PrinterModel();
            created.setModelCode("AEG-R1");
            created.setPrice(BigDecimal.ZERO);
            return modelRepository.save(created);
        });

        Seal seal = new Seal();
        seal.setSerial(precintoNro);
        seal.setColor(SealColor.AZUL);
        seal.setStatus(SealStatus.DISPONIBLE);
        seal = sealRepository.save(seal);

        String compactMac = MacAddressNormalizer.toCompactForm(colonMac);
        return new Fixture(
                model,
                seal,
                compactMac,
                colonMac,
                ptrReg,
                precintoNro,
                "/" + compactMac + "/AEG_Fiscal/Integracion/Comando",
                "/" + compactMac + "/AEG_Fiscal/Integracion/CmdServer",
                "/" + compactMac + "/AEG_Fiscal/Integracion/Respuesta");
    }

    static Printer seedExistingPrinter(
            PrinterRepository printerRepository,
            PrinterModel model,
            String ptrReg,
            String colonMac) {
        Printer printer = new Printer();
        printer.setModel(model);
        printer.setFiscalSerial(ptrReg);
        printer.setMacAddress(colonMac);
        printer.setStatus(PrinterStatus.SIN_ASIGNAR);
        printer.setPaid(false);
        printer.setDeviceType(DeviceType.INTERNO);
        return printerRepository.save(printer);
    }

    static String ptrFiscalizar(
            String ptrReg, String colonMac, String precintoNro, String color, String firmware, String model) {
        return """
                {"cmd":"ptrFiscalizar","data":{"ptrReg":"%s","macAddr":"%s","PrecintoNro":"%s","PrecintoColor":"%s","firmwareVersion":"%s","model":"%s"}}
                """.formatted(ptrReg, colonMac, precintoNro, color, firmware, model).strip();
    }

    static String resultSuccess() {
        return """
                {"cmd":"RxPtrFiscalizarRemoto","code":0,"dataS":{"error":"Impresora Fiscalizando"}}
                """.strip();
    }

    static String resultError() {
        return """
                {"cmd":"RxPtrFiscalizarRemoto","code":1,"dataS":{"error":"ERROR Fiscalizando"}}
                """.strip();
    }
}
