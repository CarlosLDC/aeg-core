package com.aeg.core.printer;

import java.util.List;

import com.aeg.core.printer.dto.PrinterDependencyRef;

public class PrinterDeleteBlockedException extends RuntimeException {

	private final List<PrinterDependencyRef> dependencies;
	private final List<String> consequences;

	public PrinterDeleteBlockedException(
			String message,
			List<PrinterDependencyRef> dependencies,
			List<String> consequences) {
		super(message);
		this.dependencies = List.copyOf(dependencies);
		this.consequences = List.copyOf(consequences);
	}

	public List<PrinterDependencyRef> getDependencies() {
		return dependencies;
	}

	public List<String> getConsequences() {
		return consequences;
	}
}
