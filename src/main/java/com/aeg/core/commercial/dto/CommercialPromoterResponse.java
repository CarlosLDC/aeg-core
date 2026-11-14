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
public class CommercialPromoterResponse {

    private Long id;
    private String nombre;
    private boolean activo;
    private OffsetDateTime createdAt;
}
