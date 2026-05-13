package com.aeg.core.seal;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class SealStatusConverter implements AttributeConverter<SealStatus, String> {

    @Override
    public String convertToDatabaseColumn(SealStatus attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public SealStatus convertToEntityAttribute(String dbData) {
        return SealStatus.fromValue(dbData);
    }
}
