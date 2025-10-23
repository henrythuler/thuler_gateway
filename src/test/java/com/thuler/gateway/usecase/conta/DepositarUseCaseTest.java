package com.thuler.gateway.usecase.conta;

import com.thuler.gateway.dto.request.DepositoRequest;
import com.thuler.gateway.dto.response.ContaResponse;
import com.thuler.gateway.domain.model.Conta;
import com.thuler.gateway.domain.model.Usuario;
import com.thuler.gateway.domain.repository.ContaRepository;
import com.thuler.gateway.domain.valueobject.CPF;
import com.thuler.gateway.infrastructure.external.authorizer.AuthorizerClient;
import com.thuler.gateway.infrastructure.external.authorizer.AuthorizerResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DepositarUseCase Tests")
class DepositarUseCaseTest {

    @Mock
    private ContaRepository contaRepository;

    @Mock
    private AuthorizerClient authorizerClient;

    @InjectMocks
    private DepositarUseCase depositarUseCase;

    private Usuario usuario;
    private Conta conta;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder()
                .id(1L)
                .nome("João Silva")
                .cpf(CPF.of("12345678909"))
                .email("joao@example.com")
                .build();

        conta = Conta.builder()
                .id(1L)
                .usuario(usuario)
                .saldo(BigDecimal.valueOf(500))
                .build();
    }

    @Test
    @DisplayName("Deve depositar valor com sucesso")
    void deveDepositarValorComSucesso() {
        DepositoRequest request = DepositoRequest.builder()
                .valor(BigDecimal.valueOf(100))
                .build();

        AuthorizerResponse authorizerResponse = new AuthorizerResponse();
        authorizerResponse.setStatus("APPROVED");
        authorizerResponse.setData(new AuthorizerResponse.AuthorizerData(true));

        when(contaRepository.findByUsuarioId(1L)).thenReturn(Optional.of(conta));
        when(authorizerClient.authorize()).thenReturn(authorizerResponse);

        Conta contaAtualizada = Conta.builder()
                .id(1L)
                .usuario(usuario)
                .saldo(BigDecimal.valueOf(600))
                .build();

        when(contaRepository.save(any(Conta.class))).thenReturn(contaAtualizada);

        ContaResponse response = depositarUseCase.execute(1L, request);

        assertNotNull(response);
        assertEquals(BigDecimal.valueOf(600), response.getSaldo());

        verify(contaRepository).findByUsuarioId(1L);
        verify(authorizerClient).authorize();
        verify(contaRepository).save(any(Conta.class));
    }

    @Test
    @DisplayName("Deve lançar exceção quando autorizador nega depósito")
    void deveLancarExcecaoQuandoAutorizadorNegaDeposito() {
        DepositoRequest request = DepositoRequest.builder()
                .valor(BigDecimal.valueOf(100))
                .build();

        AuthorizerResponse authorizerResponse = new AuthorizerResponse();
        authorizerResponse.setStatus("DENIED");
        authorizerResponse.setData(new AuthorizerResponse.AuthorizerData(false));

        when(contaRepository.findByUsuarioId(1L)).thenReturn(Optional.of(conta));
        when(authorizerClient.authorize()).thenReturn(authorizerResponse);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> depositarUseCase.execute(1L, request)
        );

        assertEquals("Depósito não autorizado pelo autorizador externo", exception.getMessage());
        verify(authorizerClient).authorize();
        verify(contaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar exceção quando conta não existe")
    void deveLancarExcecaoQuandoContaNaoExiste() {
        DepositoRequest request = DepositoRequest.builder()
                .valor(BigDecimal.valueOf(100))
                .build();

        when(contaRepository.findByUsuarioId(1L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> depositarUseCase.execute(1L, request)
        );

        assertEquals("Conta não encontrada", exception.getMessage());
        verify(contaRepository).findByUsuarioId(1L);
        verify(authorizerClient, never()).authorize();
        verify(contaRepository, never()).save(any());
    }
}