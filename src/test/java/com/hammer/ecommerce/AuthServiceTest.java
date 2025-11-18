package com.hammer.ecommerce;

import com.hammer.ecommerce.dto.login.AuthResponseDTO;
import com.hammer.ecommerce.dto.login.LoginRequestDTO;
import com.hammer.ecommerce.dto.login.RegisterRequestDTO;
import com.hammer.ecommerce.exceptions.BusinessException;
import com.hammer.ecommerce.model.Role;
import com.hammer.ecommerce.model.User;
import com.hammer.ecommerce.repositories.UserRepository;
import com.hammer.ecommerce.security.JwtUtil;
import com.hammer.ecommerce.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private RegisterRequestDTO registerRequest;
    private LoginRequestDTO loginRequest;
    private User user;

    @BeforeEach
    void setUp() {

        registerRequest = new RegisterRequestDTO();
        registerRequest.setName("João Silva");
        registerRequest.setEmail("joao@email.com");
        registerRequest.setPassword("senha123");
        registerRequest.setCpf("12345678901");
        registerRequest.setPhone("11999999999");

        loginRequest = new LoginRequestDTO();
        loginRequest.setEmail("joao@email.com");
        loginRequest.setPassword("senha123");

        user = new User();
        user.setId(1L);
        user.setName("João Silva");
        user.setEmail("joao@email.com");
        user.setPassword("$2a$10$encodedPassword");
        user.setCpf("12345678901");
        user.setPhone("11999999999");
        user.setRole(Role.CUSTOMER);
    }

    @Test
    @DisplayName("Deve registrar novo usuário com sucesso")
    void testRegister_Success() {

        // Arrange
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(userRepository.existsByCpf(registerRequest.getCpf())).thenReturn(false);
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("$2a$10$encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(jwtUtil.generateToken(user.getEmail(), user.getRole().name())).thenReturn("mock-jwt-token");

        // Act
        AuthResponseDTO response = authService.register(registerRequest);

        // Assert
        assertNotNull(response);
        assertEquals("mock-jwt-token", response.getToken());
        assertEquals("João Silva", response.getName());
        assertEquals("joao@email.com", response.getEmail());
        assertEquals("CUSTOMER", response.getRole());
        verify(userRepository, times(1)).existsByEmail(registerRequest.getEmail());
        verify(userRepository, times(1)).existsByCpf(registerRequest.getCpf());
        verify(passwordEncoder, times(1)).encode(registerRequest.getPassword());
        verify(userRepository, times(1)).save(any(User.class));
        verify(jwtUtil, times(1)).generateToken(user.getEmail(), user.getRole().name());
    }

    @Test
    @DisplayName("Deve lançar exceção ao registrar com email duplicado")
    void testRegister_DuplicateEmail() {

        // Arrange
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(true);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            authService.register(registerRequest);
        });

        assertEquals("Email já cadastrado", exception.getMessage());
        verify(userRepository, times(1)).existsByEmail(registerRequest.getEmail());
        verify(userRepository, never()).existsByCpf(any());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao registrar com CPF duplicado")
    void testRegister_DuplicateCpf() {

        // Arrange
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(userRepository.existsByCpf(registerRequest.getCpf())).thenReturn(true);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            authService.register(registerRequest);
        });

        assertEquals("CPF já cadastrado", exception.getMessage());
        verify(userRepository, times(1)).existsByEmail(registerRequest.getEmail());
        verify(userRepository, times(1)).existsByCpf(registerRequest.getCpf());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Deve fazer login com sucesso")
    void testLogin_Success() {

        // Arrange
        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken(user.getEmail(), user.getRole().name())).thenReturn("mock-jwt-token");

        // Act
        AuthResponseDTO response = authService.login(loginRequest);

        // Assert
        assertNotNull(response);
        assertEquals("mock-jwt-token", response.getToken());
        assertEquals("João Silva", response.getName());
        assertEquals("joao@email.com", response.getEmail());
        assertEquals("CUSTOMER", response.getRole());
        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository, times(1)).findByEmail(loginRequest.getEmail());
        verify(jwtUtil, times(1)).generateToken(user.getEmail(), user.getRole().name());
    }

    @Test
    @DisplayName("Deve lançar exceção ao fazer login com credenciais inválidas")
    void testLogin_InvalidCredentials() {

        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        // Act & Assert
        assertThrows(BadCredentialsException.class, () -> {
            authService.login(loginRequest);
        });

        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    @DisplayName("Deve lançar exceção ao fazer login com usuário não encontrado")
    void testLogin_UserNotFound() {

        // Arrange
        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.empty());

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            authService.login(loginRequest);
        });

        assertEquals("Usuário não encontrado", exception.getMessage());
        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository, times(1)).findByEmail(loginRequest.getEmail());
    }
}