package com.aeg.core.inventorybox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aeg.core.inventorybox.dto.InventoryBoxRequest;
import com.aeg.core.inventorybox.dto.InventoryBoxResponse;
import com.aeg.core.printer.PrinterRepository;
import com.aeg.core.servicecenter.ResourceNotFoundException;

@Service
@Transactional
public class InventoryBoxServiceImpl implements InventoryBoxService {

    private final InventoryBoxRepository repository;
    private final PrinterRepository printerRepository;

    public InventoryBoxServiceImpl(
            InventoryBoxRepository repository,
            PrinterRepository printerRepository) {
        this.repository = repository;
        this.printerRepository = printerRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryBoxResponse> findAll() {
        List<InventoryBox> boxes = repository.findAllByOrderByBoxNumberAsc();
        if (boxes.isEmpty()) {
            return List.of();
        }
        Map<Long, Long> counts = loadPrinterCountByBoxId();
        return boxes.stream()
                .map(box -> toResponse(box, counts.getOrDefault(box.getId(), 0L)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryBoxResponse findById(Long id) {
        InventoryBox box = findEntityById(id);
        long count = printerRepository.countByBox_Id(box.getId());
        return toResponse(box, count);
    }

    @Override
    public InventoryBoxResponse create(InventoryBoxRequest request) {
        if (repository.existsByBoxNumber(request.boxNumber())) {
            throw new IllegalArgumentException(
                    "Ya existe una caja de inventario con el número " + request.boxNumber() + ".");
        }
        InventoryBox box = new InventoryBox();
        box.setBoxNumber(request.boxNumber());
        box.setObservations(normalizeObservations(request.observations()));
        InventoryBox saved = repository.save(box);
        return toResponse(saved, 0L);
    }

    @Override
    public InventoryBoxResponse update(Long id, InventoryBoxRequest request) {
        InventoryBox box = findEntityById(id);
        if (repository.existsByBoxNumberAndIdNot(request.boxNumber(), id)) {
            throw new IllegalArgumentException(
                    "Ya existe una caja de inventario con el número " + request.boxNumber() + ".");
        }
        box.setBoxNumber(request.boxNumber());
        box.setObservations(normalizeObservations(request.observations()));
        InventoryBox saved = repository.save(box);
        long count = printerRepository.countByBox_Id(saved.getId());
        return toResponse(saved, count);
    }

    @Override
    public void delete(Long id) {
        InventoryBox box = findEntityById(id);
        printerRepository.clearBoxByBoxId(box.getId());
        repository.delete(box);
    }

    private InventoryBox findEntityById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory box not found with id: " + id));
    }

    private Map<Long, Long> loadPrinterCountByBoxId() {
        Map<Long, Long> map = new HashMap<>();
        for (Object[] row : printerRepository.countPrintersGroupedByBoxId()) {
            if (row != null && row.length >= 2 && row[0] instanceof Number boxId && row[1] instanceof Number count) {
                map.put(boxId.longValue(), count.longValue());
            }
        }
        return map;
    }

    private static String normalizeObservations(String observations) {
        if (observations == null) {
            return null;
        }
        String trimmed = observations.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static InventoryBoxResponse toResponse(InventoryBox box, long printerCount) {
        return new InventoryBoxResponse(
                box.getId(),
                box.getBoxNumber(),
                box.getObservations(),
                box.getCreatedAt(),
                printerCount
        );
    }
}
