package com.aeg.core.fiscalbook;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aeg.core.fiscalbook.dto.FiscalBookDtos.FiscalBookDetailResponse;
import com.aeg.core.fiscalbook.dto.FiscalBookDtos.FiscalBookSearchResponse;

@RestController
@RequestMapping("/api/fiscal-books")
public class FiscalBookController {

	private final FiscalBookService service;

	public FiscalBookController(FiscalBookService service) {
		this.service = service;
	}

	@GetMapping("/search")
	public FiscalBookSearchResponse search(
			@RequestParam(required = false) String query,
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "10") int pageSize) {
		return service.search(query, page, pageSize);
	}

	@GetMapping("/{printerId}")
	public FiscalBookDetailResponse findByPrinterId(@PathVariable Long printerId) {
		return service.findByPrinterId(printerId);
	}
}
