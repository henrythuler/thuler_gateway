package com.thuler.gateway.usecase.cobranca;

import com.thuler.gateway.dto.request.CriarCobrancaRequest;
import com.thuler.gateway.dto.response.CobrancaResponse;
import com.thuler.gateway.domain.model.Cobranca;
import com.thuler.gateway.domain.model.Usuario;
import com.thuler.gateway.domain.enums.CobrancaStatus;
import com.thuler.gateway.domain.repository.CobrancaRepository;
import com.thuler.gateway.domain.repository.UsuarioRepository;
import com.thuler.gateway.domain.valueobject.CPF;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CriarCobrancaUseCase Tests")
class CriarCobrancaUseCaseTest {

    @Mock
    private CobrancaRepository cobrancaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private CriarCobrancaUseCase criarCobrancaUseCase;

    private Usuario originador;
    private Usuario destinatario;
    private CriarCobrancaRequest request;

    @BeforeEach
    void setUp() {
        originador = Usuario.builder()
                .id(1L)
                .nome("João Silva")
                .cpf(CPF.of("12345678909"))
                .email("joao@example.com")
                .build();

        destinatario = Usuario.builder()
                .id(2L)
                .nome("Maria Santos")
                .cpf(CPF.of("98765432100"))
                .email("maria@example.com")
                .build();

        request = CriarCobrancaRequest.builder()
                .cpfDestinatario("98765432100")
                .valor(BigDecimal.valueOf(100))
                .descricao("Cobrança teste")
                .build();
    }

    @Test
    @DisplayName("Deve criar cobrança com sucesso")
    void deveCriarCobrancaComSucesso() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(originador));
        when(usuarioRepository.findByCpf(any(CPF.class))).thenReturn(Optional.of(destinatario));

        Cobranca cobrancaSalva = Cobranca.builder()
                .id(1L)
                .originador(originador)
                .destinatario(destinatario)
                .valor(BigDecimal.valueOf(100))
                .descricao("Cobrança teste")
                .status(CobrancaStatus.PENDENTE)
                .build();

        when(cobrancaRepository.save(any(Cobranca.class))).thenReturn(cobrancaSalva);

        CobrancaResponse response = criarCobrancaUseCase.execute(1L, request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("João Silva", response.getOriginadorNome());
        assertEquals("Maria Santos", response.getDestinatarioNome());
        assertEquals(BigDecimal.valueOf(100), response.getValor());
        assertEquals("Cobrança teste", response.getDescricao());
        assertEquals(CobrancaStatus.PENDENTE, response.getStatus());

        verify(usuarioRepository).findById(1L);
        verify(usuarioRepository).findByCpf(any(CPF.class));
        verify(cobrancaRepository).save(any(Cobranca.class));
    }

    @Test
    @DisplayName("Deve lançar exceção quando originador não existe")
    void deveLancarExcecaoQuandoOriginadorNaoExiste() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> criarCobrancaUseCase.execute(1L, request)
        );

        assertEquals("Originador não encontrado", exception.getMessage());
        verify(usuarioRepository).findById(1L);
        verify(usuarioRepository, never()).findByCpf(any());
        verify(cobrancaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar exceção quando destinatário não existe")
    void deveLancarExcecaoQuandoDestinatarioNaoExiste() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(originador));
        when(usuarioRepository.findByCpf(any(CPF.class))).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> criarCobrancaUseCase.execute(1L, request)
        );

        assertEquals("Destinatário não encontrado", exception.getMessage());
        verify(cobrancaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar exceção quando tenta criar cobrança para si mesmo")
    void deveLancarExcecaoQuandoTentaCriarCobrancaParaSiMesmo() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(originador));
        when(usuarioRepository.findByCpf(any(CPF.class))).thenReturn(Optional.of(originador));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> criarCobrancaUseCase.execute(1L, request)
        );

        assertEquals("Não é possível criar cobrança para si mesmo", exception.getMessage());
        verify(cobrancaRepository, never()).save(any());
    }
}