package com.thuler.gateway.domain.model;

import com.thuler.gateway.domain.valueobject.CPF;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Usuario Entity Tests")
class UsuarioTest {

    @Test
    @DisplayName("Deve criar usuário com builder")
    void deveCriarUsuarioComBuilder() {
        Usuario usuario = Usuario.builder()
                .nome("João Silva")
                .cpf(CPF.of("12345678909"))
                .email("joao@example.com")
                .senha("senha123")
                .build();

        assertNotNull(usuario);
        assertEquals("João Silva", usuario.getNome());
        assertEquals("12345678909", usuario.getCpf().getNumero());
        assertEquals("joao@example.com", usuario.getEmail());
        assertEquals("senha123", usuario.getSenha());
        assertTrue(usuario.getActive()); // Default true
    }

    @Test
    @DisplayName("Deve criar conta automaticamente ao chamar criarConta()")
    void deveCriarContaAutomaticamente() {
        Usuario usuario = Usuario.builder()
                .nome("João Silva")
                .cpf(CPF.of("12345678909"))
                .email("joao@example.com")
                .senha("senha123")
                .build();

        usuario.criarConta();

        assertNotNull(usuario.getConta());
        assertEquals(usuario, usuario.getConta().getUsuario());
    }

    @Test
    @DisplayName("Não deve criar conta duplicada")
    void naoDeveCriarContaDuplicada() {
        Usuario usuario = Usuario.builder()
                .nome("João Silva")
                .cpf(CPF.of("12345678909"))
                .email("joao@example.com")
                .senha("senha123")
                .build();

        usuario.criarConta();
        Conta contaOriginal = usuario.getConta();

        usuario.criarConta();

        assertSame(contaOriginal, usuario.getConta());
    }

    @Test
    @DisplayName("Deve implementar equals baseado no ID")
    void deveImplementarEqualsBaseadoNoId() {
        Usuario usuario1 = Usuario.builder()
                .id(1L)
                .nome("João Silva")
                .cpf(CPF.of("12345678909"))
                .email("joao@example.com")
                .senha("senha123")
                .build();

        Usuario usuario2 = Usuario.builder()
                .id(1L)
                .nome("Maria Santos")
                .cpf(CPF.of("98765432100"))
                .email("maria@example.com")
                .senha("senha456")
                .build();

        Usuario usuario3 = Usuario.builder()
                .id(2L)
                .nome("João Silva")
                .cpf(CPF.of("12345678909"))
                .email("joao@example.com")
                .senha("senha123")
                .build();

        assertEquals(usuario1, usuario2);
        assertNotEquals(usuario1, usuario3);
    }

    @Test
    @DisplayName("Deve permitir desativar usuário")
    void devePermitirDesativarUsuario() {
        Usuario usuario = Usuario.builder()
                .nome("João Silva")
                .cpf(CPF.of("12345678909"))
                .email("joao@example.com")
                .senha("senha123")
                .build();

        usuario.setActive(false);
        
        assertFalse(usuario.getActive());
    }
}