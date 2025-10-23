package com.thuler.gateway.domain.repository;

import com.thuler.gateway.domain.model.Cobranca;
import com.thuler.gateway.domain.enums.CobrancaStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CobrancaRepository extends JpaRepository<Cobranca, Long> {

    List<Cobranca> findByOriginadorIdAndStatus(Long originadorId, CobrancaStatus status);

    List<Cobranca> findByDestinatarioIdAndStatus(Long destinatarioId, CobrancaStatus status);

    List<Cobranca> findByOriginadorId(Long originadorId);

    List<Cobranca> findByDestinatarioId(Long destinatarioId);
}