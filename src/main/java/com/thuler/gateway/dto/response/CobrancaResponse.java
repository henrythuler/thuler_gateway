package com.thuler.gateway.dto.response;

import com.thuler.gateway.domain.model.Cobranca;
import com.thuler.gateway.domain.enums.CobrancaStatus;
import com.thuler.gateway.domain.enums.TipoPagamento;
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
public class CobrancaResponse {

    private Long id;
    private Long originadorId;
    private String originadorNome;
    private Long destinatarioId;
    private String destinatarioNome;
    private BigDecimal valor;
    private String descricao;
    private CobrancaStatus status;
    private TipoPagamento tipoPagamento;
    private String numeroCartao;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
    private LocalDateTime cancelledAt;

    public static CobrancaResponse fromEntity(Cobranca cobranca) {
        return CobrancaResponse.builder()
                .id(cobranca.getId())
                .originadorId(cobranca.getOriginador().getId())
                .originadorNome(cobranca.getOriginador().getNome())
                .destinatarioId(cobranca.getDestinatario().getId())
                .destinatarioNome(cobranca.getDestinatario().getNome())
                .valor(cobranca.getValor())
                .descricao(cobranca.getDescricao())
                .status(cobranca.getStatus())
                .tipoPagamento(cobranca.getTipoPagamento())
                .numeroCartao(cobranca.getNumeroCartao())
                .createdAt(cobranca.getCreatedAt())
                .paidAt(cobranca.getPaidAt())
                .cancelledAt(cobranca.getCancelledAt())
                .build();
    }
}