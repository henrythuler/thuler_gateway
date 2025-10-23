package com.thuler.gateway.usecase.cobranca;

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
@DisplayName("CancelarCobrancaUseCase Tests")
class CancelarCobrancaUseCaseTest {

    @Mock
    private CobrancaRepository cobrancaRepository;

    @Mock
    private ContaRepository contaRepository;

    @Mock
    private AuthorizerClient authorizerClient;

    @InjectMocks
    private CancelarCobrancaUseCase cancelarCobrancaUseCase;

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
    @DisplayName("Deve cancelar cobrança pendente com sucesso")
    void deveCancelarCobrancaPendenteComSucesso() {
        when(cobrancaRepository.findById(1L)).thenReturn(Optional.of(cobranca));

        Cobranca cobrancaCancelada = Cobranca.builder()
                .id(1L)
                .originador(originador)
                .destinatario(destinatario)
                .valor(BigDecimal.valueOf(100))
                .status(CobrancaStatus.CANCELADA)
                .build();

        when(cobrancaRepository.save(any(Cobranca.class))).thenReturn(cobrancaCancelada);

        CobrancaResponse response = cancelarCobrancaUseCase.execute(1L, 1L);

        assertNotNull(response);
        assertEquals(CobrancaStatus.CANCELADA, response.getStatus());

        verify(cobrancaRepository).findById(1L);
        verify(cobrancaRepository).save(any(Cobranca.class));
        verify(contaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve cancelar cobrança paga com saldo e estornar")
    void deveCancelarCobrancaPagaComSaldoEEstornar() {
        cobranca.marcarComoPaga(TipoPagamento.SALDO, null, "PAGAMENTO_SALDO");

        when(cobrancaRepository.findById(1L)).thenReturn(Optional.of(cobranca));
        when(contaRepository.findByUsuarioId(2L)).thenReturn(Optional.of(contaDestinatario));
        when(contaRepository.findByUsuarioId(1L)).thenReturn(Optional.of(contaOriginador));

        Cobranca cobrancaCancelada = Cobranca.builder()
                .id(1L)
                .originador(originador)
                .destinatario(destinatario)
                .valor(BigDecimal.valueOf(100))
                .status(CobrancaStatus.CANCELADA)
                .build();

        when(cobrancaRepository.save(any(Cobranca.class))).thenReturn(cobrancaCancelada);

        CobrancaResponse response = cancelarCobrancaUseCase.execute(1L, 1L);

        assertNotNull(response);
        assertEquals(CobrancaStatus.CANCELADA, response.getStatus());

        verify(contaRepository).findByUsuarioId(2L);
        verify(contaRepository).findByUsuarioId(1L);
        verify(contaRepository, times(2)).save(any(Conta.class));
        verify(cobrancaRepository).save(any(Cobranca.class));
    }

    @Test
    @DisplayName("Deve cancelar cobrança paga com cartão quando autorizado")
    void deveCancelarCobrancaPagaComCartaoQuandoAutorizado() {
        cobranca.marcarComoPaga(TipoPagamento.CARTAO_CREDITO, "1234", "APPROVED");

        AuthorizerResponse authorizerResponse = new AuthorizerResponse();
        authorizerResponse.setStatus("APPROVED");
        authorizerResponse.setData(new AuthorizerResponse.AuthorizerData(true));

        when(cobrancaRepository.findById(1L)).thenReturn(Optional.of(cobranca));
        when(authorizerClient.authorize()).thenReturn(authorizerResponse);
        when(contaRepository.findByUsuarioId(1L)).thenReturn(Optional.of(contaOriginador));

        Cobranca cobrancaCancelada = Cobranca.builder()
                .id(1L)
                .originador(originador)
                .destinatario(destinatario)
                .valor(BigDecimal.valueOf(100))
                .status(CobrancaStatus.CANCELADA)
                .build();

        when(cobrancaRepository.save(any(Cobranca.class))).thenReturn(cobrancaCancelada);

        CobrancaResponse response = cancelarCobrancaUseCase.execute(1L, 1L);

        assertNotNull(response);
        assertEquals(CobrancaStatus.CANCELADA, response.getStatus());

        verify(authorizerClient).authorize();
        verify(contaRepository).save(any(Conta.class));
        verify(cobrancaRepository).save(any(Cobranca.class));
    }

    @Test
    @DisplayName("Deve lançar exceção quando autorizador nega cancelamento")
    void deveLancarExcecaoQuandoAutorizadorNegaCancelamento() {
        cobranca.marcarComoPaga(TipoPagamento.CARTAO_CREDITO, "1234", "APPROVED");

        AuthorizerResponse authorizerResponse = new AuthorizerResponse();
        authorizerResponse.setStatus("DENIED");
        authorizerResponse.setData(new AuthorizerResponse.AuthorizerData(false));

        when(cobrancaRepository.findById(1L)).thenReturn(Optional.of(cobranca));
        when(authorizerClient.authorize()).thenReturn(authorizerResponse);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> cancelarCobrancaUseCase.execute(1L, 1L)
        );

        assertEquals("Cancelamento não autorizado pelo autorizador externo", exception.getMessage());
        verify(authorizerClient).authorize();
        verify(cobrancaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar exceção quando usuário não é o originador")
    void deveLancarExcecaoQuandoUsuarioNaoEOriginador() {
        when(cobrancaRepository.findById(1L)).thenReturn(Optional.of(cobranca));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> cancelarCobrancaUseCase.execute(999L, 1L)
        );

        assertEquals("Apenas o originador pode cancelar esta cobrança", exception.getMessage());
        verify(cobrancaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar cancelar cobrança já cancelada")
    void deveLancarExcecaoAoTentarCancelarCobrancaJaCancelada() {
        cobranca.setStatus(CobrancaStatus.CANCELADA);

        when(cobrancaRepository.findById(1L)).thenReturn(Optional.of(cobranca));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> cancelarCobrancaUseCase.execute(1L, 1L)
        );

        assertEquals("Cobrança já está cancelada", exception.getMessage());
    }

    @Test
    @DisplayName("Deve lançar exceção quando cobrança não existe")
    void deveLancarExcecaoQuandoCobrancaNaoExiste() {
        when(cobrancaRepository.findById(999L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> cancelarCobrancaUseCase.execute(1L, 999L)
        );

        assertEquals("Cobrança não encontrada", exception.getMessage());
    }

    @Test
    @DisplayName("Deve lançar exceção quando recebedor não tem saldo para estorno")
    void deveLancarExcecaoQuandoRecebedorNaoTemSaldoParaEstorno() {
        cobranca.marcarComoPaga(TipoPagamento.SALDO, null, "PAGAMENTO_SALDO");
        contaOriginador.setSaldo(BigDecimal.valueOf(50)); // Saldo menor que o valor a estornar

        when(cobrancaRepository.findById(1L)).thenReturn(Optional.of(cobranca));
        when(contaRepository.findByUsuarioId(2L)).thenReturn(Optional.of(contaDestinatario));
        when(contaRepository.findByUsuarioId(1L)).thenReturn(Optional.of(contaOriginador));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> cancelarCobrancaUseCase.execute(1L, 1L)
        );

        assertEquals("Recebedor não possui saldo suficiente para estorno", exception.getMessage());
    }
}