package com.thuler.gateway.usecase.cobranca;

import com.thuler.gateway.dto.request.PagarCobrancaCartaoRequest;
import com.thuler.gateway.dto.request.PagarCobrancaSaldoRequest;
import com.thuler.gateway.dto.response.CobrancaResponse;
import com.thuler.gateway.domain.model.Cobranca;
import com.thuler.gateway.domain.model.Conta;
import com.thuler.gateway.domain.model.Usuario;
import com.thuler.gateway.domain.enums.CobrancaStatus;
import com.thuler.gateway.domain.enums.TipoPagamento;
import com.thuler.gateway.domain.repository.CobrancaRepository;
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
@DisplayName("PagarCobrancaUseCase Tests")
class PagarCobrancaUseCaseTest {

    @Mock
    private CobrancaRepository cobrancaRepository;

    @Mock
    private ContaRepository contaRepository;

    @Mock
    private AuthorizerClient authorizerClient;

    @InjectMocks
    private PagarCobrancaUseCase pagarCobrancaUseCase;

    private Usuario originador;
    private Usuario destinatario;
    private Conta contaOriginador;
    private Conta contaDestinatario;
    private Cobranca cobranca;

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

        contaOriginador = Conta.builder()
                .id(1L)
                .usuario(originador)
                .saldo(BigDecimal.valueOf(500))
                .build();

        contaDestinatario = Conta.builder()
                .id(2L)
                .usuario(destinatario)
                .saldo(BigDecimal.valueOf(1000))
                .build();

