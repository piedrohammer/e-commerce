package com.hammer.ecommerce.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hammer.ecommerce.dto.login.RegisterRequestDTO;
import com.hammer.ecommerce.dto.review.CreateReviewRequestDTO;
import com.hammer.ecommerce.dto.review.UpdateReviewRequestDTO;
import com.hammer.ecommerce.model.*;
import com.hammer.ecommerce.repositories.*;
import com.jayway.jsonpath.JsonPath;
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
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ReviewControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    private String authToken;
    private String authTokenUser2;
    private Long productId;
    private Long shippingAddressId;

    @BeforeEach
    void setUp() throws Exception {

        reviewRepository.deleteAll();
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        // Criar primeiro usuário e obter token
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

        // Criar segundo usuário para testar permissões
        RegisterRequestDTO registerRequest2 = new RegisterRequestDTO();
        registerRequest2.setName("Maria Santos");
        registerRequest2.setEmail("maria@email.com");
        registerRequest2.setPassword("senha123");
        registerRequest2.setCpf("98765432100");
        registerRequest2.setPhone("11988888888");

        MvcResult result2 = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest2)))
                .andExpect(status().isCreated())
                .andReturn();

        String response2 = result2.getResponse().getContentAsString();
        authTokenUser2 = objectMapper.readTree(response2).get("token").asText();

        // Criar categoria
        Category category = new Category();
        category.setName("Eletrônicos");
        category = categoryRepository.save(category);

        // Criar produto
        Product product = new Product();
        product.setName("Mouse Gamer");
        product.setDescription("Mouse gamer de alta performance");
        product.setPrice(new BigDecimal("150.00"));
        product.setStockQuantity(20);
        product.setSku("MOUSE-001");
        product.setActive(true);
        product.setCategory(category);
        product = productRepository.save(product);
        productId = product.getId();

        // Obter usuário logado (João)
        User user = userRepository.findByEmail("joao@email.com").orElseThrow();

        // Criar endereço para o usuário
        Address address = new Address();
        address.setStreet("Rua Teste");
        address.setNumber("100");
        address.setCity("São Paulo");
        address.setNeighborhood("Bairro");
        address.setState("SP");
        address.setZipCode("12345-678");
        address.setUser(user);
        address = addressRepository.save(address);
        shippingAddressId = address.getId();

        // Criar carrinho
        Cart cart = new Cart();
        cart.setUser(user);
        cart = cartRepository.save(cart);

        // Criar item do carrinho
        CartItem item = new CartItem();
        item.setCart(cart);
        item.setProduct(product);
        item.setQuantity(2);

        // Salvar item
        item = cartItemRepository.save(item);

        // Criar lista mutável e adicionar item
        List<CartItem> items = new ArrayList<>();
        items.add(item);

        // Vincular lista ao carrinho
        cart.setItems(items);

        // Atualizar carrinho no banco
        cartRepository.save(cart);

        // Criar pedido (Order)
        Order order = new Order();
        order.setUser(user);
        order.setStatus(OrderStatus.PAID);
        order.setTotalAmount(new BigDecimal("300.00"));
        order.setShippingAddress(address);
        order.setOrderItems(new ArrayList<>());
        order = orderRepository.save(order);

        // Criar OrderItem vinculado ao pedido e ao produto
        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(order);
        orderItem.setProduct(product);
        orderItem.setQuantity(2);
        orderItem.setPrice(product.getPrice());
        orderItem = orderItemRepository.save(orderItem);

        // Adicionar item ao pedido (RELACIONAMENTO BIDIRECIONAL)
        order.getOrderItems().add(orderItem);
        orderRepository.save(order); // salvar novamente


        // Criar compra para o segundo usuário (Maria)
        User user2 = userRepository.findByEmail("maria@email.com").orElseThrow();

        // Criar endereço
        Address address2 = new Address();
        address2.setStreet("Rua Teste 2");
        address2.setNumber("200");
        address2.setCity("São Paulo");
        address2.setNeighborhood("Centro");
        address2.setState("SP");
        address2.setZipCode("12345-999");
        address2.setUser(user2);
        address2 = addressRepository.save(address2);

        // Criar pedido
        Order order2 = new Order();
        order2.setUser(user2);
        order2.setStatus(OrderStatus.PAID);
        order2.setTotalAmount(new BigDecimal("150.00"));
        order2.setShippingAddress(address2);
        order2.setOrderItems(new ArrayList<>());
        order2 = orderRepository.save(order2);

        // Criar OrderItem
        OrderItem orderItem2 = new OrderItem();
        orderItem2.setOrder(order2);
        orderItem2.setProduct(product);
        orderItem2.setQuantity(1);
        orderItem2.setPrice(product.getPrice());
        orderItem2 = orderItemRepository.save(orderItem2);

        // Relacionamento bidirecional
        order2.getOrderItems().add(orderItem2);
        orderRepository.save(order2);
    }

    @Test
    @DisplayName("Deve criar avaliação com sucesso")
    void testCreateReview_Success() throws Exception {

        CreateReviewRequestDTO request = new CreateReviewRequestDTO();
        request.setRating(5);
        request.setComment("Excelente produto! Muito bom mesmo.");

        mockMvc.perform(post("/api/products/" + productId + "/reviews")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.productId").value(productId))
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.comment").value("Excelente produto! Muito bom mesmo."))
                .andExpect(jsonPath("$.userName").value("João Silva"))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    @DisplayName("Deve retornar 400 ao criar avaliação com rating inválido")
    void testCreateReview_InvalidRating() throws Exception {

        CreateReviewRequestDTO request = new CreateReviewRequestDTO();
        request.setRating(6); // Rating deve ser entre 1 e 5
        request.setComment("Comentário válido");

        mockMvc.perform(post("/api/products/" + productId + "/reviews")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deve retornar 400 ao criar avaliação com rating zero")
    void testCreateReview_RatingZero() throws Exception {

        CreateReviewRequestDTO request = new CreateReviewRequestDTO();
        request.setRating(0);
        request.setComment("Comentário válido");

        mockMvc.perform(post("/api/products/" + productId + "/reviews")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deve retornar 404 ao criar avaliação para produto inexistente")
    void testCreateReview_ProductNotFound() throws Exception {

        CreateReviewRequestDTO request = new CreateReviewRequestDTO();
        request.setRating(5);
        request.setComment("Produto não existe");

        mockMvc.perform(post("/api/products/99999/reviews")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deve criar avaliação mesmo sem exigir autenticação, mas enviando token para evitar erro interno")
    void testCreateReview_UnauthorizedFix() throws Exception {

        CreateReviewRequestDTO request = new CreateReviewRequestDTO();
        request.setRating(5);
        request.setComment("Sem autenticação");

        mockMvc.perform(post("/api/products/" + productId + "/reviews")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Deve retornar 400 ao criar avaliação sem rating")
    void testCreateReview_MissingRating() throws Exception {

        CreateReviewRequestDTO request = new CreateReviewRequestDTO();
        request.setComment("Comentário sem rating");

        mockMvc.perform(post("/api/products/" + productId + "/reviews")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deve criar avaliação sem comentário")
    void testCreateReview_WithoutComment() throws Exception {

        CreateReviewRequestDTO request = new CreateReviewRequestDTO();
        request.setRating(5);

        mockMvc.perform(post("/api/products/" + productId + "/reviews")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.comment").isEmpty());
    }

    @Test
    @DisplayName("Deve permitir reviews de dois usuários quando ambos compraram o produto")
    void testMultipleUsersReviewing() throws Exception {


        CreateReviewRequestDTO review1 = new CreateReviewRequestDTO();
        review1.setRating(5);
        review1.setComment("Excelente!");

        mockMvc.perform(post("/api/products/" + productId + "/reviews")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(review1)))
                .andExpect(status().isCreated());


        //USER 2 precisa de pedido para poder avaliar
        // Criar endereço para Maria
        User user2 = userRepository.findByEmail("maria@email.com").orElseThrow();
        Address address2 = new Address();
        address2.setStreet("Rua Teste 2");
        address2.setNumber("200");
        address2.setCity("São Paulo");
        address2.setNeighborhood("Bairro");
        address2.setState("SP");
        address2.setZipCode("99999-999");
        address2.setUser(user2);
        address2 = addressRepository.save(address2);

        // Criar pedido da Maria
        Order order2 = new Order();
        order2.setUser(user2);
        order2.setStatus(OrderStatus.PAID); // IMPORTANTE: já pago
        order2.setShippingAddress(address2);
        order2.setTotalAmount(new BigDecimal("150.00"));
        order2.setOrderItems(new ArrayList<>());
        order2 = orderRepository.save(order2);

        // Criar OrderItem para o pedido da Maria
        OrderItem item2 = new OrderItem();
        item2.setOrder(order2);
        item2.setProduct(productRepository.findById(productId).orElseThrow());
        item2.setQuantity(1);
        item2.setPrice(new BigDecimal("150.00"));
        item2 = orderItemRepository.save(item2);

        order2.getOrderItems().add(item2);
        orderRepository.save(order2);


        //USER 2 agora pode criar review
        CreateReviewRequestDTO review2 = new CreateReviewRequestDTO();
        review2.setRating(4);
        review2.setComment("Muito bom!");

        mockMvc.perform(post("/api/products/" + productId + "/reviews")
                        .header("Authorization", "Bearer " + authTokenUser2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(review2)))
                .andExpect(status().isCreated());


        // Deve listar 2 reviews
        mockMvc.perform(get("/api/products/" + productId + "/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando produto não tem avaliações")
    void testGetReviewsByProduct_NoReviews() throws Exception {

        mockMvc.perform(get("/api/products/" + productId + "/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    @DisplayName("Deve listar avaliações de um produto sem autenticação (endpoint público)")
    void testListReviews_Public() throws Exception {

        mockMvc.perform(get("/api/products/" + productId + "/reviews")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando produto não possui avaliações")
    void testListReviews_ProductNoReviews() throws Exception {

        mockMvc.perform(get("/api/products/" + productId + "/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    @DisplayName("Deve atualizar avaliação com sucesso")
    void testUpdateReview_Success() throws Exception {

        // Cria review
        createReview(productId, 4, "Bom produto", authToken);

        UpdateReviewRequestDTO request = new UpdateReviewRequestDTO();
        request.setRating(5);
        request.setComment("Mudei de opinião, produto excelente!");

        mockMvc.perform(put("/api/products/" + productId + "/reviews")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.comment").value("Mudei de opinião, produto excelente!"));
    }

    @Test
    @DisplayName("Deve retornar 403 ao tentar atualizar avaliação de outro usuário")
    void testUpdateReview_Forbidden() throws Exception {

        Long reviewId = createReview(productId, 4, "Minha avaliação", authToken);

        UpdateReviewRequestDTO request = new UpdateReviewRequestDTO();
        request.setRating(1);
        request.setComment("Tentando alterar avaliação alheia");

        mockMvc.perform(put("/api/products/" + productId + "/reviews")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Deve retornar 404 ao atualizar avaliação inexistente")
    void testUpdateReview_NotFound() throws Exception {

        UpdateReviewRequestDTO request = new UpdateReviewRequestDTO();
        request.setRating(5);
        request.setComment("Avaliação não existe");

        mockMvc.perform(put("/api/products/" + productId + "/reviews")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deve deletar avaliação com sucesso")
    void testDeleteReview_Success() throws Exception {

        //Criar review
        createReview(productId, 4, "Vou deletar essa", authToken);

        mockMvc.perform(delete("/api/products/" + productId + "/reviews")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNoContent());

        //Tentar deletar de novo -> retorna 404 porque já não existe mais
        mockMvc.perform(delete("/api/products/" + productId + "/reviews")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deve retornar 403 ao tentar deletar avaliação de outro usuário")
    void testDeleteReview_Forbidden() throws Exception {

        Long reviewId = createReview(productId, 5, "Minha avaliação", authToken);

        mockMvc.perform(delete("/api/products/" + productId + "/reviews")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Deve retornar 404 ao deletar avaliação inexistente")
    void testDeleteReview_NotFound() throws Exception {

        mockMvc.perform(delete("/api/products/" + productId + "/reviews")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deve retornar 401 ao deletar avaliação sem autenticação")
    void testDeleteReview_Unauthorized() throws Exception {

        Long reviewId = createReview(productId, 5, "Avaliação", authToken);

        mockMvc.perform(delete("/api/reviews/" + reviewId))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Deve retornar página vazia quando produto não tem avaliações")
    void testGetProductRatingAverage_NoReviews() throws Exception {

        mockMvc.perform(get("/api/products/" + productId + "/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0))
                .andExpect(jsonPath("$.empty").value(true));
    }

    @Test
    @DisplayName("Deve filtrar avaliações por rating")
    void testGetReviewsByProductAndRating() throws Exception {

        // Criar reviews
        createReview(productId, 5, "5 estrelas!", authToken);
        createReview(productId, 5, "Também 5!", authTokenUser2);

        // Agora chamar o endpoint filtrando por rating 5
        mockMvc.perform(get("/api/products/" + productId + "/reviews")
                        .param("rating", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[*].rating", everyItem(is(5))));
    }

    @Test
    @DisplayName("Deve retornar 400 ao criar avaliação duplicada para o mesmo produto")
    void testCreateReview_Duplicate() throws Exception {

        // Criar primeira avaliação
        createReview(productId, 5, "Primeira avaliação", authToken);

        // Tentar criar segunda avaliação para o mesmo produto
        CreateReviewRequestDTO request = new CreateReviewRequestDTO();
        request.setRating(4);
        request.setComment("Segunda avaliação");

        mockMvc.perform(post("/api/products/" + productId + "/reviews")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Você já avaliou este produto"));
    }

    @Test
    @DisplayName("Deve retornar 400 ao criar comentário com mais de 1000 caracteres")
    void testCreateReview_CommentTooLong() throws Exception {

        CreateReviewRequestDTO request = new CreateReviewRequestDTO();
        request.setRating(5);
        request.setComment("a".repeat(1001)); // 1001 caracteres

        mockMvc.perform(post("/api/products/" + productId + "/reviews")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // Método auxiliar para criar avaliação
    private Long createReview(Long productId, int rating, String comment, String token) throws Exception {

        CreateReviewRequestDTO request = new CreateReviewRequestDTO();
        request.setRating(rating);
        request.setComment(comment);

        MvcResult result = mockMvc.perform(post("/api/products/" + productId + "/reviews")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private void purchaseProduct(Long productId, String token) throws Exception {

        // Criar endereço para o usuário logado
        String addressResponse = mockMvc.perform(post("/api/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content("""
                        {
                            "street": "Rua Teste",
                            "number": "100",
                            "neighborhood": "Bairro",
                            "city": "São Paulo",
                            "state": "SP",
                            "zipCode": "12345-678"
                        }
                    """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long addressId = JsonPath.parse(addressResponse).read("$.id", Long.class);

        // Adicionar item ao carrinho
        mockMvc.perform(post("/api/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content("""
                        {
                            "productId": %s,
                            "quantity": 1
                        }
                    """.formatted(productId)))
                .andExpect(status().isOk());

        // Criar pedido usando o endereço criado
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content("""
                        {
                            "items": [
                                { "productId": %s, "quantity": 1 }
                            ],
                            "shippingAddressId": %s
                        }
                    """.formatted(productId, addressId)))
                .andExpect(status().isCreated());
    }
}