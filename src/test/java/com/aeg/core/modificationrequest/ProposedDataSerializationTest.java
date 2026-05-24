package com.aeg.core.modificationrequest;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.aeg.core.employee.EmployeeType;
import com.aeg.core.modificationrequest.dto.EmployeeModificationProposedData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class ProposedDataSerializationTest {

	@Test
	void writeValueAsString_onRecord_includesAllFields() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		var proposed = new EmployeeModificationProposedData(
				"V555",
				"Después",
				"04129999999",
				"despues@test.com",
				EmployeeType.TECNICO,
				9L,
				true,
				false);

		String json = mapper.writeValueAsString(proposed);
		JsonNode node = mapper.readTree(json);

		assertThat(node.get("nationalId").asText()).isEqualTo("V555");
		assertThat(node.get("name").asText()).isEqualTo("Después");
		assertThat(node.get("branchId").asLong()).isEqualTo(9L);
		assertThat(node.get("isTechnician").asBoolean()).isTrue();
	}

	@Test
	void valueToTree_onRecord_canBeEmptyWithMinimalMapper() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
		var proposed = new EmployeeModificationProposedData(
				"V555",
				"Después",
				"04129999999",
				"despues@test.com",
				EmployeeType.TECNICO,
				9L,
				true,
				false);

		JsonNode tree = mapper.valueToTree(proposed);

		assertThat(tree.isObject()).isTrue();
		assertThat(tree.hasNonNull("nationalId"))
				.as("valueToTree must include nationalId (mqtt-style mapper)")
				.isTrue();
	}
}
