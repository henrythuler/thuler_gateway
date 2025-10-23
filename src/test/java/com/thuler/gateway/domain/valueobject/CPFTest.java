package com.thuler.gateway.domain.valueobject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CPF Value Object Tests")
class CPFTest {

    @Test
    @DisplayName("Deve criar CPF válido sem formatação")
    void deveCriarCpfValidoSemFormatacao() {
        String cpfNumero = "12345678909";

        CPF cpf = CPF.of(cpfNumero);

        assertNotNull(cpf);
        assertEquals("12345678909", cpf.getNumero());
    }

    @Test
    @DisplayName("Deve criar CPF válido com formatação")
    void deveCriarCpfValidoComFormatacao() {
        String cpfFormatado = "123.456.789-09";

        CPF cpf = CPF.of(cpfFormatado);

        assertNotNull(cpf);
        assertEquals("12345678909", cpf.getNumero());
    }

    @Test
    @DisplayName("Deve formatar CPF corretamente")
    void deveFormatarCpfCorretamente() {
        String cpfNumero = "12345678909";
        CPF cpf = CPF.of(cpfNumero);

        String formatado = cpf.getNumeroFormatado();

        assertEquals("123.456.789-09", formatado);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "00000000000",
            "11111111111",
            "22222222222",
            "33333333333",
            "44444444444",
            "55555555555",
            "66666666666",
            "77777777777",
            "88888888888",
            "99999999999"
    })
    @DisplayName("Deve rejeitar CPF com todos dígitos iguais")
    void deveRejeitarCpfComTodosDigitosIguais(String cpfInvalido) {
        assertThrows(IllegalArgumentException.class, () -> CPF.of(cpfInvalido));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "123",
            "12345678",
            "123456789012",
            "",
            "abcdefghijk"
    })
    @DisplayName("Deve rejeitar CPF com tamanho inválido")
    void deveRejeitarCpfComTamanhoInvalido(String cpfInvalido) {
        assertThrows(IllegalArgumentException.class, () -> CPF.of(cpfInvalido));
    }

    @Test
    @DisplayName("Deve rejeitar CPF nulo")
    void deveRejeitarCpfNulo() {
        assertThrows(IllegalArgumentException.class, () -> CPF.of(null));
    }
}