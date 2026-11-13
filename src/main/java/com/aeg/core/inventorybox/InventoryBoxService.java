package com.aeg.core.inventorybox;

import java.util.List;

import com.aeg.core.inventorybox.dto.InventoryBoxRequest;
import com.aeg.core.inventorybox.dto.InventoryBoxResponse;

public interface InventoryBoxService {
    List<InventoryBoxResponse> findAll();

    InventoryBoxResponse findById(Long id);

    InventoryBoxResponse create(InventoryBoxRequest request);

    InventoryBoxResponse update(Long id, InventoryBoxRequest request);

    void delete(Long id);
}
