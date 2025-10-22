package com.thuler.gateway.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagarCobrancaCartaoRequest {

    @NotNull(message = "ID da cobrança é obrigatório")
    private Long cobrancaId;

    @NotBlank(message = "Número do cartão é obrigatório")
    @Pattern(regexp = "\\d{16}", message = "Número do cartão deve ter 16 dígitos")
    private String numeroCartao;

    @NotBlank(message = "Data de expiração é obrigatória")
    @Pattern(regexp = "(0[1-9]|1[0-2])/\\d{2}", message = "Data de expiração deve estar no formato MM/YY")
    private String dataExpiracao;

    @NotBlank(message = "CVV é obrigatório")
    @Size(min = 3, max = 4, message = "CVV deve ter 3 ou 4 dígitos")
    private String cvv;
}
