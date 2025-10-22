package com.thuler.gateway.dto.response;

import com.thuler.gateway.domain.model.Conta;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContaResponse {

    private Long id;
    private Long usuarioId;
    private BigDecimal saldo;
    private LocalDateTime createdAt;

    public static ContaResponse fromEntity(Conta conta) {
        return ContaResponse.builder()
                .id(conta.getId())
                .usuarioId(conta.getUsuario().getId())
                .saldo(conta.getSaldo())
                .createdAt(conta.getCreatedAt())
                .build();
    }
}
