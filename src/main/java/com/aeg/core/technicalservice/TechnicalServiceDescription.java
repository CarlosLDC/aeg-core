package com.aeg.core.technicalservice;

public final class TechnicalServiceDescription {

	private TechnicalServiceDescription() {
	}

	public static String merge(String reportedFailure, String notes) {
		String failure = reportedFailure == null ? "" : reportedFailure.strip();
		String noteText = notes == null ? "" : notes.strip();
		if (failure.isEmpty()) {
			return noteText;
		}
		if (noteText.isEmpty() || failure.equals(noteText)) {
			return failure;
		}
		return failure + "\n\n" + noteText;
	}

	public static String fromVisit(TechnicalServiceVisit visit) {
		if (visit == null) {
			return "";
		}
		return merge(visit.getReportedFailure(), visit.getNotes());
	}
}
