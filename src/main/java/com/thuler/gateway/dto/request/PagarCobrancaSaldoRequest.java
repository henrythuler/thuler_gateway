package com.thuler.gateway.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagarCobrancaSaldoRequest {

    @NotNull(message = "ID da cobrança é obrigatório")
    private Long cobrancaId;
}
