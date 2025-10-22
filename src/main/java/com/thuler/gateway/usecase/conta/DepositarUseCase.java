package com.thuler.gateway.usecase.conta;

import com.thuler.gateway.dto.request.DepositoRequest;
import com.thuler.gateway.dto.response.ContaResponse;
import com.thuler.gateway.domain.model.Conta;
import com.thuler.gateway.domain.repository.ContaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DepositarUseCase {

    private final ContaRepository contaRepository;

    @Transactional
    public ContaResponse execute(Long usuarioId, DepositoRequest request) {
        Conta conta = contaRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada"));

        conta.depositar(request.getValor());
        conta = contaRepository.save(conta);

        return ContaResponse.fromEntity(conta);
    }
}