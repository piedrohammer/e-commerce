package com.hammer.ecommerce.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hammer.ecommerce.dto.login.LoginRequestDTO;
import com.hammer.ecommerce.dto.login.RegisterRequestDTO;
import com.hammer.ecommerce.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Deve registrar novo usuário com sucesso")
    void testRegister_Success() throws Exception {
        RegisterRequestDTO request = new RegisterRequestDTO();
        request.setName("João Silva");
        request.setEmail("joao@email.com");
        request.setPassword("senha123");
        request.setCpf("12345678901");
        request.setPhone("11999999999");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.userId").exists())
                .andExpect(jsonPath("$.name").value("João Silva"))
                .andExpect(jsonPath("$.email").value("joao@email.com"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    @DisplayName("Deve retornar 400 ao registrar com dados inválidos")
    void testRegister_InvalidData() throws Exception {
        RegisterRequestDTO request = new RegisterRequestDTO();
        request.setName("Jo"); // Nome muito curto
        request.setEmail("email-invalido");
        request.setPassword("123"); // Senha muito curta
        request.setCpf("123"); // CPF inválido

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").exists());
    }

    @Test
    @DisplayName("Deve retornar 400 ao registrar com email duplicado")
    void testRegister_DuplicateEmail() throws Exception {
        // Primeiro registro
        RegisterRequestDTO firstRequest = new RegisterRequestDTO();
        firstRequest.setName("João Silva");
        firstRequest.setEmail("joao@email.com");
        firstRequest.setPassword("senha123");
        firstRequest.setCpf("12345678901");
        firstRequest.setPhone("11999999999");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstRequest)))
                .andExpect(status().isCreated());

        // Segundo registro com mesmo email
        RegisterRequestDTO secondRequest = new RegisterRequestDTO();
        secondRequest.setName("Maria Silva");
        secondRequest.setEmail("joao@email.com"); // Email duplicado
        secondRequest.setPassword("senha456");
        secondRequest.setCpf("98765432100");
        secondRequest.setPhone("11988888888");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email já cadastrado"));
    }

    @Test
    @DisplayName("Deve fazer login com sucesso")
    void testLogin_Success() throws Exception {
        // Registrar usuário primeiro
        RegisterRequestDTO registerRequest = new RegisterRequestDTO();
        registerRequest.setName("João Silva");
        registerRequest.setEmail("joao@email.com");
        registerRequest.setPassword("senha123");
        registerRequest.setCpf("12345678901");
        registerRequest.setPhone("11999999999");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        // Login
        LoginRequestDTO loginRequest = new LoginRequestDTO();
        loginRequest.setEmail("joao@email.com");
        loginRequest.setPassword("senha123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.name").value("João Silva"))
                .andExpect(jsonPath("$.email").value("joao@email.com"));
    }

    @Test
    @DisplayName("Deve retornar 401 ao fazer login com senha incorreta")
    void testLogin_WrongPassword() throws Exception {
        // Registrar usuário
        RegisterRequestDTO registerRequest = new RegisterRequestDTO();
        registerRequest.setName("João Silva");
        registerRequest.setEmail("joao@email.com");
        registerRequest.setPassword("senha123");
        registerRequest.setCpf("12345678901");
        registerRequest.setPhone("11999999999");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        // Login com senha errada
        LoginRequestDTO loginRequest = new LoginRequestDTO();
        loginRequest.setEmail("joao@email.com");
        loginRequest.setPassword("senhaerrada");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Email ou senha inválidos"));
    }

    @Test
    @DisplayName("Deve retornar 401 ao fazer login com usuário inexistente")
    void testLogin_UserNotFound() throws Exception {
        LoginRequestDTO loginRequest = new LoginRequestDTO();
        loginRequest.setEmail("naoexiste@email.com");
        loginRequest.setPassword("senha123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }
}