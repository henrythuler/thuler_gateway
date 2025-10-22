package com.thuler.gateway.controller;

import com.thuler.gateway.dto.request.CriarCobrancaRequest;
import com.thuler.gateway.dto.request.PagarCobrancaCartaoRequest;
import com.thuler.gateway.dto.request.PagarCobrancaSaldoRequest;
import com.thuler.gateway.dto.response.CobrancaResponse;
import com.thuler.gateway.usecase.cobranca.CancelarCobrancaUseCase;
import com.thuler.gateway.usecase.cobranca.ConsultarCobrancasUseCase;
import com.thuler.gateway.usecase.cobranca.CriarCobrancaUseCase;
import com.thuler.gateway.usecase.cobranca.PagarCobrancaUseCase;
import com.thuler.gateway.domain.enums.CobrancaStatus;
import com.thuler.gateway.infrastructure.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cobrancas")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Cobranças", description = "Endpoints para gerenciamento de cobranças")
public class CobrancaController {

    private final CriarCobrancaUseCase criarCobrancaUseCase;
    private final ConsultarCobrancasUseCase consultarCobrancasUseCase;
    private final PagarCobrancaUseCase pagarCobrancaUseCase;
    private final CancelarCobrancaUseCase cancelarCobrancaUseCase;

    @PostMapping
    @Operation(summary = "Criar nova cobrança", description = "Cria uma cobrança para outro usuário usando o CPF do destinatário")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Cobrança criada com sucesso",
                    content = @Content(schema = @Schema(implementation = CobrancaResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Destinatário não encontrado"),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    public ResponseEntity<CobrancaResponse> criar(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody CriarCobrancaRequest request) {

        CobrancaResponse response = criarCobrancaUseCase.execute(user.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/enviadas")
    @Operation(summary = "Consultar cobranças enviadas", description = "Lista cobranças criadas pelo usuário autenticado")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cobranças listadas com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    public ResponseEntity<List<CobrancaResponse>> consultarEnviadas(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Parameter(description = "Filtrar por status (opcional)")
            @RequestParam(required = false) CobrancaStatus status) {

        List<CobrancaResponse> response = consultarCobrancasUseCase.consultarCobrancasEnviadas(user.getId(), status);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/recebidas")
    @Operation(summary = "Consultar cobranças recebidas", description = "Lista cobranças recebidas pelo usuário autenticado")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cobranças listadas com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    public ResponseEntity<List<CobrancaResponse>> consultarRecebidas(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Parameter(description = "Filtrar por status (opcional)")
            @RequestParam(required = false) CobrancaStatus status) {

        List<CobrancaResponse> response = consultarCobrancasUseCase.consultarCobrancasRecebidas(user.getId(), status);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/pagar/saldo")
    @Operation(summary = "Pagar cobrança com saldo", description = "Paga uma cobrança usando saldo em conta")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cobrança paga com sucesso",
                    content = @Content(schema = @Schema(implementation = CobrancaResponse.class))),
            @ApiResponse(responseCode = "400", description = "Saldo insuficiente ou cobrança inválida"),
            @ApiResponse(responseCode = "404", description = "Cobrança não encontrada"),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    public ResponseEntity<CobrancaResponse> pagarComSaldo(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody PagarCobrancaSaldoRequest request) {

        CobrancaResponse response = pagarCobrancaUseCase.pagarComSaldo(user.getId(), request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/pagar/cartao")
    @Operation(summary = "Pagar cobrança com cartão de crédito", description = "Paga uma cobrança usando cartão de crédito (integra com autorizador externo)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cobrança paga com sucesso",
                    content = @Content(schema = @Schema(implementation = CobrancaResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados do cartão inválidos"),
            @ApiResponse(responseCode = "404", description = "Cobrança não encontrada"),
            @ApiResponse(responseCode = "422", description = "Pagamento não autorizado"),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    public ResponseEntity<CobrancaResponse> pagarComCartao(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody PagarCobrancaCartaoRequest request) {

        CobrancaResponse response = pagarCobrancaUseCase.pagarComCartao(user.getId(), request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{cobrancaId}")
    @Operation(summary = "Cancelar cobrança", description = "Cancela uma cobrança (pendente ou paga com estorno)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cobrança cancelada com sucesso",
                    content = @Content(schema = @Schema(implementation = CobrancaResponse.class))),
            @ApiResponse(responseCode = "400", description = "Cobrança não pode ser cancelada"),
            @ApiResponse(responseCode = "404", description = "Cobrança não encontrada"),
            @ApiResponse(responseCode = "403", description = "Apenas o originador pode cancelar"),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    public ResponseEntity<CobrancaResponse> cancelar(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Parameter(description = "ID da cobrança a ser cancelada")
            @PathVariable Long cobrancaId) {

        CobrancaResponse response = cancelarCobrancaUseCase.execute(user.getId(), cobrancaId);
        return ResponseEntity.ok(response);
    }
}