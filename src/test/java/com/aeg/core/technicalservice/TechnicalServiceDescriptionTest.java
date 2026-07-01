package com.aeg.core.technicalservice;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TechnicalServiceDescriptionTest {

	@Test
	void mergeKeepsFailureWhenNotesAreEmpty() {
		assertEquals("Papel atascado", TechnicalServiceDescription.merge("Papel atascado", null));
	}

	@Test
	void mergeCombinesDistinctTexts() {
		assertEquals(
				"Papel atascado\n\nSe limpió el rodillo",
				TechnicalServiceDescription.merge("Papel atascado", "Se limpió el rodillo"));
	}

	@Test
	void mergeAvoidsDuplicateText() {
		assertEquals("Mismo texto", TechnicalServiceDescription.merge("Mismo texto", "Mismo texto"));
	}
}
