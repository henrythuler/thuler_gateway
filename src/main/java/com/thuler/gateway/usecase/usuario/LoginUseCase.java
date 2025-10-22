package com.thuler.gateway.usecase.usuario;

import com.thuler.gateway.dto.request.LoginRequest;
import com.thuler.gateway.dto.response.LoginResponse;
import com.thuler.gateway.dto.response.UsuarioResponse;
import com.thuler.gateway.domain.model.Usuario;
import com.thuler.gateway.domain.repository.UsuarioRepository;
import com.thuler.gateway.domain.valueobject.CPF;
import com.thuler.gateway.infrastructure.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginUseCase {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public LoginResponse execute(LoginRequest request) {
        Usuario usuario = buscarUsuario(request.getIdentificador());

        if (!passwordEncoder.matches(request.getSenha(), usuario.getSenha())) {
            throw new IllegalArgumentException("Credenciais inválidas");
        }

        if (!usuario.getActive()) {
            throw new IllegalArgumentException("Usuário inativo");
        }

        String token = jwtTokenProvider.generateToken(usuario.getId(), usuario.getEmail());

        return LoginResponse.of(token, UsuarioResponse.fromEntity(usuario));
    }

    private Usuario buscarUsuario(String identificador) {
        if (identificador.contains("@")) {
            return usuarioRepository.findByEmail(identificador)
                    .orElseThrow(() -> new IllegalArgumentException("Credenciais inválidas"));
        }

        CPF cpf = CPF.of(identificador);
        return usuarioRepository.findByCpf(cpf)
                .orElseThrow(() -> new IllegalArgumentException("Credenciais inválidas"));
    }
}