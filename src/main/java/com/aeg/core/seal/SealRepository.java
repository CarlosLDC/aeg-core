package com.aeg.core.seal;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SealRepository extends JpaRepository<Seal, Long> {
    boolean existsBySerialIgnoreCase(String serial);

    @Query("SELECT s FROM Seal s WHERE s.printer IS NOT NULL AND s.printer.id IN :printerIds")
    List<Seal> findByPrinter_IdIn(@Param("printerIds") Collection<Long> printerIds);

    List<Seal> findByPrinter_Id(Long printerId);
}
