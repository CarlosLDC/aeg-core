package com.aeg.core.decommission;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PrinterDecommissionRepository extends JpaRepository<PrinterDecommission, Long> {

    List<PrinterDecommission> findByPrinter_IdOrderByDateDescIdDesc(Long printerId);
}
