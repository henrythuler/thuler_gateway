package com.thuler.gateway.usecase.cobranca;

import com.thuler.gateway.dto.response.CobrancaResponse;
import com.thuler.gateway.domain.model.Cobranca;
import com.thuler.gateway.domain.model.Conta;
import com.thuler.gateway.domain.repository.CobrancaRepository;
import com.thuler.gateway.domain.repository.ContaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CancelarCobrancaUseCase {

    private final CobrancaRepository cobrancaRepository;
    private final ContaRepository contaRepository;

    @Transactional
    public CobrancaResponse execute(Long usuarioId, Long cobrancaId) {
        Cobranca cobranca = cobrancaRepository.findById(cobrancaId)
                .orElseThrow(() -> new IllegalArgumentException("Cobrança não encontrada"));

        if (!cobranca.getOriginador().getId().equals(usuarioId)) {
            throw new IllegalArgumentException("Apenas o originador pode cancelar esta cobrança");
        }

        if (cobranca.isPendente()) {
            cobranca.cancelar(null);

        } else if (cobranca.isPaga()) {
            if (cobranca.foiPagaComSaldo()) {
                estornarPagamentoSaldo(cobranca);
            } else if (cobranca.foiPagaComCartao()) {
                estornarPagamentoCartao(cobranca);
            }
        } else {
            throw new IllegalStateException("Cobrança já está cancelada");
        }

        cobranca = cobrancaRepository.save(cobranca);

        return CobrancaResponse.fromEntity(cobranca);
    }

    private void estornarPagamentoSaldo(Cobranca cobranca) {
        Conta contaPagador = contaRepository.findByUsuarioId(cobranca.getDestinatario().getId())
                .orElseThrow(() -> new IllegalArgumentException("Conta do pagador não encontrada"));

        Conta contaRecebedor = contaRepository.findByUsuarioId(cobranca.getOriginador().getId())
                .orElseThrow(() -> new IllegalArgumentException("Conta do recebedor não encontrada"));

        contaRecebedor.debitar(cobranca.getValor());
        contaPagador.creditar(cobranca.getValor());

        contaRepository.save(contaPagador);
        contaRepository.save(contaRecebedor);

        cobranca.cancelar("ESTORNO_SALDO");
    }

    private void estornarPagamentoCartao(Cobranca cobranca) {
        Conta contaRecebedor = contaRepository.findByUsuarioId(cobranca.getOriginador().getId())
                .orElseThrow(() -> new IllegalArgumentException("Conta do recebedor não encontrada"));

        contaRecebedor.debitar(cobranca.getValor());
        contaRepository.save(contaRecebedor);

        cobranca.cancelar("ESTORNO_CARTAO_APROVADO");
    }
}
