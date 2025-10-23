package com.thuler.gateway.controller;

import com.thuler.gateway.dto.request.CriarCobrancaRequest;
import com.thuler.gateway.domain.model.Cobranca;
import com.thuler.gateway.domain.model.Usuario;
import com.thuler.gateway.domain.enums.CobrancaStatus;
import com.thuler.gateway.domain.valueobject.CPF;
import com.thuler.gateway.infrastructure.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("CobrancaController Integration Tests")
class CobrancaControllerTest extends BaseIntegrationTest {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private Usuario originador;
    private Usuario destinatario;
    private String tokenOriginador;
    private String tokenDestinatario;

    @BeforeEach
    void setupUsuarios() {
        originador = Usuario.builder()
                .nome("João Silva")
                .cpf(CPF.of("52998224725"))
                .email("joao@example.com")
                .senha(passwordEncoder.encode("senha123"))
                .active(true)
                .build();
        originador.criarConta();
        originador = usuarioRepository.save(originador);

        destinatario = Usuario.builder()
                .nome("Maria Santos")
                .cpf(CPF.of("12345678909"))
                .email("maria@example.com")
                .senha(passwordEncoder.encode("senha123"))
                .active(true)
                .build();
        destinatario.criarConta();
        destinatario = usuarioRepository.save(destinatario);

        tokenOriginador = jwtTokenProvider.generateToken(originador.getId(), originador.getEmail());
        tokenDestinatario = jwtTokenProvider.generateToken(destinatario.getId(), destinatario.getEmail());
    }

    @Test
    @DisplayName("POST /api/cobrancas - Deve criar cobrança com sucesso")
    void deveCriarCobrancaComSucesso() throws Exception {
        CriarCobrancaRequest request = CriarCobrancaRequest.builder()
                .cpfDestinatario("12345678909")
                .valor(BigDecimal.valueOf(100))
                .descricao("Cobrança de teste")
                .build();

        mockMvc.perform(post("/api/cobrancas")
                        .header("Authorization", "Bearer " + tokenOriginador)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.originadorNome").value("João Silva"))
                .andExpect(jsonPath("$.destinatarioNome").value("Maria Santos"))
                .andExpect(jsonPath("$.valor").value(100))
                .andExpect(jsonPath("$.descricao").value("Cobrança de teste"))
                .andExpect(jsonPath("$.status").value("PENDENTE"));
    }

    @Test
    @DisplayName("POST /api/cobrancas - Deve retornar 401 sem autenticação")
    void deveRetornar401SemAutenticacao() throws Exception {
        CriarCobrancaRequest request = CriarCobrancaRequest.builder()
                .cpfDestinatario("12345678909")
                .valor(BigDecimal.valueOf(100))
                .descricao("Cobrança de teste")
                .build();

        mockMvc.perform(post("/api/cobrancas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Autenticação necessária para acessar esse recurso"));
    }

    @Test
    @DisplayName("POST /api/cobrancas - Deve retornar 400 com CPF destinatário inexistente")
    void deveRetornar400ComCpfDestinatarioInexistente() throws Exception {
        CriarCobrancaRequest request = CriarCobrancaRequest.builder()
                .cpfDestinatario("98765432100")
                .valor(BigDecimal.valueOf(100))
                .descricao("Cobrança de teste")
                .build();

        mockMvc.perform(post("/api/cobrancas")
                        .header("Authorization", "Bearer " + tokenOriginador)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Destinatário não encontrado"));
    }

    @Test
    @DisplayName("GET /api/cobrancas/enviadas - Deve listar cobranças enviadas")
    void deveListarCobrancasEnviadas() throws Exception {
        Cobranca cobranca1 = Cobranca.builder()
                .originador(originador)
                .destinatario(destinatario)
                .valor(BigDecimal.valueOf(100))
                .descricao("Cobrança 1")
                .status(CobrancaStatus.PENDENTE)
                .build();

        Cobranca cobranca2 = Cobranca.builder()
                .originador(originador)
                .destinatario(destinatario)
                .valor(BigDecimal.valueOf(200))
                .descricao("Cobrança 2")
                .status(CobrancaStatus.PAGA)
                .build();

        cobrancaRepository.save(cobranca1);
        cobrancaRepository.save(cobranca2);

        mockMvc.perform(get("/api/cobrancas/enviadas")
                        .header("Authorization", "Bearer " + tokenOriginador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].originadorNome").value("João Silva"))
                .andExpect(jsonPath("$[1].originadorNome").value("João Silva"));
    }

    @Test
    @DisplayName("GET /api/cobrancas/enviadas?status=PENDENTE - Deve filtrar por status")
    void deveListarCobrancasEnviadasComFiltroStatus() throws Exception {
        Cobranca cobranca1 = Cobranca.builder()
                .originador(originador)
                .destinatario(destinatario)
                .valor(BigDecimal.valueOf(100))
                .status(CobrancaStatus.PENDENTE)
                .build();

        Cobranca cobranca2 = Cobranca.builder()
                .originador(originador)
                .destinatario(destinatario)
                .valor(BigDecimal.valueOf(200))
                .status(CobrancaStatus.PAGA)
                .build();

        cobrancaRepository.save(cobranca1);
        cobrancaRepository.save(cobranca2);

        // Act & Assert
        mockMvc.perform(get("/api/cobrancas/enviadas?status=PENDENTE")
                        .header("Authorization", "Bearer " + tokenOriginador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status").value("PENDENTE"));
    }

    @Test
    @DisplayName("GET /api/cobrancas/recebidas - Deve listar cobranças recebidas")
    void deveListarCobrancasRecebidas() throws Exception {
        Cobranca cobranca = Cobranca.builder()
                .originador(originador)
                .destinatario(destinatario)
                .valor(BigDecimal.valueOf(100))
                .status(CobrancaStatus.PENDENTE)
                .build();

        cobrancaRepository.save(cobranca);

        mockMvc.perform(get("/api/cobrancas/recebidas")
                        .header("Authorization", "Bearer " + tokenDestinatario))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].destinatarioNome").value("Maria Santos"));
    }
}