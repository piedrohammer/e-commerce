package com.hammer.ecommerce.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hammer.ecommerce.dto.address.AddressRequestDTO;
import com.hammer.ecommerce.dto.login.RegisterRequestDTO;
import com.hammer.ecommerce.model.Address;
import com.hammer.ecommerce.model.User;
import com.hammer.ecommerce.repositories.AddressRepository;
import com.hammer.ecommerce.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AddressControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String authToken;
    private User testUser;

    @BeforeEach
    void setUp() throws Exception {

        addressRepository.deleteAll();
        userRepository.deleteAll();

        // Criar usuário de teste e obter token
        RegisterRequestDTO registerRequest = new RegisterRequestDTO();
        registerRequest.setName("João Silva");
        registerRequest.setEmail("joao@email.com");
        registerRequest.setPassword("senha123");
        registerRequest.setCpf("12345678901");
        registerRequest.setPhone("11999999999");

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        authToken = objectMapper.readTree(response).get("token").asText();

        testUser = userRepository.findByEmail("joao@email.com").orElseThrow();
    }

    @Test
    @DisplayName("Deve criar endereço com sucesso")
    void testCreateAddress_Success() throws Exception {

        AddressRequestDTO request = new AddressRequestDTO();
        request.setStreet("Rua das Flores");
        request.setNumber("123");
        request.setComplement("Apto 45");
        request.setNeighborhood("Centro");
        request.setCity("São Paulo");
        request.setState("SP");
        request.setZipCode("01234-567");
        request.setIsDefault(true);

        mockMvc.perform(post("/api/addresses")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.street").value("Rua das Flores"))
                .andExpect(jsonPath("$.number").value("123"))
                .andExpect(jsonPath("$.city").value("São Paulo"))
                .andExpect(jsonPath("$.state").value("SP"))
                .andExpect(jsonPath("$.zipCode").value("01234-567"))
                .andExpect(jsonPath("$.isDefault").value(true));
    }

    @Test
    @DisplayName("Deve retornar 401 ao criar endereço sem autenticação")
    void testCreateAddress_Unauthorized() throws Exception {

        AddressRequestDTO request = new AddressRequestDTO();
        request.setStreet("Rua das Flores");
        request.setNumber("123");
        request.setNeighborhood("Centro");
        request.setCity("São Paulo");
        request.setState("SP");
        request.setZipCode("01234-567");

        mockMvc.perform(post("/api/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Deve marcar primeiro endereço como padrão automaticamente")
    void testCreateAddress_FirstAsDefault() throws Exception {

        AddressRequestDTO request = new AddressRequestDTO();
        request.setStreet("Rua das Flores");
        request.setNumber("123");
        request.setNeighborhood("Centro");
        request.setCity("São Paulo");
        request.setState("SP");
        request.setZipCode("01234567");
        request.setIsDefault(false); // Marcar como false, mas deve virar true

        mockMvc.perform(post("/api/addresses")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isDefault").value(true));
    }

    @Test
    @DisplayName("Deve formatar CEP corretamente")
    void testCreateAddress_FormatZipCode() throws Exception {

        AddressRequestDTO request = new AddressRequestDTO();
        request.setStreet("Rua das Flores");
        request.setNumber("123");
        request.setNeighborhood("Centro");
        request.setCity("São Paulo");
        request.setState("SP");
        request.setZipCode("01234567"); // Sem hífen

        mockMvc.perform(post("/api/addresses")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.zipCode").value("01234-567")); // Com hífen
    }

    @Test
    @DisplayName("Deve retornar 400 ao criar endereço com CEP inválido")
    void testCreateAddress_InvalidZipCode() throws Exception {

        AddressRequestDTO request = new AddressRequestDTO();
        request.setStreet("Rua das Flores");
        request.setNumber("123");
        request.setNeighborhood("Centro");
        request.setCity("São Paulo");
        request.setState("SP");
        request.setZipCode("123"); // CEP inválido

        mockMvc.perform(post("/api/addresses")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deve retornar 400 ao criar endereço com dados inválidos")
    void testCreateAddress_InvalidData() throws Exception {

        AddressRequestDTO request = new AddressRequestDTO();
        request.setStreet(""); // Vazio
        request.setNumber("123");
        request.setNeighborhood("Centro");
        request.setCity("São Paulo");
        request.setState("S"); // UF com 1 caractere
        request.setZipCode("01234-567");

        mockMvc.perform(post("/api/addresses")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").exists());
    }

    @Test
    @DisplayName("Deve listar endereços do usuário")
    void testListAddresses() throws Exception {
        // Criar dois endereços
        createAddress("Rua A", "100", "SP");
        createAddress("Rua B", "200", "RJ");

        mockMvc.perform(get("/api/addresses")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].street").exists())
                .andExpect(jsonPath("$[1].street").exists());
    }

    @Test
    @DisplayName("Deve buscar endereço por ID")
    void testGetAddressById() throws Exception {
        Address address = createAddress("Rua das Flores", "123", "SP");

        mockMvc.perform(get("/api/addresses/" + address.getId())
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.street").value("Rua das Flores"))
                .andExpect(jsonPath("$.number").value("123"));
    }

    @Test
    @DisplayName("Deve retornar 404 ao buscar endereço inexistente")
    void testGetAddressById_NotFound() throws Exception {
        mockMvc.perform(get("/api/addresses/999")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deve atualizar endereço")
    void testUpdateAddress() throws Exception {

        Address address = createAddress("Rua Original", "100", "SP");

        AddressRequestDTO request = new AddressRequestDTO();
        request.setStreet("Rua Atualizada");
        request.setNumber("200");
        request.setComplement("Bloco B");
        request.setNeighborhood("Centro");
        request.setCity("São Paulo");
        request.setState("SP");
        request.setZipCode("01234-567");
        request.setIsDefault(true);

        mockMvc.perform(put("/api/addresses/" + address.getId())
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.street").value("Rua Atualizada"))
                .andExpect(jsonPath("$.number").value("200"));
    }

    @Test
    @DisplayName("Deve deletar endereço")
    void testDeleteAddress() throws Exception {

        Address address = createAddress("Rua Delete", "100", "SP");

        mockMvc.perform(delete("/api/addresses/" + address.getId())
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNoContent());

        // Verificar que foi deletado
        mockMvc.perform(get("/api/addresses/" + address.getId())
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deve definir endereço como padrão")
    void testSetAddressAsDefault() throws Exception {

        Address address1 = createAddress("Rua 1", "100", "SP");
        address1.setIsDefault(true);
        addressRepository.save(address1);

        Address address2 = createAddress("Rua 2", "200", "RJ");
        address2.setIsDefault(false);
        addressRepository.save(address2);

        mockMvc.perform(patch("/api/addresses/" + address2.getId() + "/set-default")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isDefault").value(true));

        // Verificar que o primeiro não é mais padrão
        mockMvc.perform(get("/api/addresses/" + address1.getId())
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isDefault").value(false));
    }

    @Test
    @DisplayName("Deve desmarcar endereço padrão anterior ao criar novo padrão")
    void testCreateAddress_UnsetPreviousDefault() throws Exception {

        // Criar primeiro endereço (será padrão)
        Address first = createAddress("Rua 1", "100", "SP");
        first.setIsDefault(true);
        addressRepository.save(first);

        // Criar segundo endereço como padrão
        AddressRequestDTO request = new AddressRequestDTO();
        request.setStreet("Rua 2");
        request.setNumber("200");
        request.setNeighborhood("Centro");
        request.setCity("Rio de Janeiro");
        request.setState("RJ");
        request.setZipCode("20000-000");
        request.setIsDefault(true);

        mockMvc.perform(post("/api/addresses")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isDefault").value(true));

        // Verificar que o primeiro não é mais padrão
        mockMvc.perform(get("/api/addresses/" + first.getId())
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isDefault").value(false));
    }

    private Address createAddress(String street, String number, String state) {

        Address address = new Address();
        address.setStreet(street);
        address.setNumber(number);
        address.setNeighborhood("Centro");
        address.setCity("Cidade Teste");
        address.setState(state);
        address.setZipCode("01234-567");
        address.setIsDefault(false);
        address.setUser(testUser);
        return addressRepository.save(address);
    }
}