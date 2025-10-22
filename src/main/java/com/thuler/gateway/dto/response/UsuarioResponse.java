package com.thuler.gateway.dto.response;

import com.thuler.gateway.domain.model.Usuario;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioResponse {

    private Long id;
    private String nome;
    private String cpf;
    private String email;
    private Boolean active;
    private LocalDateTime createdAt;

    public static UsuarioResponse fromEntity(Usuario usuario) {
        return UsuarioResponse.builder()
                .id(usuario.getId())
                .nome(usuario.getNome())
                .cpf(usuario.getCpf().getNumeroFormatado())
                .email(usuario.getEmail())
                .active(usuario.getActive())
                .createdAt(usuario.getCreatedAt())
                .build();
    }
}
