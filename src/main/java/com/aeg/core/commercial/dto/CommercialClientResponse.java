package com.aeg.core.commercial.dto;

import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommercialClientResponse {

    private Long id;
    private String nombre;
    private String rif;
    private String notas;
    private boolean activo;
    private OffsetDateTime createdAt;
}
