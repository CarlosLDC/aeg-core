package com.aeg.core.printer;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PrinterStatusConverter implements AttributeConverter<PrinterStatus, String> {

    @Override
    public String convertToDatabaseColumn(PrinterStatus attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public PrinterStatus convertToEntityAttribute(String dbData) {
        return PrinterStatus.fromValue(dbData);
    }
}
