package com.thuler.gateway.infrastructure.security;

import com.thuler.gateway.domain.model.Usuario;
import com.thuler.gateway.domain.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + email));

        return new AuthenticatedUser(
                usuario.getId(),
                usuario.getEmail(),
                usuario.getSenha(),
                usuario.getActive()
        );
    }

    public UserDetails loadUserById(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + id));

        return new AuthenticatedUser(
                usuario.getId(),
                usuario.getEmail(),
                usuario.getSenha(),
                usuario.getActive()
        );
    }
}