package com.thuler.gateway.controller;

import com.thuler.gateway.domain.model.Cobranca;
import com.thuler.gateway.domain.model.Conta;
import com.thuler.gateway.domain.model.Usuario;
import com.thuler.gateway.domain.enums.CobrancaStatus;
import com.thuler.gateway.domain.enums.TipoPagamento;
import com.thuler.gateway.domain.valueobject.CPF;
import com.thuler.gateway.infrastructure.external.authorizer.AuthorizerClient;
import com.thuler.gateway.infrastructure.external.authorizer.AuthorizerResponse;
import com.thuler.gateway.infrastructure.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("CobrancaController - Cancelamento Integration Tests")
class CobrancaControllerCancelamentoTest extends BaseIntegrationTest {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private AuthorizerClient authorizerClient;

    private Usuario originador;
    private Usuario destinatario;
    private Conta contaOriginador;
    private Conta contaDestinatario;
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
        contaOriginador = originador.getConta();
        contaOriginador.depositar(BigDecimal.valueOf(500));
        contaRepository.save(contaOriginador);

        destinatario = Usuario.builder()
                .nome("Maria Santos")
                .cpf(CPF.of("12345678909"))
                .email("maria@example.com")
                .senha(passwordEncoder.encode("senha123"))
                .active(true)
                .build();
        destinatario.criarConta();
        destinatario = usuarioRepository.save(destinatario);
        contaDestinatario = destinatario.getConta();

