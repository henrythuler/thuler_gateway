package com.thuler.gateway.domain.repository;

import com.thuler.gateway.domain.model.Conta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ContaRepository extends JpaRepository<Conta, Long> {

    Optional<Conta> findByUsuarioId(Long usuarioId);
}