        cobranca = Cobranca.builder()
                .id(1L)
                .originador(originador)
                .destinatario(destinatario)
                .valor(BigDecimal.valueOf(100))
                .descricao("Cobrança teste")
                .status(CobrancaStatus.PENDENTE)
                .build();
    }

    @Test
    @DisplayName("Deve pagar cobrança com saldo com sucesso")
    void devePagarCobrancaComSaldoComSucesso() {
        PagarCobrancaSaldoRequest request = PagarCobrancaSaldoRequest.builder()
                .cobrancaId(1L)
                .build();

        when(cobrancaRepository.findById(1L)).thenReturn(Optional.of(cobranca));
        when(contaRepository.findByUsuarioId(2L)).thenReturn(Optional.of(contaDestinatario));
        when(contaRepository.findByUsuarioId(1L)).thenReturn(Optional.of(contaOriginador));

        Cobranca cobrancaPaga = Cobranca.builder()
                .id(1L)
                .originador(originador)
                .destinatario(destinatario)
                .valor(BigDecimal.valueOf(100))
                .status(CobrancaStatus.PAGA)
                .tipoPagamento(TipoPagamento.SALDO)
                .build();

        when(cobrancaRepository.save(any(Cobranca.class))).thenReturn(cobrancaPaga);

        CobrancaResponse response = pagarCobrancaUseCase.pagarComSaldo(2L, request);

        assertNotNull(response);
        assertEquals(CobrancaStatus.PAGA, response.getStatus());
        assertEquals(TipoPagamento.SALDO, response.getTipoPagamento());

        verify(cobrancaRepository).findById(1L);
        verify(contaRepository).findByUsuarioId(2L);
        verify(contaRepository).findByUsuarioId(1L);
        verify(contaRepository, times(2)).save(any(Conta.class));
        verify(cobrancaRepository).save(any(Cobranca.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao pagar com saldo insuficiente")
    void deveLancarExcecaoAoPagarComSaldoInsuficiente() {
        contaDestinatario.setSaldo(BigDecimal.valueOf(50)); // Saldo menor que o valor da cobrança

        PagarCobrancaSaldoRequest request = PagarCobrancaSaldoRequest.builder()
                .cobrancaId(1L)
                .build();

        when(cobrancaRepository.findById(1L)).thenReturn(Optional.of(cobranca));
        when(contaRepository.findByUsuarioId(2L)).thenReturn(Optional.of(contaDestinatario));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> pagarCobrancaUseCase.pagarComSaldo(2L, request)
        );

        assertEquals("Saldo insuficiente", exception.getMessage());
        verify(cobrancaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar exceção quando usuário não é o destinatário")
    void deveLancarExcecaoQuandoUsuarioNaoEDestinatario() {
        PagarCobrancaSaldoRequest request = PagarCobrancaSaldoRequest.builder()
                .cobrancaId(1L)
                .build();

        when(cobrancaRepository.findById(1L)).thenReturn(Optional.of(cobranca));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> pagarCobrancaUseCase.pagarComSaldo(999L, request) // ID diferente do destinatário
        );

        assertEquals("Apenas o destinatário pode pagar esta cobrança", exception.getMessage());
        verify(contaRepository, never()).findByUsuarioId(any());
        verify(cobrancaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar pagar cobrança não pendente")
    void deveLancarExcecaoAoTentarPagarCobrancaNaoPendente() {
        cobranca.setStatus(CobrancaStatus.PAGA);

        PagarCobrancaSaldoRequest request = PagarCobrancaSaldoRequest.builder()
                .cobrancaId(1L)
                .build();

        when(cobrancaRepository.findById(1L)).thenReturn(Optional.of(cobranca));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> pagarCobrancaUseCase.pagarComSaldo(2L, request)
        );

        assertEquals("Apenas cobranças pendentes podem ser pagas", exception.getMessage());
    }

    @Test
    @DisplayName("Deve pagar cobrança com cartão com sucesso")
    void devePagarCobrancaComCartaoComSucesso() {
        PagarCobrancaCartaoRequest request = PagarCobrancaCartaoRequest.builder()
                .cobrancaId(1L)
                .numeroCartao("1234567890123456")
                .dataExpiracao("12/25")
                .cvv("123")
                .build();

        AuthorizerResponse authorizerResponse = new AuthorizerResponse();
        authorizerResponse.setStatus("APPROVED");
        authorizerResponse.setData(new AuthorizerResponse.AuthorizerData(true));

        when(cobrancaRepository.findById(1L)).thenReturn(Optional.of(cobranca));
        when(authorizerClient.authorize()).thenReturn(authorizerResponse);
        when(contaRepository.findByUsuarioId(1L)).thenReturn(Optional.of(contaOriginador));

        Cobranca cobrancaPaga = Cobranca.builder()
                .id(1L)
                .originador(originador)
                .destinatario(destinatario)
                .valor(BigDecimal.valueOf(100))
                .status(CobrancaStatus.PAGA)
                .tipoPagamento(TipoPagamento.CARTAO_CREDITO)
                .numeroCartao("3456")
                .build();

        when(cobrancaRepository.save(any(Cobranca.class))).thenReturn(cobrancaPaga);

        // Act
        CobrancaResponse response = pagarCobrancaUseCase.pagarComCartao(2L, request);

        // Assert
        assertNotNull(response);
        assertEquals(CobrancaStatus.PAGA, response.getStatus());
        assertEquals(TipoPagamento.CARTAO_CREDITO, response.getTipoPagamento());
        assertEquals("3456", response.getNumeroCartao());

        verify(authorizerClient).authorize();
        verify(contaRepository).save(any(Conta.class));
        verify(cobrancaRepository).save(any(Cobranca.class));
    }

    @Test
    @DisplayName("Deve lançar exceção quando autorizador nega pagamento com cartão")
    void deveLancarExcecaoQuandoAutorizadorNegaPagamento() {
        // Arrange
        PagarCobrancaCartaoRequest request = PagarCobrancaCartaoRequest.builder()
                .cobrancaId(1L)
                .numeroCartao("1234567890123456")
                .dataExpiracao("12/25")
                .cvv("123")
                .build();

        AuthorizerResponse authorizerResponse = new AuthorizerResponse();
        authorizerResponse.setStatus("DENIED");
        authorizerResponse.setData(new AuthorizerResponse.AuthorizerData(false));

        when(cobrancaRepository.findById(1L)).thenReturn(Optional.of(cobranca));
        when(authorizerClient.authorize()).thenReturn(authorizerResponse);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> pagarCobrancaUseCase.pagarComCartao(2L, request)
        );

        assertEquals("Pagamento não autorizado pelo autorizador externo", exception.getMessage());
        verify(authorizerClient).authorize();
        verify(cobrancaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar exceção quando cobrança não existe")
    void deveLancarExcecaoQuandoCobrancaNaoExiste() {
        // Arrange
        PagarCobrancaSaldoRequest request = PagarCobrancaSaldoRequest.builder()
                .cobrancaId(999L)
                .build();

        when(cobrancaRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> pagarCobrancaUseCase.pagarComSaldo(2L, request)
        );

        assertEquals("Cobrança não encontrada", exception.getMessage());
    }
}