package com.aeg.core.commercial;

import java.time.OffsetDateTime;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.aeg.core.commercial.dto.CommercialPromoterRequest;
import com.aeg.core.commercial.dto.CommercialPromoterResponse;

@RestController
@RequestMapping("/api/commercial-promoters")
@Transactional
public class CommercialPromoterController {

    private final PromotorComercialRepository repository;

    public CommercialPromoterController(PromotorComercialRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<CommercialPromoterResponse> findAll(
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        List<PromotorComercial> promoters = includeInactive
                ? repository.findAllByOrderByNombreAsc()
                : repository.findAllByActivoTrueOrderByNombreAsc();
        return promoters.stream().map(CommercialPromoterController::toResponse).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommercialPromoterResponse create(@Valid @RequestBody CommercialPromoterRequest request) {
        String nombre = request.getNombre().trim();
        if (nombre.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre del promotor es obligatorio.");
        }
        if (repository.findByNombreIgnoreCase(nombre).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un promotor con ese nombre.");
        }

        PromotorComercial entity = new PromotorComercial();
        entity.setNombre(nombre);
        entity.setActivo(request.getActivo() == null || request.getActivo());
        entity.setCreatedAt(OffsetDateTime.now());
        return toResponse(repository.save(entity));
    }

    private static CommercialPromoterResponse toResponse(PromotorComercial entity) {
        return CommercialPromoterResponse.builder()
                .id(entity.getId())
                .nombre(entity.getNombre())
                .activo(Boolean.TRUE.equals(entity.getActivo()))
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
