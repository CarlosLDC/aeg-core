package com.aeg.core.seal;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class SealColorConverter implements AttributeConverter<SealColor, String> {

    @Override
    public String convertToDatabaseColumn(SealColor attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public SealColor convertToEntityAttribute(String dbData) {
        return SealColor.fromValue(dbData);
    }
}
