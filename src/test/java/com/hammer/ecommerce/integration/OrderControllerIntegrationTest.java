package com.hammer.ecommerce.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hammer.ecommerce.dto.cart.AddToCartRequestDTO;
import com.hammer.ecommerce.dto.address.AddressRequestDTO;
import com.hammer.ecommerce.dto.order.CreateOrderRequestDTO;
import com.hammer.ecommerce.dto.login.RegisterRequestDTO;
import com.hammer.ecommerce.model.*;
import com.hammer.ecommerce.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private UserRepository userRepository;

    private String authToken;
    private Product product;
    private Long addressId;
    private User testUser;

    @BeforeEach
    void setUp() throws Exception {

        orderRepository.deleteAll();
        cartRepository.deleteAll();
        addressRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        // Criar usuário e obter token
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

        // Criar categoria
        Category category = new Category();
        category.setName("Eletrônicos");
        category = categoryRepository.save(category);

        // Criar produto
        product = new Product();
        product.setName("Mouse Gamer");
        product.setPrice(new BigDecimal("150.00"));
        product.setStockQuantity(20);
        product.setSku("MOUSE-001");
        product.setActive(true);
        product.setCategory(category);
        product = productRepository.save(product);

        // Criar endereço
        AddressRequestDTO addressRequest = new AddressRequestDTO();
        addressRequest.setStreet("Rua das Flores");
        addressRequest.setNumber("123");
        addressRequest.setNeighborhood("Centro");
        addressRequest.setCity("São Paulo");
        addressRequest.setState("SP");
        addressRequest.setZipCode("01234-567");

        MvcResult addressResult = mockMvc.perform(post("/api/addresses")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addressRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String addressResponse = addressResult.getResponse().getContentAsString();
        addressId = objectMapper.readTree(addressResponse).get("id").asLong();
    }

    @Test
    @DisplayName("Deve criar pedido com sucesso (checkout)")
    void testCreateOrder_Success() throws Exception {

        // Adicionar produto ao carrinho
        AddToCartRequestDTO cartRequest = new AddToCartRequestDTO();
        cartRequest.setProductId(product.getId());
        cartRequest.setQuantity(2);

        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cartRequest)))
                .andExpect(status().isOk());

        // Criar pedido
        CreateOrderRequestDTO orderRequest = new CreateOrderRequestDTO();
        orderRequest.setShippingAddressId(addressId);

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderNumber").exists())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(300.00))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].productName").value("Mouse Gamer"))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.shippingAddress.street").value("Rua das Flores"));

        // Verificar que o carrinho foi limpo
        mockMvc.perform(get("/api/cart")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)));

        // Verificar que o estoque foi reduzido
        mockMvc.perform(get("/api/products/" + product.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockQuantity").value(18)); // 20 - 2
    }

    @Test
    @DisplayName("Deve retornar 400 ao criar pedido com carrinho vazio")
    void testCreateOrder_EmptyCart() throws Exception {

        CreateOrderRequestDTO orderRequest = new CreateOrderRequestDTO();
        orderRequest.setShippingAddressId(addressId);

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Carrinho está vazio"));
    }

    @Test
    @DisplayName("Deve retornar 404 ao criar pedido com endereço inexistente")
    void testCreateOrder_AddressNotFound() throws Exception {

        // Adicionar produto ao carrinho
        AddToCartRequestDTO cartRequest = new AddToCartRequestDTO();
        cartRequest.setProductId(product.getId());
        cartRequest.setQuantity(1);

        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cartRequest)))
                .andExpect(status().isOk());

        // Tentar criar pedido com endereço inexistente
        CreateOrderRequestDTO orderRequest = new CreateOrderRequestDTO();
        orderRequest.setShippingAddressId(999L);

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deve retornar 400 ao criar pedido com estoque insuficiente")
    void testCreateOrder_InsufficientStock() throws Exception {

        // Adicionar quantidade maior que o estoque
        AddToCartRequestDTO cartRequest = new AddToCartRequestDTO();
        cartRequest.setProductId(product.getId());
        cartRequest.setQuantity(5);

        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cartRequest)))
                .andExpect(status().isOk());

        // Reduzir estoque manualmente
        product.setStockQuantity(3);
        productRepository.save(product);

        // Tentar criar pedido
        CreateOrderRequestDTO orderRequest = new CreateOrderRequestDTO();
        orderRequest.setShippingAddressId(addressId);

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Estoque insuficiente")));
    }

    @Test
    @DisplayName("Deve listar pedidos do usuário")
    void testListOrders() throws Exception {

        // Criar dois pedidos
        createOrder();
        createOrder();

        mockMvc.perform(get("/api/orders")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$.content[0].orderNumber").exists())
                .andExpect(jsonPath("$.content[0].status").exists());
    }

    @Test
    @DisplayName("Deve buscar pedido por ID")
    void testGetOrderById() throws Exception {

        Long orderId = createOrder();

        mockMvc.perform(get("/api/orders/" + orderId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.orderNumber").exists())
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    @DisplayName("Deve retornar 404 ao buscar pedido inexistente")
    void testGetOrderById_NotFound() throws Exception {

        mockMvc.perform(get("/api/orders/999")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deve cancelar pedido com sucesso")
    void testCancelOrder() throws Exception {

        Long orderId = createOrder();

        mockMvc.perform(put("/api/orders/" + orderId + "/cancel")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // Verificar que o estoque foi devolvido
        mockMvc.perform(get("/api/products/" + product.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockQuantity").value(20)); // Estoque restaurado
    }

    @Test
    @DisplayName("Deve retornar 400 ao cancelar pedido já cancelado")
    void testCancelOrder_AlreadyCancelled() throws Exception {

        Long orderId = createOrder();

        // Cancelar primeira vez
        mockMvc.perform(put("/api/orders/" + orderId + "/cancel")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk());

        // Tentar cancelar novamente
        mockMvc.perform(put("/api/orders/" + orderId + "/cancel")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Pedido já está cancelado"));
    }

    @Test
    @DisplayName("Admin deve listar todos os pedidos")
    @WithMockUser(roles = "ADMIN")
    void testListAllOrders_Admin() throws Exception {

        createOrder();
        createOrder();

        mockMvc.perform(get("/api/orders/admin/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(2))));
    }

    @Test
    @DisplayName("Admin deve atualizar status do pedido")
    @WithMockUser(roles = "ADMIN")
    void testUpdateOrderStatus_Admin() throws Exception {

        Long orderId = createOrder();

        mockMvc.perform(put("/api/orders/" + orderId + "/status")
                        .param("status", "PAID"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    @DisplayName("Deve retornar 403 ao acessar endpoints admin como CUSTOMER")
    void testAdminEndpoints_Forbidden() throws Exception {
        mockMvc.perform(get("/api/orders/admin/all")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Deve paginar pedidos corretamente")
    void testPaginateOrders() throws Exception {

        // Criar vários pedidos
        for (int i = 0; i < 5; i++) {
            createOrder();
        }

        mockMvc.perform(get("/api/orders")
                        .header("Authorization", "Bearer " + authToken)
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(greaterThanOrEqualTo(5)));
    }

    private Long createOrder() throws Exception {

        // Adicionar produto ao carrinho
        AddToCartRequestDTO cartRequest = new AddToCartRequestDTO();
        cartRequest.setProductId(product.getId());
        cartRequest.setQuantity(1);

        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cartRequest)))
                .andExpect(status().isOk());

        // Criar pedido
        CreateOrderRequestDTO orderRequest = new CreateOrderRequestDTO();
        orderRequest.setShippingAddressId(addressId);

        MvcResult result = mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }
}