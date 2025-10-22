package com.thuler.gateway.controller;

import com.thuler.gateway.dto.request.DepositoRequest;
import com.thuler.gateway.dto.response.ContaResponse;
import com.thuler.gateway.usecase.conta.DepositarUseCase;
import com.thuler.gateway.domain.model.Conta;
import com.thuler.gateway.domain.repository.ContaRepository;
import com.thuler.gateway.infrastructure.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/conta")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Conta", description = "Endpoints para gerenciamento de conta e saldo")
public class ContaController {

    private final DepositarUseCase depositarUseCase;
    private final ContaRepository contaRepository;

    @PostMapping("/deposito")
    @Operation(summary = "Realizar depósito", description = "Adiciona saldo na conta do usuário")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Depósito realizado com sucesso",
                    content = @Content(schema = @Schema(implementation = ContaResponse.class))),
            @ApiResponse(responseCode = "400", description = "Valor inválido"),
            @ApiResponse(responseCode = "422", description = "Depósito não autorizado"),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    public ResponseEntity<ContaResponse> depositar(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody DepositoRequest request) {

        ContaResponse response = depositarUseCase.execute(user.getId(), request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/saldo")
    @Operation(summary = "Consultar saldo", description = "Retorna o saldo atual da conta do usuário")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Saldo consultado com sucesso",
                    content = @Content(schema = @Schema(implementation = ContaResponse.class))),
            @ApiResponse(responseCode = "404", description = "Conta não encontrada"),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    public ResponseEntity<ContaResponse> consultarSaldo(@AuthenticationPrincipal AuthenticatedUser user) {
        Conta conta = contaRepository.findByUsuarioId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada"));

        return ResponseEntity.ok(ContaResponse.fromEntity(conta));
    }
}