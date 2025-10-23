package com.thuler.gateway.controller;

import com.thuler.gateway.dto.request.PagarCobrancaCartaoRequest;
import com.thuler.gateway.dto.request.PagarCobrancaSaldoRequest;
import com.thuler.gateway.domain.model.Cobranca;
import com.thuler.gateway.domain.model.Conta;
import com.thuler.gateway.domain.model.Usuario;
import com.thuler.gateway.domain.enums.CobrancaStatus;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("CobrancaController - Pagamento Integration Tests")
class CobrancaControllerPagamentoTest extends BaseIntegrationTest {

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
    private String tokenDestinatario;
    private Cobranca cobranca;

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

        contaDestinatario.depositar(BigDecimal.valueOf(1000));
        contaRepository.save(contaDestinatario);

        tokenDestinatario = jwtTokenProvider.generateToken(destinatario.getId(), destinatario.getEmail());

        cobranca = Cobranca.builder()
                .originador(originador)
                .destinatario(destinatario)
                .valor(BigDecimal.valueOf(100))
                .descricao("Cobrança teste")
                .status(CobrancaStatus.PENDENTE)
                .build();
        cobranca = cobrancaRepository.save(cobranca);
    }

    @Test
    @DisplayName("POST /api/cobrancas/pagar/saldo - Deve pagar com saldo com sucesso")
    void devePagarComSaldoComSucesso() throws Exception {
        PagarCobrancaSaldoRequest request = PagarCobrancaSaldoRequest.builder()
                .cobrancaId(cobranca.getId())
                .build();

        mockMvc.perform(post("/api/cobrancas/pagar/saldo")
                        .header("Authorization", "Bearer " + tokenDestinatario)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cobranca.getId()))
                .andExpect(jsonPath("$.status").value("PAGA"))
                .andExpect(jsonPath("$.tipoPagamento").value("SALDO"))
                .andExpect(jsonPath("$.paidAt").exists());

        Conta contaDestAtualizada = contaRepository.findById(contaDestinatario.getId()).get();
        Conta contaOrigAtualizada = contaRepository.findById(contaOriginador.getId()).get();

        assert contaDestAtualizada.getSaldo().compareTo(BigDecimal.valueOf(900)) == 0; // 1000 - 100
        assert contaOrigAtualizada.getSaldo().compareTo(BigDecimal.valueOf(100)) == 0; // 0 + 100
    }

    @Test
    @DisplayName("POST /api/cobrancas/pagar/saldo - Deve retornar 400 com saldo insuficiente")
    void deveRetornar400ComSaldoInsuficiente() throws Exception {
        contaDestinatario.setSaldo(BigDecimal.valueOf(50));
        contaRepository.save(contaDestinatario);

        PagarCobrancaSaldoRequest request = PagarCobrancaSaldoRequest.builder()
                .cobrancaId(cobranca.getId())
                .build();

        mockMvc.perform(post("/api/cobrancas/pagar/saldo")
                        .header("Authorization", "Bearer " + tokenDestinatario)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Saldo insuficiente"));
    }

    @Test
    @DisplayName("POST /api/cobrancas/pagar/cartao - Deve pagar com cartão com sucesso")
    void devePagarComCartaoComSucesso() throws Exception {
        PagarCobrancaCartaoRequest request = PagarCobrancaCartaoRequest.builder()
                .cobrancaId(cobranca.getId())
                .numeroCartao("1234567890123456")
                .dataExpiracao("12/25")
                .cvv("123")
                .build();

        AuthorizerResponse authorizerResponse = new AuthorizerResponse();
        authorizerResponse.setStatus("APPROVED");
        authorizerResponse.setData(new AuthorizerResponse.AuthorizerData(true));

        when(authorizerClient.authorize()).thenReturn(authorizerResponse);

        mockMvc.perform(post("/api/cobrancas/pagar/cartao")
                        .header("Authorization", "Bearer " + tokenDestinatario)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cobranca.getId()))
                .andExpect(jsonPath("$.status").value("PAGA"))
                .andExpect(jsonPath("$.tipoPagamento").value("CARTAO_CREDITO"))
                .andExpect(jsonPath("$.numeroCartao").value("3456"))
                .andExpect(jsonPath("$.paidAt").exists());
    }

    @Test
    @DisplayName("POST /api/cobrancas/pagar/cartao - Deve retornar 400 quando autorizador nega")
    void deveRetornar400QuandoAutorizadorNegaPagamentoCartao() throws Exception {
        PagarCobrancaCartaoRequest request = PagarCobrancaCartaoRequest.builder()
                .cobrancaId(cobranca.getId())
                .numeroCartao("1234567890123456")
                .dataExpiracao("12/25")
                .cvv("123")
                .build();

        AuthorizerResponse authorizerResponse = new AuthorizerResponse();
        authorizerResponse.setStatus("DENIED");
        authorizerResponse.setData(new AuthorizerResponse.AuthorizerData(false));

        when(authorizerClient.authorize()).thenReturn(authorizerResponse);

        mockMvc.perform(post("/api/cobrancas/pagar/cartao")
                        .header("Authorization", "Bearer " + tokenDestinatario)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Pagamento não autorizado pelo autorizador externo"));
    }

    @Test
    @DisplayName("POST /api/cobrancas/pagar/cartao - Deve retornar 400 com número de cartão inválido")
    void deveRetornar400ComNumeroCartaoInvalido() throws Exception {
        PagarCobrancaCartaoRequest request = PagarCobrancaCartaoRequest.builder()
                .cobrancaId(cobranca.getId())
                .numeroCartao("123456")
                .dataExpiracao("12/25")
                .cvv("123")
                .build();

        mockMvc.perform(post("/api/cobrancas/pagar/cartao")
                        .header("Authorization", "Bearer " + tokenDestinatario)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0].field").value("numeroCartao"));
    }

    @Test
    @DisplayName("POST /api/cobrancas/pagar/cartao - Deve retornar 400 com data de expiração inválida")
    void deveRetornar400ComDataExpiracaoInvalida() throws Exception {
        PagarCobrancaCartaoRequest request = PagarCobrancaCartaoRequest.builder()
                .cobrancaId(cobranca.getId())
                .numeroCartao("1234567890123456")
                .dataExpiracao("13/25") // Mês inválido
                .cvv("123")
                .build();

        mockMvc.perform(post("/api/cobrancas/pagar/cartao")
                        .header("Authorization", "Bearer " + tokenDestinatario)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0].field").value("dataExpiracao"));
    }
}
