package com.aeg.core.inventorybox.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record InventoryBoxRequest(
        @NotNull @Min(1) Integer boxNumber,
        @Size(max = 255) String observations
) {}
