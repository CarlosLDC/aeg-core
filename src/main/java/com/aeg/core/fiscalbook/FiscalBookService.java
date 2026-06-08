package com.aeg.core.fiscalbook;

import com.aeg.core.fiscalbook.dto.FiscalBookDtos.FiscalBookDetailResponse;
import com.aeg.core.fiscalbook.dto.FiscalBookDtos.FiscalBookSearchResponse;

public interface FiscalBookService {

	FiscalBookSearchResponse search(String query, int page, int pageSize);

	FiscalBookDetailResponse findByPrinterId(Long printerId);
}
