package com.thuler.gateway.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private String token;
    private String type;
    private UsuarioResponse usuario;

    public static LoginResponse of(String token, UsuarioResponse usuario) {
        return LoginResponse.builder()
                .token(token)
                .type("Bearer")
                .usuario(usuario)
                .build();
    }
}
