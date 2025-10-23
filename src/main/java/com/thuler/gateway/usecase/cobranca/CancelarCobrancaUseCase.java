package com.thuler.gateway.usecase.cobranca;

import com.thuler.gateway.dto.response.CobrancaResponse;
import com.thuler.gateway.domain.model.Cobranca;
import com.thuler.gateway.domain.model.Conta;
import com.thuler.gateway.domain.repository.CobrancaRepository;
import com.thuler.gateway.domain.repository.ContaRepository;
import com.thuler.gateway.infrastructure.external.authorizer.AuthorizerClient;
import com.thuler.gateway.infrastructure.external.authorizer.AuthorizerResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CancelarCobrancaUseCase {

    private final CobrancaRepository cobrancaRepository;
    private final ContaRepository contaRepository;
    private final AuthorizerClient authorizerClient;

    @Transactional
    public CobrancaResponse execute(Long usuarioId, Long cobrancaId) {
        log.info("Iniciando cancelamento da cobrança ID: {} pelo usuário ID: {}", cobrancaId, usuarioId);

        Cobranca cobranca = cobrancaRepository.findById(cobrancaId)
                .orElseThrow(() -> new IllegalArgumentException("Cobrança não encontrada"));

        if (!cobranca.getOriginador().getId().equals(usuarioId)) {
            log.warn("Usuário ID: {} tentou cancelar cobrança que não é originador", usuarioId);
            throw new IllegalArgumentException("Apenas o originador pode cancelar esta cobrança");
        }

        if (cobranca.isCancelada()) {
            log.warn("Tentativa de cancelar cobrança já cancelada. Cobrança ID: {}", cobrancaId);
            throw new IllegalStateException("Cobrança já está cancelada");
        }

        if (cobranca.isPendente()) {
            log.info("Cancelando cobrança pendente ID: {}", cobrancaId);
            cobranca.cancelar(null);

        } else if (cobranca.isPaga()) {
            log.info("Cancelando cobrança paga ID: {}. Tipo pagamento: {}",
                    cobrancaId, cobranca.getTipoPagamento());

            if (cobranca.foiPagaComSaldo()) {
                estornarPagamentoSaldo(cobranca);
            } else if (cobranca.foiPagaComCartao()) {
                estornarPagamentoCartao(cobranca);
            }
        }

        cobranca = cobrancaRepository.save(cobranca);

        log.info("Cobrança cancelada com sucesso. Cobrança ID: {}", cobranca.getId());

        return CobrancaResponse.fromEntity(cobranca);
    }

    private void estornarPagamentoSaldo(Cobranca cobranca) {
        log.info("Estornando pagamento com saldo da cobrança ID: {}", cobranca.getId());

        Conta contaPagador = contaRepository.findByUsuarioId(cobranca.getDestinatario().getId())
                .orElseThrow(() -> new IllegalArgumentException("Conta do pagador não encontrada"));

        Conta contaRecebedor = contaRepository.findByUsuarioId(cobranca.getOriginador().getId())
                .orElseThrow(() -> new IllegalArgumentException("Conta do recebedor não encontrada"));

        if (!contaRecebedor.temSaldoSuficiente(cobranca.getValor())) {
            log.error("Recebedor não tem saldo suficiente para estorno. Saldo: R$ {}, Valor estorno: R$ {}",
                    contaRecebedor.getSaldo(), cobranca.getValor());
            throw new IllegalArgumentException("Recebedor não possui saldo suficiente para estorno");
        }

        contaRecebedor.debitar(cobranca.getValor());
        contaPagador.creditar(cobranca.getValor());

        contaRepository.save(contaPagador);
        contaRepository.save(contaRecebedor);

        cobranca.cancelar("ESTORNO_SALDO");

        log.info("Estorno de pagamento com saldo realizado com sucesso. Cobrança ID: {}", cobranca.getId());
    }

    private void estornarPagamentoCartao(Cobranca cobranca) {
        log.info("Estornando pagamento com cartão da cobrança ID: {}", cobranca.getId());

        log.info("Consultando autorizador externo para cancelamento de pagamento com cartão");
        AuthorizerResponse authorizerResponse = authorizerClient.authorize();

        if (!authorizerResponse.isAutorizado()) {
            log.warn("Cancelamento de pagamento com cartão não autorizado. Cobrança ID: {}", cobranca.getId());
            throw new IllegalArgumentException("Cancelamento não autorizado pelo autorizador externo");
        }

        log.info("Cancelamento autorizado pelo autorizador externo");

        Conta contaRecebedor = contaRepository.findByUsuarioId(cobranca.getOriginador().getId())
                .orElseThrow(() -> new IllegalArgumentException("Conta do recebedor não encontrada"));

        if (!contaRecebedor.temSaldoSuficiente(cobranca.getValor())) {
            log.error("Recebedor não tem saldo suficiente para estorno. Saldo: R$ {}, Valor estorno: R$ {}",
                    contaRecebedor.getSaldo(), cobranca.getValor());
            throw new IllegalArgumentException("Recebedor não possui saldo suficiente para estorno");
        }

        contaRecebedor.debitar(cobranca.getValor());
        contaRepository.save(contaRecebedor);

        String authorizerResponseStr = String.format(
                "CANCELLED - Status: %s, Authorization: %s",
                authorizerResponse.getStatus(),
                authorizerResponse.getData().getAuthorized()
        );

        cobranca.cancelar(authorizerResponseStr);

        log.info("Estorno de pagamento com cartão realizado com sucesso. Cobrança ID: {}", cobranca.getId());
    }
}