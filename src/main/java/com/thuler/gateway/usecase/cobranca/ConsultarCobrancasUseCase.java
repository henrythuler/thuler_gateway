package com.thuler.gateway.usecase.cobranca;

import com.thuler.gateway.dto.response.CobrancaResponse;
import com.thuler.gateway.domain.model.Cobranca;
import com.thuler.gateway.domain.enums.CobrancaStatus;
import com.thuler.gateway.domain.repository.CobrancaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConsultarCobrancasUseCase {

    private final CobrancaRepository cobrancaRepository;

    public List<CobrancaResponse> consultarCobrancasEnviadas(Long usuarioId, CobrancaStatus status) {
        List<Cobranca> cobrancas;

        if (status != null) {
            cobrancas = cobrancaRepository.findByOriginadorIdAndStatus(usuarioId, status);
        } else {
            cobrancas = cobrancaRepository.findByOriginadorId(usuarioId);
        }

        return cobrancas.stream()
                .map(CobrancaResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<CobrancaResponse> consultarCobrancasRecebidas(Long usuarioId, CobrancaStatus status) {
        List<Cobranca> cobrancas;

        if (status != null) {
            cobrancas = cobrancaRepository.findByDestinatarioIdAndStatus(usuarioId, status);
        } else {
            cobrancas = cobrancaRepository.findByDestinatarioId(usuarioId);
        }

        return cobrancas.stream()
                .map(CobrancaResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
