package com.aeg.core.commercial.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CommercialClientRequest {

    @NotBlank
    @Size(max = 120)
    private String nombre;

    @Size(max = 20)
    private String rif;

    @Size(max = 255)
    private String notas;

    private Boolean activo;
}
