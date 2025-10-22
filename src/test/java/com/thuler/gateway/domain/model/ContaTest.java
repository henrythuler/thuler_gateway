package com.thuler.gateway.domain.model;

import com.thuler.gateway.domain.valueobject.CPF;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Conta Entity Tests")
class ContaTest {

    private Conta conta;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder()
                .id(1L)
                .nome("João Silva")
                .cpf(CPF.of("12345678909"))
                .email("joao@example.com")
                .senha("senha123")
                .build();

        conta = Conta.builder()
                .id(1L)
                .usuario(usuario)
                .saldo(BigDecimal.valueOf(1000))
                .build();
    }

    @Test
    @DisplayName("Deve criar conta com saldo inicial zero")
    void deveCriarContaComSaldoInicialZero() {
        Conta novaConta = Conta.builder()
                .usuario(usuario)
                .build();

        assertEquals(BigDecimal.ZERO, novaConta.getSaldo());
    }

    @Test
    @DisplayName("Deve depositar valor positivo")
    void deveDepositarValorPositivo() {
        BigDecimal valorDeposito = BigDecimal.valueOf(500);
        BigDecimal saldoEsperado = BigDecimal.valueOf(1500);

        conta.depositar(valorDeposito);

        assertEquals(0, saldoEsperado.compareTo(conta.getSaldo()));
    }

    @Test
    @DisplayName("Deve lançar exceção ao depositar valor zero")
    void deveLancarExcecaoAoDepositarValorZero() {
        BigDecimal valorInvalido = BigDecimal.ZERO;

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> conta.depositar(valorInvalido)
        );

        assertEquals("Valor do depósito deve ser positivo", exception.getMessage());
    }

    @Test
    @DisplayName("Deve lançar exceção ao depositar valor negativo")
    void deveLancarExcecaoAoDepositarValorNegativo() {
        BigDecimal valorInvalido = BigDecimal.valueOf(-100);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> conta.depositar(valorInvalido)
        );

        assertEquals("Valor do depósito deve ser positivo", exception.getMessage());
    }

    @Test
    @DisplayName("Deve debitar valor quando saldo é suficiente")
    void deveDebitarValorQuandoSaldoSuficiente() {
        BigDecimal valorDebito = BigDecimal.valueOf(300);
        BigDecimal saldoEsperado = BigDecimal.valueOf(700);

        conta.debitar(valorDebito);

        assertEquals(0, saldoEsperado.compareTo(conta.getSaldo()));
    }

    @Test
    @DisplayName("Deve lançar exceção ao debitar valor maior que saldo")
    void deveLancarExcecaoAoDebitarValorMaiorQueSaldo() {
        BigDecimal valorDebito = BigDecimal.valueOf(1500);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> conta.debitar(valorDebito)
        );

        assertEquals("Saldo insuficiente", exception.getMessage());
    }

    @Test
    @DisplayName("Deve lançar exceção ao debitar valor zero")
    void deveLancarExcecaoAoDebitarValorZero() {
        BigDecimal valorInvalido = BigDecimal.ZERO;

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> conta.debitar(valorInvalido)
        );

        assertEquals("Valor do débito deve ser positivo", exception.getMessage());
    }

    @Test
    @DisplayName("Deve creditar valor positivo")
    void deveCreditarValorPositivo() {
        BigDecimal valorCredito = BigDecimal.valueOf(200);
        BigDecimal saldoEsperado = BigDecimal.valueOf(1200);

        conta.creditar(valorCredito);

        assertEquals(0, saldoEsperado.compareTo(conta.getSaldo()));
    }

    @Test
    @DisplayName("Deve lançar exceção ao creditar valor zero")
    void deveLancarExcecaoAoCreditarValorZero() {
        BigDecimal valorInvalido = BigDecimal.ZERO;

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> conta.creditar(valorInvalido)
        );

        assertEquals("Valor do crédito deve ser positivo", exception.getMessage());
    }

    @Test
    @DisplayName("Deve verificar se tem saldo suficiente - TRUE")
    void deveVerificarSeSaldoSuficienteTrue() {
        BigDecimal valor = BigDecimal.valueOf(500);

        boolean resultado = conta.temSaldoSuficiente(valor);

        assertTrue(resultado);
    }

    @Test
    @DisplayName("Deve verificar se tem saldo suficiente - FALSE")
    void deveVerificarSeSaldoSuficienteFalse() {
        BigDecimal valor = BigDecimal.valueOf(1500);

        boolean resultado = conta.temSaldoSuficiente(valor);

        assertFalse(resultado);
    }

    @Test
    @DisplayName("Deve verificar se tem saldo exato")
    void deveVerificarSeSaldoExato() {
        BigDecimal valor = BigDecimal.valueOf(1000);

        boolean resultado = conta.temSaldoSuficiente(valor);

        assertTrue(resultado);
    }

    @Test
    @DisplayName("Deve implementar equals baseado no ID")
    void deveImplementarEqualsBaseadoNoId() {
        Conta conta1 = Conta.builder()
                .id(1L)
                .usuario(usuario)
                .saldo(BigDecimal.valueOf(1000))
                .build();

        Conta conta2 = Conta.builder()
                .id(1L)
                .usuario(usuario)
                .saldo(BigDecimal.valueOf(5000))
                .build();

        Conta conta3 = Conta.builder()
                .id(2L)
                .usuario(usuario)
                .saldo(BigDecimal.valueOf(1000))
                .build();

        assertEquals(conta1, conta2);
        assertNotEquals(conta1, conta3);
    }
}