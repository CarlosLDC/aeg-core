package com.aeg.core.printer.dto;

import com.aeg.core.printer.PrinterTicketSection;

public record PrinterEnajenacionTicketResponse(
        PrinterTicketSection header,
        PrinterTicketSection trailer
) {}
