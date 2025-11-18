package com.hammer.ecommerce.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hammer.ecommerce.dto.cart.AddToCartRequestDTO;
import com.hammer.ecommerce.dto.login.RegisterRequestDTO;
import com.hammer.ecommerce.dto.cart.UpdateCartItemRequestDTO;
import com.hammer.ecommerce.model.Category;
import com.hammer.ecommerce.model.Product;
import com.hammer.ecommerce.repositories.CartRepository;
import com.hammer.ecommerce.repositories.CategoryRepository;
import com.hammer.ecommerce.repositories.ProductRepository;
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
class CartControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    private String authToken;
    private Product product1;
    private Product product2;

    @BeforeEach
    void setUp() throws Exception {

        cartRepository.deleteAll();
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

        // Criar categoria
        Category category = new Category();
        category.setName("Eletrônicos");
        category.setDescription("Produtos eletrônicos");
        category = categoryRepository.save(category);

        // Criar produtos
        product1 = new Product();
        product1.setName("Mouse Gamer");
        product1.setDescription("Mouse RGB");
        product1.setPrice(new BigDecimal("150.00"));
        product1.setStockQuantity(20);
        product1.setSku("MOUSE-001");
        product1.setActive(true);
        product1.setCategory(category);
        product1 = productRepository.save(product1);

        product2 = new Product();
        product2.setName("Teclado Mecânico");
        product2.setDescription("Teclado RGB");
        product2.setPrice(new BigDecimal("300.00"));
        product2.setStockQuantity(10);
        product2.setSku("TECLADO-001");
        product2.setActive(true);
        product2.setCategory(category);
        product2 = productRepository.save(product2);
    }

    @Test
    @DisplayName("Deve retornar carrinho vazio inicialmente")
    void testGetCart_Empty() throws Exception {
        mockMvc.perform(get("/api/cart")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.totalAmount").value(0))
                .andExpect(jsonPath("$.totalItems").value(0));
    }

    @Test
    @DisplayName("Deve adicionar produto ao carrinho")
    void testAddToCart_Success() throws Exception {

        AddToCartRequestDTO request = new AddToCartRequestDTO();
        request.setProductId(product1.getId());
        request.setQuantity(2);

        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].productName").value("Mouse Gamer"))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].subtotal").value(300.00))
                .andExpect(jsonPath("$.totalAmount").value(300.00))
                .andExpect(jsonPath("$.totalItems").value(2));
    }

    @Test
    @DisplayName("Deve incrementar quantidade ao adicionar produto existente")
    void testAddToCart_IncrementExisting() throws Exception {

        // Adicionar produto pela primeira vez
        AddToCartRequestDTO request = new AddToCartRequestDTO();
        request.setProductId(product1.getId());
        request.setQuantity(2);

        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Adicionar o mesmo produto novamente
        request.setQuantity(3);

        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].quantity").value(5)) // 2 + 3
                .andExpect(jsonPath("$.totalAmount").value(750.00)); // 150 * 5
    }

    @Test
    @DisplayName("Deve adicionar múltiplos produtos ao carrinho")
    void testAddToCart_MultipleProducts() throws Exception {

        // Adicionar produto 1
        AddToCartRequestDTO request1 = new AddToCartRequestDTO();
        request1.setProductId(product1.getId());
        request1.setQuantity(2);

        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isOk());

        // Adicionar produto 2
        AddToCartRequestDTO request2 = new AddToCartRequestDTO();
        request2.setProductId(product2.getId());
        request2.setQuantity(1);

        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.totalAmount").value(600.00)) // (150*2) + (300*1)
                .andExpect(jsonPath("$.totalItems").value(3)); // 2 + 1
    }

    @Test
    @DisplayName("Deve retornar 400 ao adicionar quantidade maior que estoque")
    void testAddToCart_InsufficientStock() throws Exception {

        AddToCartRequestDTO request = new AddToCartRequestDTO();
        request.setProductId(product1.getId());
        request.setQuantity(25); // Estoque é 20

        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Estoque insuficiente")));
    }

    @Test
    @DisplayName("Deve retornar 404 ao adicionar produto inexistente")
    void testAddToCart_ProductNotFound() throws Exception {

        AddToCartRequestDTO request = new AddToCartRequestDTO();
        request.setProductId(999L);
        request.setQuantity(1);

        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deve retornar 400 ao adicionar produto inativo")
    void testAddToCart_InactiveProduct() throws Exception {

        product1.setActive(false);
        productRepository.save(product1);

        AddToCartRequestDTO request = new AddToCartRequestDTO();
        request.setProductId(product1.getId());
        request.setQuantity(1);

        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Produto não está disponível"));
    }

    @Test
    @DisplayName("Deve atualizar quantidade de item no carrinho")
    void testUpdateCartItem() throws Exception {

        // Adicionar produto ao carrinho
        AddToCartRequestDTO addRequest = new AddToCartRequestDTO();
        addRequest.setProductId(product1.getId());
        addRequest.setQuantity(2);

        MvcResult addResult = mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String addResponse = addResult.getResponse().getContentAsString();
        Long itemId = objectMapper.readTree(addResponse).get("items").get(0).get("id").asLong();

        // Atualizar quantidade
        UpdateCartItemRequestDTO updateRequest = new UpdateCartItemRequestDTO();
        updateRequest.setQuantity(5);

        mockMvc.perform(put("/api/cart/items/" + itemId)
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity").value(5))
                .andExpect(jsonPath("$.totalAmount").value(750.00)); // 150 * 5
    }

    @Test
    @DisplayName("Deve remover item do carrinho")
    void testRemoveCartItem() throws Exception {

        // Adicionar produto ao carrinho
        AddToCartRequestDTO addRequest = new AddToCartRequestDTO();
        addRequest.setProductId(product1.getId());
        addRequest.setQuantity(2);

        MvcResult addResult = mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String addResponse = addResult.getResponse().getContentAsString();
        Long itemId = objectMapper.readTree(addResponse).get("items").get(0).get("id").asLong();

        // Remover item
        mockMvc.perform(delete("/api/cart/items/" + itemId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.totalAmount").value(0));
    }

    @Test
    @DisplayName("Deve limpar carrinho completamente")
    void testClearCart() throws Exception {

        // Adicionar produtos ao carrinho
        AddToCartRequestDTO request1 = new AddToCartRequestDTO();
        request1.setProductId(product1.getId());
        request1.setQuantity(2);

        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isOk());

        AddToCartRequestDTO request2 = new AddToCartRequestDTO();
        request2.setProductId(product2.getId());
        request2.setQuantity(1);

        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isOk());

        // Limpar carrinho
        mockMvc.perform(delete("/api/cart")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNoContent());

        // Verificar que está vazio
        mockMvc.perform(get("/api/cart")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.totalAmount").value(0));
    }

    @Test
    @DisplayName("Deve retornar 401 ao acessar carrinho sem autenticação")
    void testGetCart_Unauthorized() throws Exception {

        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Deve calcular subtotais e total corretamente")
    void testCart_CalculateTotals() throws Exception {

        // Adicionar produto 1: 2 unidades de R$ 150,00 = R$ 300,00
        AddToCartRequestDTO request1 = new AddToCartRequestDTO();
        request1.setProductId(product1.getId());
        request1.setQuantity(2);

        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isOk());

        // Adicionar produto 2: 3 unidades de R$ 300,00 = R$ 900,00
        AddToCartRequestDTO request2 = new AddToCartRequestDTO();
        request2.setProductId(product2.getId());
        request2.setQuantity(3);

        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].subtotal").value(300.00))
                .andExpect(jsonPath("$.items[1].subtotal").value(900.00))
                .andExpect(jsonPath("$.totalAmount").value(1200.00))
                .andExpect(jsonPath("$.totalAmount").value(1200.00)) // 300 + 900
                .andExpect(jsonPath("$.totalItems").value(5)); // 2 + 3
    }
}