package com.thuler.gateway.usecase.usuario;

import com.thuler.gateway.dto.request.LoginRequest;
import com.thuler.gateway.dto.response.LoginResponse;
import com.thuler.gateway.domain.model.Usuario;
import com.thuler.gateway.domain.repository.UsuarioRepository;
import com.thuler.gateway.domain.valueobject.CPF;
import com.thuler.gateway.infrastructure.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoginUseCase Tests")
class LoginUseCaseTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private LoginUseCase loginUseCase;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder()
                .id(1L)
                .nome("João Silva")
                .cpf(CPF.of("12345678909"))
                .email("joao@example.com")
                .senha("senha_encoded")
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Deve fazer login com email com sucesso")
    void deveFazerLoginComEmailComSucesso() {
        // Arrange
        LoginRequest request = LoginRequest.builder()
                .identificador("joao@example.com")
                .senha("senha123")
                .build();

        when(usuarioRepository.findByEmail("joao@example.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senha123", "senha_encoded")).thenReturn(true);
        when(jwtTokenProvider.generateToken(1L, "joao@example.com")).thenReturn("jwt_token");

        // Act
        LoginResponse response = loginUseCase.execute(request);

        // Assert
        assertNotNull(response);
        assertEquals("jwt_token", response.getToken());
        assertEquals("Bearer", response.getType());
        assertNotNull(response.getUsuario());
        assertEquals("João Silva", response.getUsuario().getNome());

        verify(usuarioRepository).findByEmail("joao@example.com");
        verify(passwordEncoder).matches("senha123", "senha_encoded");
        verify(jwtTokenProvider).generateToken(1L, "joao@example.com");
    }

    @Test
    @DisplayName("Deve fazer login com CPF com sucesso")
    void deveFazerLoginComCpfComSucesso() {
        // Arrange
        LoginRequest request = LoginRequest.builder()
                .identificador("12345678909")
                .senha("senha123")
                .build();

        when(usuarioRepository.findByCpf(any(CPF.class))).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senha123", "senha_encoded")).thenReturn(true);
        when(jwtTokenProvider.generateToken(1L, "joao@example.com")).thenReturn("jwt_token");

        // Act
        LoginResponse response = loginUseCase.execute(request);

        // Assert
        assertNotNull(response);
        assertEquals("jwt_token", response.getToken());
        assertEquals("Bearer", response.getType());

        verify(usuarioRepository).findByCpf(any(CPF.class));
        verify(passwordEncoder).matches("senha123", "senha_encoded");
        verify(jwtTokenProvider).generateToken(1L, "joao@example.com");
    }

    @Test
    @DisplayName("Deve lançar exceção quando usuário não existe")
    void deveLancarExcecaoQuandoUsuarioNaoExiste() {
        // Arrange
        LoginRequest request = LoginRequest.builder()
                .identificador("inexistente@example.com")
                .senha("senha123")
                .build();

        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> loginUseCase.execute(request)
        );

        assertEquals("Credenciais inválidas", exception.getMessage());
        verify(usuarioRepository).findByEmail("inexistente@example.com");
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    @DisplayName("Deve lançar exceção quando senha está incorreta")
    void deveLancarExcecaoQuandoSenhaIncorreta() {
        // Arrange
        LoginRequest request = LoginRequest.builder()
                .identificador("joao@example.com")
                .senha("senha_errada")
                .build();

        when(usuarioRepository.findByEmail("joao@example.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senha_errada", "senha_encoded")).thenReturn(false);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> loginUseCase.execute(request)
        );

        assertEquals("Credenciais inválidas", exception.getMessage());
        verify(usuarioRepository).findByEmail("joao@example.com");
        verify(passwordEncoder).matches("senha_errada", "senha_encoded");
        verify(jwtTokenProvider, never()).generateToken(any(), any());
    }

    @Test
    @DisplayName("Deve lançar exceção quando usuário está inativo")
    void deveLancarExcecaoQuandoUsuarioInativo() {
        // Arrange
        usuario.setActive(false);

        LoginRequest request = LoginRequest.builder()
                .identificador("joao@example.com")
                .senha("senha123")
                .build();

        when(usuarioRepository.findByEmail("joao@example.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senha123", "senha_encoded")).thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> loginUseCase.execute(request)
        );

        assertEquals("Usuário inativo", exception.getMessage());
        verify(jwtTokenProvider, never()).generateToken(any(), any());
    }
}