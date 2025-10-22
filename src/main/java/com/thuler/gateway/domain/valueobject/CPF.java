package com.thuler.gateway.domain.valueobject;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EqualsAndHashCode
public class CPF implements Serializable {

    private String numero;

    public static CPF of(String numero) {
        if (!isValido(numero)) {
            throw new IllegalArgumentException("CPF inválido: " + numero);
        }
        return new CPF(removerFormatacao(numero));
    }

    private static String removerFormatacao(String cpf) {
        return cpf.replaceAll("[^0-9]", "");
    }

    public static boolean isValido(String cpf) {
        if (cpf == null) {
            return false;
        }

        String cpfLimpo = removerFormatacao(cpf);

        if (cpfLimpo.length() != 11) {
            return false;
        }

        if (cpfLimpo.matches("(\\d)\\1{10}")) {
            return false;
        }

        try {
            int soma = 0;
            for (int i = 0; i < 9; i++) {
                soma += Character.getNumericValue(cpfLimpo.charAt(i)) * (10 - i);
            }
            int primeiroDigito = 11 - (soma % 11);
            if (primeiroDigito >= 10) {
                primeiroDigito = 0;
            }

            if (Character.getNumericValue(cpfLimpo.charAt(9)) != primeiroDigito) {
                return false;
            }

            soma = 0;
            for (int i = 0; i < 10; i++) {
                soma += Character.getNumericValue(cpfLimpo.charAt(i)) * (11 - i);
            }
            int segundoDigito = 11 - (soma % 11);
            if (segundoDigito >= 10) {
                segundoDigito = 0;
            }

            return Character.getNumericValue(cpfLimpo.charAt(10)) == segundoDigito;

        } catch (Exception e) {
            return false;
        }
    }

    public String getNumeroFormatado() {
        if (numero == null || numero.length() != 11) {
            return numero;
        }
        return String.format("%s.%s.%s-%s",
                numero.substring(0, 3),
                numero.substring(3, 6),
                numero.substring(6, 9),
                numero.substring(9, 11));
    }

    @Override
    public String toString() {
        return getNumeroFormatado();
    }
}
