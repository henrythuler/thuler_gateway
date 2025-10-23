package com.thuler.gateway.usecase.cobranca;

import com.thuler.gateway.dto.response.CobrancaResponse;
import com.thuler.gateway.domain.model.Cobranca;
import com.thuler.gateway.domain.model.Usuario;
import com.thuler.gateway.domain.enums.CobrancaStatus;
import com.thuler.gateway.domain.repository.CobrancaRepository;
import com.thuler.gateway.domain.valueobject.CPF;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConsultarCobrancasUseCase Tests")
class ConsultarCobrancasUseCaseTest {

    @Mock
    private CobrancaRepository cobrancaRepository;

    @InjectMocks
    private ConsultarCobrancasUseCase consultarCobrancasUseCase;

    private Usuario originador;
    private Usuario destinatario;

    @BeforeEach
    void setUp() {
        originador = Usuario.builder()
                .id(1L)
                .nome("João Silva")
                .cpf(CPF.of("12345678909"))
                .email("joao@example.com")
                .build();

        destinatario = Usuario.builder()
                .id(2L)
                .nome("Maria Santos")
                .cpf(CPF.of("98765432100"))
                .email("maria@example.com")
                .build();
    }

    @Test
    @DisplayName("Deve consultar cobranças enviadas sem filtro de status")
    void deveConsultarCobrancasEnviadasSemFiltro() {
        List<Cobranca> cobrancas = Arrays.asList(
                Cobranca.builder()
                        .id(1L)
                        .originador(originador)
                        .destinatario(destinatario)
                        .valor(BigDecimal.valueOf(100))
                        .status(CobrancaStatus.PENDENTE)
                        .build(),
                Cobranca.builder()
                        .id(2L)
                        .originador(originador)
                        .destinatario(destinatario)
                        .valor(BigDecimal.valueOf(200))
                        .status(CobrancaStatus.PAGA)
                        .build()
        );

        when(cobrancaRepository.findByOriginadorId(1L)).thenReturn(cobrancas);

        List<CobrancaResponse> responses = consultarCobrancasUseCase.consultarCobrancasEnviadas(1L, null);

        assertNotNull(responses);
        assertEquals(2, responses.size());
        verify(cobrancaRepository).findByOriginadorId(1L);
        verify(cobrancaRepository, never()).findByOriginadorIdAndStatus(any(), any());
    }

    @Test
    @DisplayName("Deve consultar cobranças enviadas com filtro de status")
    void deveConsultarCobrancasEnviadasComFiltro() {
        List<Cobranca> cobrancas = Collections.singletonList(
                Cobranca.builder()
                        .id(1L)
                        .originador(originador)
                        .destinatario(destinatario)
                        .valor(BigDecimal.valueOf(100))
                        .status(CobrancaStatus.PENDENTE)
                        .build()
        );

        when(cobrancaRepository.findByOriginadorIdAndStatus(1L, CobrancaStatus.PENDENTE))
                .thenReturn(cobrancas);

        List<CobrancaResponse> responses = consultarCobrancasUseCase
                .consultarCobrancasEnviadas(1L, CobrancaStatus.PENDENTE);

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(CobrancaStatus.PENDENTE, responses.get(0).getStatus());
        verify(cobrancaRepository).findByOriginadorIdAndStatus(1L, CobrancaStatus.PENDENTE);
    }

    @Test
    @DisplayName("Deve consultar cobranças recebidas sem filtro de status")
    void deveConsultarCobrancasRecebidasSemFiltro() {
        List<Cobranca> cobrancas = Arrays.asList(
                Cobranca.builder()
                        .id(1L)
                        .originador(originador)
                        .destinatario(destinatario)
                        .valor(BigDecimal.valueOf(100))
                        .status(CobrancaStatus.PENDENTE)
                        .build()
        );

        when(cobrancaRepository.findByDestinatarioId(2L)).thenReturn(cobrancas);

        List<CobrancaResponse> responses = consultarCobrancasUseCase.consultarCobrancasRecebidas(2L, null);

        assertNotNull(responses);
        assertEquals(1, responses.size());
        verify(cobrancaRepository).findByDestinatarioId(2L);
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando não há cobranças")
    void deveRetornarListaVaziaQuandoNaoHaCobrancas() {
        when(cobrancaRepository.findByOriginadorId(1L)).thenReturn(Collections.emptyList());

        List<CobrancaResponse> responses = consultarCobrancasUseCase.consultarCobrancasEnviadas(1L, null);

        assertNotNull(responses);
        assertTrue(responses.isEmpty());
    }
}