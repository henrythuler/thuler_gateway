package com.thuler.gateway.domain.model;

import com.thuler.gateway.domain.enums.CobrancaStatus;
import com.thuler.gateway.domain.enums.TipoPagamento;
import com.thuler.gateway.domain.valueobject.CPF;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Cobranca Entity Tests")
class CobrancaTest {

    private Usuario originador;
    private Usuario destinatario;
    private Cobranca cobranca;

    @BeforeEach
    void setUp() {
        originador = Usuario.builder()
                .id(1L)
                .nome("João Silva")
                .cpf(CPF.of("12345678909"))
                .email("joao@example.com")
                .senha("senha123")
                .build();

        destinatario = Usuario.builder()
                .id(2L)
                .nome("Maria Santos")
                .cpf(CPF.of("98765432100"))
                .email("maria@example.com")
                .senha("senha456")
                .build();

        cobranca = Cobranca.builder()
                .id(1L)
                .originador(originador)
                .destinatario(destinatario)
                .valor(BigDecimal.valueOf(100))
                .descricao("Teste de cobrança")
                .build();
    }

    @Test
    @DisplayName("Deve criar cobrança com status PENDENTE por padrão")
    void deveCriarCobrancaComStatusPendente() {
        // Assert
        assertEquals(CobrancaStatus.PENDENTE, cobranca.getStatus());
        assertTrue(cobranca.isPendente());
        assertFalse(cobranca.isPaga());
        assertFalse(cobranca.isCancelada());
    }

    @Test
    @DisplayName("Deve marcar cobrança como paga com saldo")
    void deveMarcarCobrancaComoPagaComSaldo() {
        cobranca.marcarComoPaga(TipoPagamento.SALDO, null, "PAGAMENTO_SALDO");

        assertEquals(CobrancaStatus.PAGA, cobranca.getStatus());
        assertEquals(TipoPagamento.SALDO, cobranca.getTipoPagamento());
        assertNull(cobranca.getNumeroCartao());
        assertEquals("PAGAMENTO_SALDO", cobranca.getAutorizadorResponse());
        assertNotNull(cobranca.getPaidAt());
        assertTrue(cobranca.isPaga());
        assertTrue(cobranca.foiPagaComSaldo());
        assertFalse(cobranca.foiPagaComCartao());
    }

    @Test
    @DisplayName("Deve marcar cobrança como paga com cartão")
    void deveMarcarCobrancaComoPagaComCartao() {
        cobranca.marcarComoPaga(TipoPagamento.CARTAO_CREDITO, "1234", "APPROVED");

        assertEquals(CobrancaStatus.PAGA, cobranca.getStatus());
        assertEquals(TipoPagamento.CARTAO_CREDITO, cobranca.getTipoPagamento());
        assertEquals("1234", cobranca.getNumeroCartao());
        assertEquals("APPROVED", cobranca.getAutorizadorResponse());
        assertNotNull(cobranca.getPaidAt());
        assertTrue(cobranca.isPaga());
        assertTrue(cobranca.foiPagaComCartao());
        assertFalse(cobranca.foiPagaComSaldo());
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar pagar cobrança já paga")
    void deveLancarExcecaoAoTentarPagarCobrancaJaPaga() {
        cobranca.marcarComoPaga(TipoPagamento.SALDO, null, "PAGAMENTO_SALDO");

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> cobranca.marcarComoPaga(TipoPagamento.SALDO, null, "TENTATIVA_DUPLICADA")
        );

        assertEquals("Apenas cobranças pendentes podem ser pagas", exception.getMessage());
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar pagar cobrança cancelada")
    void deveLancarExcecaoAoTentarPagarCobrancaCancelada() {
        cobranca.cancelar(null);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> cobranca.marcarComoPaga(TipoPagamento.SALDO, null, "TENTATIVA_APOS_CANCELAMENTO")
        );

        assertEquals("Apenas cobranças pendentes podem ser pagas", exception.getMessage());
    }

    @Test
    @DisplayName("Deve cancelar cobrança pendente")
    void deveCancelarCobrancaPendente() {
        cobranca.cancelar(null);

        assertEquals(CobrancaStatus.CANCELADA, cobranca.getStatus());
        assertNotNull(cobranca.getCancelledAt());
        assertTrue(cobranca.isCancelada());
    }

    @Test
    @DisplayName("Deve cancelar cobrança paga com informações do autorizador")
    void deveCancelarCobrancaPagaComInformacoesAutorizador() {
        cobranca.marcarComoPaga(TipoPagamento.CARTAO_CREDITO, "1234", "APPROVED");

        cobranca.cancelar("CANCELLED_BY_AUTHORIZER");

        assertEquals(CobrancaStatus.CANCELADA, cobranca.getStatus());
        assertEquals("CANCELLED_BY_AUTHORIZER", cobranca.getAutorizadorResponse());
        assertNotNull(cobranca.getCancelledAt());
        assertTrue(cobranca.isCancelada());
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar cancelar cobrança já cancelada")
    void deveLancarExcecaoAoTentarCancelarCobrancaJaCancelada() {
        cobranca.cancelar(null);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> cobranca.cancelar(null)
        );

        assertEquals("Cobrança já está cancelada", exception.getMessage());
    }

    @Test
    @DisplayName("Deve implementar equals baseado no ID")
    void deveImplementarEqualsBaseadoNoId() {
        Cobranca cobranca1 = Cobranca.builder()
                .id(1L)
                .originador(originador)
                .destinatario(destinatario)
                .valor(BigDecimal.valueOf(100))
                .build();

        Cobranca cobranca2 = Cobranca.builder()
                .id(1L)
                .originador(destinatario)
                .destinatario(originador)
                .valor(BigDecimal.valueOf(500))
                .build();

        Cobranca cobranca3 = Cobranca.builder()
                .id(2L)
                .originador(originador)
                .destinatario(destinatario)
                .valor(BigDecimal.valueOf(100))
                .build();

        assertEquals(cobranca1, cobranca2);
        assertNotEquals(cobranca1, cobranca3);
    }
}