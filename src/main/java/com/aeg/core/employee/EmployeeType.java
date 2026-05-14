package com.aeg.core.employee;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum EmployeeType {
	ADMINISTRATIVO("administrativo"),
	TECNICO("tecnico"),
	VENDEDOR("vendedor"),
	GERENTE("gerente");

	private final String value;

	EmployeeType(String value) {
		this.value = value;
	}

	@JsonValue
	public String getValue() {
		return value;
	}

	@JsonCreator
	public static EmployeeType fromValue(String v) {
		if (v == null) {
			return null;
		}
		for (EmployeeType t : values()) {
			if (t.value.equalsIgnoreCase(v) || t.name().equalsIgnoreCase(v)) {
				return t;
			}
		}
		return null;
	}
}
