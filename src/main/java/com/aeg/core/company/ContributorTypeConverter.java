package com.aeg.core.company;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class ContributorTypeConverter implements AttributeConverter<ContributorType, String> {

    @Override
    public String convertToDatabaseColumn(ContributorType attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public ContributorType convertToEntityAttribute(String dbData) {
        return ContributorType.fromString(dbData);
    }
}
