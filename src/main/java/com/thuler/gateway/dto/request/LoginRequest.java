package com.thuler.gateway.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "CPF ou Email é obrigatório")
    private String identificador;

    @NotBlank(message = "Senha é obrigatória")
    private String senha;
}
