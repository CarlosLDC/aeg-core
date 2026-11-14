package com.aeg.core.commercial.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CommercialPromoterRequest {

    @NotBlank
    @Size(max = 100)
    private String nombre;

    private Boolean activo;
}
