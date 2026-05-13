package com.aeg.core.printer;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class DeviceTypeConverter implements AttributeConverter<DeviceType, String> {

    @Override
    public String convertToDatabaseColumn(DeviceType attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public DeviceType convertToEntityAttribute(String dbData) {
        return DeviceType.fromValue(dbData);
    }
}
