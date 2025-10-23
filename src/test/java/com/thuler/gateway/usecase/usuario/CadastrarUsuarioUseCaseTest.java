package com.thuler.gateway.usecase.usuario;

import com.thuler.gateway.dto.request.CadastroUsuarioRequest;
import com.thuler.gateway.dto.response.UsuarioResponse;
import com.thuler.gateway.domain.model.Usuario;
import com.thuler.gateway.domain.repository.UsuarioRepository;
import com.thuler.gateway.domain.valueobject.CPF;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CadastrarUsuarioUseCase Tests")
class CadastrarUsuarioUseCaseTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private CadastrarUsuarioUseCase cadastrarUsuarioUseCase;

    private CadastroUsuarioRequest request;

    @BeforeEach
    void setUp() {
        request = CadastroUsuarioRequest.builder()
                .nome("João Silva")
                .cpf("12345678909")
                .email("joao@example.com")
                .senha("senha123")
                .build();
    }

    @Test
    @DisplayName("Deve cadastrar usuário com sucesso")
    void deveCadastrarUsuarioComSucesso() {
        // Arrange
        when(usuarioRepository.existsByCpf(any(CPF.class))).thenReturn(false);
        when(usuarioRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("senha_encoded");

        Usuario usuarioSalvo = Usuario.builder()
                .id(1L)
                .nome(request.getNome())
                .cpf(CPF.of(request.getCpf()))
                .email(request.getEmail())
                .senha("senha_encoded")
                .active(true)
                .build();

        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuarioSalvo);

        // Act
        UsuarioResponse response = cadastrarUsuarioUseCase.execute(request);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("João Silva", response.getNome());
        assertEquals("123.456.789-09", response.getCpf());
        assertEquals("joao@example.com", response.getEmail());
        assertTrue(response.getActive());

        verify(usuarioRepository).existsByCpf(any(CPF.class));
        verify(usuarioRepository).existsByEmail("joao@example.com");
        verify(passwordEncoder).encode("senha123");
        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    @DisplayName("Deve lançar exceção quando CPF já existe")
    void deveLancarExcecaoQuandoCpfJaExiste() {
        // Arrange
        when(usuarioRepository.existsByCpf(any(CPF.class))).thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> cadastrarUsuarioUseCase.execute(request)
        );

        assertEquals("CPF já cadastrado", exception.getMessage());
        verify(usuarioRepository).existsByCpf(any(CPF.class));
        verify(usuarioRepository, never()).existsByEmail(anyString());
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    @DisplayName("Deve lançar exceção quando Email já existe")
    void deveLancarExcecaoQuandoEmailJaExiste() {
        // Arrange
        when(usuarioRepository.existsByCpf(any(CPF.class))).thenReturn(false);
        when(usuarioRepository.existsByEmail(anyString())).thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> cadastrarUsuarioUseCase.execute(request)
        );

        assertEquals("Email já cadastrado", exception.getMessage());
        verify(usuarioRepository).existsByCpf(any(CPF.class));
        verify(usuarioRepository).existsByEmail("joao@example.com");
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    @DisplayName("Deve criar conta automaticamente ao cadastrar usuário")
    void deveCriarContaAutomaticamenteAoCadastrarUsuario() {
        // Arrange
        when(usuarioRepository.existsByCpf(any(CPF.class))).thenReturn(false);
        when(usuarioRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("senha_encoded");

        Usuario usuarioCapturado = Usuario.builder().build();

        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario usuario = invocation.getArgument(0);
            usuario.setId(1L);
            return usuario;
        });

        // Act
        UsuarioResponse response = cadastrarUsuarioUseCase.execute(request);

        // Assert
        assertNotNull(response);
        verify(usuarioRepository).save(argThat(usuario -> usuario.getConta() != null));
    }
}
