package com.thuler.gateway.controller;

import com.thuler.gateway.dto.request.CadastroUsuarioRequest;
import com.thuler.gateway.dto.request.LoginRequest;
import com.thuler.gateway.domain.model.Usuario;
import com.thuler.gateway.domain.valueobject.CPF;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("UsuarioController Integration Tests")
class UsuarioControllerTest extends BaseIntegrationTest {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("POST /api/usuarios/cadastro - Deve cadastrar usuário com sucesso")
    void deveCadastrarUsuarioComSucesso() throws Exception {
        CadastroUsuarioRequest request = CadastroUsuarioRequest.builder()
                .nome("João Silva")
                .cpf("52998224725")
                .email("joao@example.com")
                .senha("senha123")
                .build();

        mockMvc.perform(post("/api/usuarios/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.nome").value("João Silva"))
                .andExpect(jsonPath("$.cpf").value("529.982.247-25"))
                .andExpect(jsonPath("$.email").value("joao@example.com"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    @DisplayName("POST /api/usuarios/cadastro - Deve retornar 400 com CPF inválido")
    void deveRetornar400ComCpfInvalido() throws Exception {
        CadastroUsuarioRequest request = CadastroUsuarioRequest.builder()
                .nome("João Silva")
                .cpf("12345678900") // CPF inválido
                .email("joao@example.com")
                .senha("senha123")
                .build();

        mockMvc.perform(post("/api/usuarios/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("CPF inválido: 12345678900"));
    }

    @Test
    @DisplayName("POST /api/usuarios/cadastro - Deve retornar 400 com dados faltando")
    void deveRetornar400ComDadosFaltando() throws Exception {
        CadastroUsuarioRequest request = CadastroUsuarioRequest.builder()
                .nome("João Silva")
                .email("joao@example.com")
                .senha("senha123")
                .build();

        mockMvc.perform(post("/api/usuarios/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[*].field", hasItem("cpf")));
    }

    @Test
    @DisplayName("POST /api/usuarios/cadastro - Deve retornar 400 com CPF duplicado")
    void deveRetornar400ComCpfDuplicado() throws Exception {
        Usuario usuarioExistente = Usuario.builder()
                .nome("Maria Santos")
                .cpf(CPF.of("52998224725"))
                .email("maria@example.com")
                .senha(passwordEncoder.encode("senha123"))
                .active(true)
                .build();
        usuarioExistente.criarConta();
        usuarioRepository.save(usuarioExistente);

        CadastroUsuarioRequest request = CadastroUsuarioRequest.builder()
                .nome("João Silva")
                .cpf("52998224725")
                .email("joao@example.com")
                .senha("senha123")
                .build();

        mockMvc.perform(post("/api/usuarios/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("CPF já cadastrado"));
    }

    @Test
    @DisplayName("POST /api/usuarios/cadastro - Deve retornar 400 com email duplicado")
    void deveRetornar400ComEmailDuplicado() throws Exception {
        Usuario usuarioExistente = Usuario.builder()
                .nome("Maria Santos")
                .cpf(CPF.of("52998224725"))
                .email("joao@example.com") // Email que será duplicado
                .senha(passwordEncoder.encode("senha123"))
                .active(true)
                .build();
        usuarioExistente.criarConta();
        usuarioRepository.save(usuarioExistente);

        CadastroUsuarioRequest request = CadastroUsuarioRequest.builder()
                .nome("João Silva")
                .cpf("12345678909")
                .email("joao@example.com")
                .senha("senha123")
                .build();

        mockMvc.perform(post("/api/usuarios/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email já cadastrado"));
    }

    @Test
    @DisplayName("POST /api/usuarios/login - Deve fazer login com email com sucesso")
    void deveFazerLoginComEmailComSucesso() throws Exception {
        Usuario usuario = Usuario.builder()
                .nome("João Silva")
                .cpf(CPF.of("52998224725"))
                .email("joao@example.com")
                .senha(passwordEncoder.encode("senha123"))
                .active(true)
                .build();
        usuario.criarConta();
        usuarioRepository.save(usuario);

        LoginRequest request = LoginRequest.builder()
                .identificador("joao@example.com")
                .senha("senha123")
                .build();

        mockMvc.perform(post("/api/usuarios/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.usuario.nome").value("João Silva"))
                .andExpect(jsonPath("$.usuario.email").value("joao@example.com"));
    }

    @Test
    @DisplayName("POST /api/usuarios/login - Deve fazer login com CPF com sucesso")
    void deveFazerLoginComCpfComSucesso() throws Exception {
        Usuario usuario = Usuario.builder()
                .nome("João Silva")
                .cpf(CPF.of("52998224725"))
                .email("joao@example.com")
                .senha(passwordEncoder.encode("senha123"))
                .active(true)
                .build();
        usuario.criarConta();
        usuarioRepository.save(usuario);

        LoginRequest request = LoginRequest.builder()
                .identificador("52998224725")
                .senha("senha123")
                .build();

        mockMvc.perform(post("/api/usuarios/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.type").value("Bearer"));
    }

    @Test
    @DisplayName("POST /api/usuarios/login - Deve retornar 400 com credenciais inválidas")
    void deveRetornar400ComCredenciaisInvalidas() throws Exception {
        Usuario usuario = Usuario.builder()
                .nome("João Silva")
                .cpf(CPF.of("52998224725"))
                .email("joao@example.com")
                .senha(passwordEncoder.encode("senha123"))
                .active(true)
                .build();
        usuario.criarConta();
        usuarioRepository.save(usuario);

        LoginRequest request = LoginRequest.builder()
                .identificador("joao@example.com")
                .senha("senha_errada")
                .build();

        mockMvc.perform(post("/api/usuarios/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Credenciais inválidas"));
    }

    @Test
    @DisplayName("POST /api/usuarios/login - Deve retornar 400 com usuário inativo")
    void deveRetornar400ComUsuarioInativo() throws Exception {
        Usuario usuario = Usuario.builder()
                .nome("João Silva")
                .cpf(CPF.of("52998224725"))
                .email("joao@example.com")
                .senha(passwordEncoder.encode("senha123"))
                .active(false)
                .build();
        usuario.criarConta();
        usuarioRepository.save(usuario);

        LoginRequest request = LoginRequest.builder()
                .identificador("joao@example.com")
                .senha("senha123")
                .build();

        mockMvc.perform(post("/api/usuarios/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Usuário inativo"));
    }
}