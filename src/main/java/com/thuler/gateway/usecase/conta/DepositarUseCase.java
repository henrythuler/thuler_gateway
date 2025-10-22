package com.thuler.gateway.usecase.conta;

import com.thuler.gateway.dto.request.DepositoRequest;
import com.thuler.gateway.dto.response.ContaResponse;
import com.thuler.gateway.domain.model.Conta;
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
public class DepositarUseCase {

    private final ContaRepository contaRepository;
    private final AuthorizerClient authorizerClient;

    @Transactional
    public ContaResponse execute(Long usuarioId, DepositoRequest request) {
        log.info("Iniciando depósito de R$ {} para usuário ID: {}", request.getValor(), usuarioId);

        Conta conta = contaRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada"));

        log.info("Consultando autorizador externo para depósito");
        AuthorizerResponse authorizerResponse = authorizerClient.authorize();

        if (!authorizerResponse.isAutorizado()) {
            log.warn("Depósito não autorizado para usuário ID: {}", usuarioId);
            throw new IllegalArgumentException("Depósito não autorizado pelo autorizador externo");
        }

        log.info("Depósito autorizado pelo autorizador externo");
        conta.depositar(request.getValor());
        conta = contaRepository.save(conta);

        log.info("Depósito realizado com sucesso. Novo saldo: R$ {}", conta.getSaldo());

        return ContaResponse.fromEntity(conta);
    }
}