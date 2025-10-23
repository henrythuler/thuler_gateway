package com.thuler.gateway.usecase.cobranca;

import com.thuler.gateway.dto.request.PagarCobrancaCartaoRequest;
import com.thuler.gateway.dto.request.PagarCobrancaSaldoRequest;
import com.thuler.gateway.dto.response.CobrancaResponse;
import com.thuler.gateway.domain.model.Cobranca;
import com.thuler.gateway.domain.model.Conta;
import com.thuler.gateway.domain.enums.TipoPagamento;
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
public class PagarCobrancaUseCase {

    private final CobrancaRepository cobrancaRepository;
    private final ContaRepository contaRepository;
    private final AuthorizerClient authorizerClient;

    @Transactional
    public CobrancaResponse pagarComSaldo(Long pagadorId, PagarCobrancaSaldoRequest request) {
        log.info("Iniciando pagamento com saldo da cobrança ID: {} pelo usuário ID: {}",
                request.getCobrancaId(), pagadorId);

        Cobranca cobranca = cobrancaRepository.findById(request.getCobrancaId())
                .orElseThrow(() -> new IllegalArgumentException("Cobrança não encontrada"));

        if (!cobranca.isPendente()) {
            log.warn("Tentativa de pagar cobrança com status: {}", cobranca.getStatus());
            throw new IllegalStateException("Apenas cobranças pendentes podem ser pagas");
        }

        if (!cobranca.getDestinatario().getId().equals(pagadorId)) {
            log.warn("Usuário ID: {} tentou pagar cobrança que não é destinatário", pagadorId);
            throw new IllegalArgumentException("Apenas o destinatário pode pagar esta cobrança");
        }

        if (!cobranca.isPendente()) {
            log.warn("Tentativa de pagar cobrança com status: {}", cobranca.getStatus());
            throw new IllegalStateException("Apenas cobranças pendentes podem ser pagas");
        }

        Conta contaPagador = contaRepository.findByUsuarioId(pagadorId)
                .orElseThrow(() -> new IllegalArgumentException("Conta do pagador não encontrada"));

        Conta contaRecebedor = contaRepository.findByUsuarioId(cobranca.getOriginador().getId())
                .orElseThrow(() -> new IllegalArgumentException("Conta do recebedor não encontrada"));

        if (!contaPagador.temSaldoSuficiente(cobranca.getValor())) {
            log.warn("Saldo insuficiente para pagamento. Saldo: R$ {}, Valor cobrança: R$ {}",
                    contaPagador.getSaldo(), cobranca.getValor());
            throw new IllegalArgumentException("Saldo insuficiente");
        }

        contaPagador.debitar(cobranca.getValor());
        contaRecebedor.creditar(cobranca.getValor());

        cobranca.marcarComoPaga(TipoPagamento.SALDO, null, "PAGAMENTO_SALDO");

        contaRepository.save(contaPagador);
        contaRepository.save(contaRecebedor);
        cobranca = cobrancaRepository.save(cobranca);

        log.info("Pagamento com saldo realizado com sucesso. Cobrança ID: {}", cobranca.getId());

        return CobrancaResponse.fromEntity(cobranca);
    }

    @Transactional
    public CobrancaResponse pagarComCartao(Long pagadorId, PagarCobrancaCartaoRequest request) {
        log.info("Iniciando pagamento com cartão da cobrança ID: {} pelo usuário ID: {}",
                request.getCobrancaId(), pagadorId);

        Cobranca cobranca = cobrancaRepository.findById(request.getCobrancaId())
                .orElseThrow(() -> new IllegalArgumentException("Cobrança não encontrada"));

        if (!cobranca.isPendente()) {
            log.warn("Tentativa de pagar cobrança com status: {}", cobranca.getStatus());
            throw new IllegalStateException("Apenas cobranças pendentes podem ser pagas");
        }

        if (!cobranca.getDestinatario().getId().equals(pagadorId)) {
            log.warn("Usuário ID: {} tentou pagar cobrança que não é destinatário", pagadorId);
            throw new IllegalArgumentException("Apenas o destinatário pode pagar esta cobrança");
        }

        if (!cobranca.isPendente()) {
            log.warn("Tentativa de pagar cobrança com status: {}", cobranca.getStatus());
            throw new IllegalStateException("Apenas cobranças pendentes podem ser pagas");
        }

        log.info("Consultando autorizador externo para pagamento com cartão");
        AuthorizerResponse authorizerResponse = authorizerClient.authorize();

        if (!authorizerResponse.isAutorizado()) {
            log.warn("Pagamento com cartão não autorizado para cobrança ID: {}", request.getCobrancaId());
            throw new IllegalArgumentException("Pagamento não autorizado pelo autorizador externo");
        }

        log.info("Pagamento com cartão autorizado pelo autorizador externo");

        String ultimos4Digitos = request.getNumeroCartao().substring(12);
        String authorizerResponseStr = String.format(
                "APPROVED - Status: %s, Authorization: %s",
                authorizerResponse.getStatus(),
                authorizerResponse.getData().getAuthorized()
        );

        Conta contaRecebedor = contaRepository.findByUsuarioId(cobranca.getOriginador().getId())
                .orElseThrow(() -> new IllegalArgumentException("Conta do recebedor não encontrada"));

        contaRecebedor.creditar(cobranca.getValor());

        cobranca.marcarComoPaga(TipoPagamento.CARTAO_CREDITO, ultimos4Digitos, authorizerResponseStr);

        contaRepository.save(contaRecebedor);
        cobranca = cobrancaRepository.save(cobranca);

        log.info("Pagamento com cartão realizado com sucesso. Cobrança ID: {}, Últimos 4 dígitos: {}",
                cobranca.getId(), ultimos4Digitos);

        return CobrancaResponse.fromEntity(cobranca);
    }
}