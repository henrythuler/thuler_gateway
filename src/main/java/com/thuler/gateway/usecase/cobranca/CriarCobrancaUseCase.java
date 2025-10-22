package com.thuler.gateway.usecase.cobranca;

import com.thuler.gateway.dto.request.CriarCobrancaRequest;
import com.thuler.gateway.dto.response.CobrancaResponse;
import com.thuler.gateway.domain.model.Cobranca;
import com.thuler.gateway.domain.model.Usuario;
import com.thuler.gateway.domain.repository.CobrancaRepository;
import com.thuler.gateway.domain.repository.UsuarioRepository;
import com.thuler.gateway.domain.valueobject.CPF;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CriarCobrancaUseCase {

    private final CobrancaRepository cobrancaRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional
    public CobrancaResponse execute(Long originadorId, CriarCobrancaRequest request) {
        Usuario originador = usuarioRepository.findById(originadorId)
                .orElseThrow(() -> new IllegalArgumentException("Originador não encontrado"));

        CPF cpfDestinatario = CPF.of(request.getCpfDestinatario());
        Usuario destinatario = usuarioRepository.findByCpf(cpfDestinatario)
                .orElseThrow(() -> new IllegalArgumentException("Destinatário não encontrado"));

        if (originador.getId().equals(destinatario.getId())) {
            throw new IllegalArgumentException("Não é possível criar cobrança para si mesmo");
        }

        Cobranca cobranca = Cobranca.builder()
                .originador(originador)
                .destinatario(destinatario)
                .valor(request.getValor())
                .descricao(request.getDescricao())
                .build();

        cobranca = cobrancaRepository.save(cobranca);

        return CobrancaResponse.fromEntity(cobranca);
    }
}
