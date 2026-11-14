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

import com.aeg.core.commercial.dto.CommercialClientRequest;
import com.aeg.core.commercial.dto.CommercialClientResponse;

@RestController
@RequestMapping("/api/commercial-clients")
@Transactional
public class CommercialClientController {

    private final ClienteComercialRepository repository;

    public CommercialClientController(ClienteComercialRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<CommercialClientResponse> findAll(
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        List<ClienteComercial> clients = includeInactive
                ? repository.findAllByOrderByNombreAsc()
                : repository.findAllByActivoTrueOrderByNombreAsc();
        return clients.stream().map(CommercialClientController::toResponse).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommercialClientResponse create(@Valid @RequestBody CommercialClientRequest request) {
        String nombre = request.getNombre().trim();
        if (nombre.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre del cliente es obligatorio.");
        }
        if (repository.findByNombreIgnoreCase(nombre).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un cliente comercial con ese nombre.");
        }

        ClienteComercial entity = new ClienteComercial();
        entity.setNombre(nombre);
        entity.setRif(trimToNull(request.getRif()));
        entity.setNotas(trimToNull(request.getNotas()));
        entity.setActivo(request.getActivo() == null || request.getActivo());
        entity.setCreatedAt(OffsetDateTime.now());
        return toResponse(repository.save(entity));
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static CommercialClientResponse toResponse(ClienteComercial entity) {
        return CommercialClientResponse.builder()
                .id(entity.getId())
                .nombre(entity.getNombre())
                .rif(entity.getRif())
                .notas(entity.getNotas())
                .activo(Boolean.TRUE.equals(entity.getActivo()))
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
