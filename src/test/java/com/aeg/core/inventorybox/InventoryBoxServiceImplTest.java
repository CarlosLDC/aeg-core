package com.aeg.core.inventorybox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.aeg.core.inventorybox.dto.InventoryBoxRequest;
import com.aeg.core.inventorybox.dto.InventoryBoxResponse;
import com.aeg.core.printer.PrinterRepository;
import com.aeg.core.servicecenter.ResourceNotFoundException;

class InventoryBoxServiceImplTest {

    private InventoryBoxRepository repository;
    private PrinterRepository printerRepository;
    private InventoryBoxServiceImpl service;

    @BeforeEach
    void setup() {
        repository = mock(InventoryBoxRepository.class);
        printerRepository = mock(PrinterRepository.class);
        service = new InventoryBoxServiceImpl(repository, printerRepository);
    }

    @Test
    void findAll_returnsBoxesWithPrinterCounts() {
        InventoryBox b1 = box(1L, 1, "Estante A");
        InventoryBox b2 = box(2L, 2, null);
        when(repository.findAllByOrderByBoxNumberAsc()).thenReturn(List.of(b1, b2));
        when(printerRepository.countPrintersGroupedByBoxId())
                .thenReturn(List.<Object[]>of(new Object[] { 1L, 8L }));

        List<InventoryBoxResponse> result = service.findAll();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).boxNumber()).isEqualTo(1);
        assertThat(result.get(0).printerCount()).isEqualTo(8L);
        assertThat(result.get(1).boxNumber()).isEqualTo(2);
        assertThat(result.get(1).printerCount()).isEqualTo(0L);
    }

    @Test
    void create_rejectsDuplicateBoxNumber() {
        when(repository.existsByBoxNumber(1)).thenReturn(true);

        assertThatThrownBy(() -> service.create(new InventoryBoxRequest(1, "Duplicada")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ya existe una caja");
    }

    @Test
    void create_normalizesBlankObservationsToNull() {
        when(repository.existsByBoxNumber(84)).thenReturn(false);
        when(repository.save(any(InventoryBox.class))).thenAnswer(inv -> {
            InventoryBox saved = inv.getArgument(0);
            saved.setId(84L);
            saved.setCreatedAt(OffsetDateTime.now());
            return saved;
        });

        InventoryBoxResponse created = service.create(new InventoryBoxRequest(84, "   "));

        assertThat(created.boxNumber()).isEqualTo(84);
        assertThat(created.observations()).isNull();
        assertThat(created.printerCount()).isEqualTo(0L);
    }

    @Test
    void delete_clearsPrinterReferencesBeforeDeleting() {
        InventoryBox box = box(10L, 10, null);
        when(repository.findById(10L)).thenReturn(Optional.of(box));

        service.delete(10L);

        verify(printerRepository).clearBoxByBoxId(10L);
        verify(repository).delete(box);
    }

    @Test
    void findById_throwsWhenMissing() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private static InventoryBox box(Long id, int number, String observations) {
        InventoryBox box = new InventoryBox();
        box.setId(id);
        box.setBoxNumber(number);
        box.setObservations(observations);
        box.setCreatedAt(OffsetDateTime.now());
        return box;
    }
}
