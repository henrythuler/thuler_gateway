package com.thuler.gateway.usecase.cobranca;

import com.thuler.gateway.dto.request.PagarCobrancaCartaoRequest;
import com.thuler.gateway.dto.request.PagarCobrancaSaldoRequest;
import com.thuler.gateway.dto.response.CobrancaResponse;
import com.thuler.gateway.domain.model.Cobranca;
import com.thuler.gateway.domain.model.Conta;
import com.thuler.gateway.domain.enums.TipoPagamento;
import com.thuler.gateway.domain.repository.CobrancaRepository;
import com.thuler.gateway.domain.repository.ContaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PagarCobrancaUseCase {

    private final CobrancaRepository cobrancaRepository;
    private final ContaRepository contaRepository;

    @Transactional
    public CobrancaResponse pagarComSaldo(Long pagadorId, PagarCobrancaSaldoRequest request) {
        Cobranca cobranca = cobrancaRepository.findById(request.getCobrancaId())
                .orElseThrow(() -> new IllegalArgumentException("Cobrança não encontrada"));

        if (!cobranca.getDestinatario().getId().equals(pagadorId)) {
            throw new IllegalArgumentException("Apenas o destinatário pode pagar esta cobrança");
        }

        Conta contaPagador = contaRepository.findByUsuarioId(pagadorId)
                .orElseThrow(() -> new IllegalArgumentException("Conta do pagador não encontrada"));

        Conta contaRecebedor = contaRepository.findByUsuarioId(cobranca.getOriginador().getId())
                .orElseThrow(() -> new IllegalArgumentException("Conta do recebedor não encontrada"));

        if (!contaPagador.temSaldoSuficiente(cobranca.getValor())) {
            throw new IllegalArgumentException("Saldo insuficiente");
        }

        contaPagador.debitar(cobranca.getValor());
        contaRecebedor.creditar(cobranca.getValor());

        cobranca.marcarComoPaga(TipoPagamento.SALDO, null, null);

        contaRepository.save(contaPagador);
        contaRepository.save(contaRecebedor);
        cobranca = cobrancaRepository.save(cobranca);

        return CobrancaResponse.fromEntity(cobranca);
    }

    @Transactional
    public CobrancaResponse pagarComCartao(Long pagadorId, PagarCobrancaCartaoRequest request) {
        Cobranca cobranca = cobrancaRepository.findById(request.getCobrancaId())
                .orElseThrow(() -> new IllegalArgumentException("Cobrança não encontrada"));

        if (!cobranca.getDestinatario().getId().equals(pagadorId)) {
            throw new IllegalArgumentException("Apenas o destinatário pode pagar esta cobrança");
        }

        String ultimosDigitos = request.getNumeroCartao().substring(12);
        String authorizerResponse = "APPROVED";

        Conta contaRecebedor = contaRepository.findByUsuarioId(cobranca.getOriginador().getId())
                .orElseThrow(() -> new IllegalArgumentException("Conta do recebedor não encontrada"));

        contaRecebedor.creditar(cobranca.getValor());

        cobranca.marcarComoPaga(TipoPagamento.CARTAO_CREDITO, ultimosDigitos, authorizerResponse);

        contaRepository.save(contaRecebedor);
        cobranca = cobrancaRepository.save(cobranca);

        return CobrancaResponse.fromEntity(cobranca);
    }
}