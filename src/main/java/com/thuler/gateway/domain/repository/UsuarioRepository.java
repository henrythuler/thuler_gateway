package com.thuler.gateway.domain.repository;

import com.thuler.gateway.domain.model.Usuario;
import com.thuler.gateway.domain.valueobject.CPF;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCpf(CPF cpf);

    Optional<Usuario> findByEmail(String email);

    boolean existsByCpf(CPF cpf);

    boolean existsByEmail(String email);
}
