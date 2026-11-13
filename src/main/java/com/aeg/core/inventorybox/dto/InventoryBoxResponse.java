package com.aeg.core.inventorybox.dto;

import java.time.OffsetDateTime;

public record InventoryBoxResponse(
        Long id,
        Integer boxNumber,
        String observations,
        OffsetDateTime createdAt,
        long printerCount
) {}
