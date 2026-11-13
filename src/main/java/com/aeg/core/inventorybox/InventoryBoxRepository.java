package com.aeg.core.inventorybox;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryBoxRepository extends JpaRepository<InventoryBox, Long> {
    List<InventoryBox> findAllByOrderByBoxNumberAsc();

    boolean existsByBoxNumber(Integer boxNumber);

    boolean existsByBoxNumberAndIdNot(Integer boxNumber, Long id);
}
