package com.aeg.core.fiscalizacion.mqtt;

import com.aeg.core.printermodel.PrinterModel;
import com.aeg.core.seal.Seal;

public record FiscalizacionContext(
        String ptrReg,
        String colonMac,
        String compactMac,
        String precintoNro,
        String precintoColor,
        String firmwareVersion,
        String modelCode,
        Seal seal,
        PrinterModel printerModel) {
}
