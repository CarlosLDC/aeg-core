package com.aeg.core.employee;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class EmployeeTypeConverter implements AttributeConverter<EmployeeType, String> {

	@Override
	public String convertToDatabaseColumn(EmployeeType attribute) {
		return attribute == null ? null : attribute.getValue();
	}

	@Override
	public EmployeeType convertToEntityAttribute(String dbData) {
		return dbData == null ? null : EmployeeType.fromValue(dbData);
	}
}
