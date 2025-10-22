package com.thuler.gateway.usecase.usuario;

import com.thuler.gateway.dto.request.CadastroUsuarioRequest;
import com.thuler.gateway.dto.response.UsuarioResponse;
import com.thuler.gateway.domain.model.Usuario;
import com.thuler.gateway.domain.repository.UsuarioRepository;
import com.thuler.gateway.domain.valueobject.CPF;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CadastrarUsuarioUseCase {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UsuarioResponse execute(CadastroUsuarioRequest request) {
        CPF cpf = CPF.of(request.getCpf());

        if (usuarioRepository.existsByCpf(cpf)) {
            throw new IllegalArgumentException("CPF já cadastrado");
        }

        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email já cadastrado");
        }

        Usuario usuario = Usuario.builder()
                .nome(request.getNome())
                .cpf(cpf)
                .email(request.getEmail())
                .senha(passwordEncoder.encode(request.getSenha()))
                .active(true)
                .build();

        usuario.criarConta();

        usuario = usuarioRepository.save(usuario);

        return UsuarioResponse.fromEntity(usuario);
    }
}
