package com.thuler.gateway.controller;

import com.thuler.gateway.dto.request.DepositoRequest;
import com.thuler.gateway.domain.model.Conta;
import com.thuler.gateway.domain.model.Usuario;
import com.thuler.gateway.domain.valueobject.CPF;
import com.thuler.gateway.infrastructure.external.authorizer.AuthorizerClient;
import com.thuler.gateway.infrastructure.external.authorizer.AuthorizerResponse;
import com.thuler.gateway.infrastructure.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("ContaController Integration Tests")
class ContaControllerTest extends BaseIntegrationTest {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private AuthorizerClient authorizerClient;

    private Usuario usuario;
    private Conta conta;
    private String token;

    @BeforeEach
    void setupUsuario() {
        usuario = Usuario.builder()
                .nome("João Silva")
                .cpf(CPF.of("52998224725"))
                .email("joao@example.com")
                .senha(passwordEncoder.encode("senha123"))
                .active(true)
                .build();
        usuario.criarConta();
        usuario = usuarioRepository.save(usuario);

        conta = usuario.getConta();
        conta.depositar(BigDecimal.valueOf(500));
        contaRepository.save(conta);

        token = jwtTokenProvider.generateToken(usuario.getId(), usuario.getEmail());
    }

    @Test
    @DisplayName("GET /api/conta/saldo - Deve consultar saldo com sucesso")
    void deveConsultarSaldoComSucesso() throws Exception {
        mockMvc.perform(get("/api/conta/saldo")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.usuarioId").value(usuario.getId()))
                .andExpect(jsonPath("$.saldo").value(500))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    @DisplayName("GET /api/conta/saldo - Deve retornar 401 sem autenticação")
    void deveRetornar401SemAutenticacao() throws Exception {
        mockMvc.perform(get("/api/conta/saldo"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("POST /api/conta/deposito - Deve depositar com sucesso quando autorizado")
    void deveDepositarComSucessoQuandoAutorizado() throws Exception {
        DepositoRequest request = DepositoRequest.builder()
                .valor(BigDecimal.valueOf(100))
                .build();

        AuthorizerResponse authorizerResponse = new AuthorizerResponse();
        authorizerResponse.setStatus("APPROVED");
        authorizerResponse.setData(new AuthorizerResponse.AuthorizerData(true));

        when(authorizerClient.authorize()).thenReturn(authorizerResponse);

        mockMvc.perform(post("/api/conta/deposito")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saldo").value(600)) // 500 + 100
                .andExpect(jsonPath("$.usuarioId").value(usuario.getId()));
    }

    @Test
    @DisplayName("POST /api/conta/deposito - Deve retornar 400 quando autorizador nega")
    void deveRetornar400QuandoAutorizadorNega() throws Exception {
        DepositoRequest request = DepositoRequest.builder()
                .valor(BigDecimal.valueOf(100))
                .build();

        AuthorizerResponse authorizerResponse = new AuthorizerResponse();
        authorizerResponse.setStatus("DENIED");
        authorizerResponse.setData(new AuthorizerResponse.AuthorizerData(false));

        when(authorizerClient.authorize()).thenReturn(authorizerResponse);

        mockMvc.perform(post("/api/conta/deposito")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Depósito não autorizado pelo autorizador externo"));
    }

    @Test
    @DisplayName("POST /api/conta/deposito - Deve retornar 400 com valor zero")
    void deveRetornar400ComValorZero() throws Exception {
        DepositoRequest request = DepositoRequest.builder()
                .valor(BigDecimal.ZERO)
                .build();

        mockMvc.perform(post("/api/conta/deposito")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0].field").value("valor"))
                .andExpect(jsonPath("$.errors[0].message").value("Valor deve ser maior que zero"));
    }

    @Test
    @DisplayName("POST /api/conta/deposito - Deve retornar 400 com valor negativo")
    void deveRetornar400ComValorNegativo() throws Exception {
        DepositoRequest request = DepositoRequest.builder()
                .valor(BigDecimal.valueOf(-50))
                .build();

        mockMvc.perform(post("/api/conta/deposito")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0].field").value("valor"))
                .andExpect(jsonPath("$.errors[0].message").value("Valor deve ser maior que zero"));
    }

    @Test
    @DisplayName("POST /api/conta/deposito - Deve retornar 400 com valor nulo")
    void deveRetornar400ComValorNulo() throws Exception {
        DepositoRequest request = DepositoRequest.builder()
                .valor(null)
                .build();

        mockMvc.perform(post("/api/conta/deposito")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0].field").value("valor"))
                .andExpect(jsonPath("$.errors[0].message").value("Valor é obrigatório"));
    }

    @Test
    @DisplayName("POST /api/conta/deposito - Deve retornar 401 sem autenticação")
    void deveRetornar401SemAutenticacaoNoDeposito() throws Exception {
        DepositoRequest request = DepositoRequest.builder()
                .valor(BigDecimal.valueOf(100))
                .build();

        mockMvc.perform(post("/api/conta/deposito")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("POST /api/conta/deposito - Deve retornar 401 com token inválido")
    void deveRetornar401ComTokenInvalido() throws Exception {
        DepositoRequest request = DepositoRequest.builder()
                .valor(BigDecimal.valueOf(100))
                .build();

        mockMvc.perform(post("/api/conta/deposito")
                        .header("Authorization", "Bearer token_invalido")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}