        tokenOriginador = jwtTokenProvider.generateToken(originador.getId(), originador.getEmail());
        tokenDestinatario = jwtTokenProvider.generateToken(destinatario.getId(), destinatario.getEmail());
    }

    @Test
    @DisplayName("DELETE /api/cobrancas/{id} - Deve cancelar cobrança pendente com sucesso")
    void deveCancelarCobrancaPendenteComSucesso() throws Exception {
        Cobranca cobranca = Cobranca.builder()
                .originador(originador)
                .destinatario(destinatario)
                .valor(BigDecimal.valueOf(100))
                .status(CobrancaStatus.PENDENTE)
                .build();
        cobranca = cobrancaRepository.save(cobranca);

        mockMvc.perform(delete("/api/cobrancas/" + cobranca.getId())
                        .header("Authorization", "Bearer " + tokenOriginador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cobranca.getId()))
                .andExpect(jsonPath("$.status").value("CANCELADA"))
                .andExpect(jsonPath("$.cancelledAt").exists());
    }

    @Test
    @DisplayName("DELETE /api/cobrancas/{id} - Deve cancelar e estornar cobrança paga com saldo")
    void deveCancelarEEstornarCobrancaPagaComSaldo() throws Exception {
        Cobranca cobranca = Cobranca.builder()
                .originador(originador)
                .destinatario(destinatario)
                .valor(BigDecimal.valueOf(100))
                .status(CobrancaStatus.PAGA)
                .tipoPagamento(TipoPagamento.SALDO)
                .build();
        cobranca = cobrancaRepository.save(cobranca);

        mockMvc.perform(delete("/api/cobrancas/" + cobranca.getId())
                        .header("Authorization", "Bearer " + tokenOriginador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADA"));

        Conta contaOrigAtualizada = contaRepository.findById(contaOriginador.getId()).get();
        assert contaOrigAtualizada.getSaldo().compareTo(BigDecimal.valueOf(400)) == 0; // 500 - 100
    }

    @Test
    @DisplayName("DELETE /api/cobrancas/{id} - Deve cancelar cobrança paga com cartão quando autorizado")
    void deveCancelarCobrancaPagaComCartaoQuandoAutorizado() throws Exception {
        Cobranca cobranca = Cobranca.builder()
                .originador(originador)
                .destinatario(destinatario)
                .valor(BigDecimal.valueOf(100))
                .status(CobrancaStatus.PAGA)
                .tipoPagamento(TipoPagamento.CARTAO_CREDITO)
                .numeroCartao("1234")
                .build();
        cobranca = cobrancaRepository.save(cobranca);

        AuthorizerResponse authorizerResponse = new AuthorizerResponse();
        authorizerResponse.setStatus("APPROVED");
        authorizerResponse.setData(new AuthorizerResponse.AuthorizerData(true));

        when(authorizerClient.authorize()).thenReturn(authorizerResponse);

        mockMvc.perform(delete("/api/cobrancas/" + cobranca.getId())
                        .header("Authorization", "Bearer " + tokenOriginador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADA"));
    }

    @Test
    @DisplayName("DELETE /api/cobrancas/{id} - Deve retornar 400 quando autorizador nega cancelamento")
    void deveRetornar400QuandoAutorizadorNegaCancelamento() throws Exception {
        Cobranca cobranca = Cobranca.builder()
                .originador(originador)
                .destinatario(destinatario)
                .valor(BigDecimal.valueOf(100))
                .status(CobrancaStatus.PAGA)
                .tipoPagamento(TipoPagamento.CARTAO_CREDITO)
                .numeroCartao("1234")
                .build();
        cobranca = cobrancaRepository.save(cobranca);

        AuthorizerResponse authorizerResponse = new AuthorizerResponse();
        authorizerResponse.setStatus("DENIED");
        authorizerResponse.setData(new AuthorizerResponse.AuthorizerData(false));

        when(authorizerClient.authorize()).thenReturn(authorizerResponse);

        mockMvc.perform(delete("/api/cobrancas/" + cobranca.getId())
                        .header("Authorization", "Bearer " + tokenOriginador))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cancelamento não autorizado pelo autorizador externo"));
    }

    @Test
    @DisplayName("DELETE /api/cobrancas/{id} - Deve retornar 403 quando usuário não é o originador")
    void deveRetornar403QuandoUsuarioNaoEOriginador() throws Exception {
        Cobranca cobranca = Cobranca.builder()
                .originador(originador)
                .destinatario(destinatario)
                .valor(BigDecimal.valueOf(100))
                .status(CobrancaStatus.PENDENTE)
                .build();
        cobranca = cobrancaRepository.save(cobranca);

        mockMvc.perform(delete("/api/cobrancas/" + cobranca.getId())
                        .header("Authorization", "Bearer " + tokenDestinatario))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Apenas o originador pode cancelar esta cobrança"));
    }

    @Test
    @DisplayName("DELETE /api/cobrancas/{id} - Deve retornar 404 quando cobrança não existe")
    void deveRetornar404QuandoCobrancaNaoExiste() throws Exception {
        mockMvc.perform(delete("/api/cobrancas/999")
                        .header("Authorization", "Bearer " + tokenOriginador))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cobrança não encontrada"));
    }

    @Test
    @DisplayName("DELETE /api/cobrancas/{id} - Deve retornar 409 ao tentar cancelar cobrança já cancelada")
    void deveRetornar409AoTentarCancelarCobrancaJaCancelada() throws Exception {
        Cobranca cobranca = Cobranca.builder()
                .originador(originador)
                .destinatario(destinatario)
                .valor(BigDecimal.valueOf(100))
                .status(CobrancaStatus.CANCELADA)
                .build();
        cobranca = cobrancaRepository.save(cobranca);

        mockMvc.perform(delete("/api/cobrancas/" + cobranca.getId())
                        .header("Authorization", "Bearer " + tokenOriginador))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Cobrança já está cancelada"));
    }

    @Test
    @DisplayName("DELETE /api/cobrancas/{id} - Deve retornar 401 sem autenticação")
    void deveRetornar401SemAutenticacao() throws Exception {
        Cobranca cobranca = Cobranca.builder()
                .originador(originador)
                .destinatario(destinatario)
                .valor(BigDecimal.valueOf(100))
                .status(CobrancaStatus.PENDENTE)
                .build();
        cobranca = cobrancaRepository.save(cobranca);

        mockMvc.perform(delete("/api/cobrancas/" + cobranca.getId()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }
}
