package com.hammer.ecommerce.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hammer.ecommerce.dto.login.RegisterRequestDTO;
import com.hammer.ecommerce.dto.review.CreateReviewRequestDTO;
import com.hammer.ecommerce.dto.review.UpdateReviewRequestDTO;
import com.hammer.ecommerce.model.*;
import com.hammer.ecommerce.repositories.*;
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

    private String authToken;
    private String authTokenUser2;
    private Long productId;

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
    @DisplayName("Deve retornar 401 ao criar avaliação sem autenticação")
    void testCreateReview_Unauthorized() throws Exception {

        CreateReviewRequestDTO request = new CreateReviewRequestDTO();
        request.setRating(5);
        request.setComment("Sem autenticação");

        mockMvc.perform(post("/api/products/" + productId + "/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
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
    @DisplayName("Deve listar avaliações de um produto")
    void testGetReviewsByProduct() throws Exception {

        // Criar várias avaliações
        createReview(productId, 5, "Ótimo produto!", authToken);
        createReview(productId, 4, "Muito bom!", authTokenUser2);
        createReview(productId, 3, "Razoável", authToken);

        mockMvc.perform(get("/api/products/" + productId + "/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$.content[0].rating").exists())
                .andExpect(jsonPath("$.content[0].userName").exists());
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
    @DisplayName("Deve buscar avaliação por ID")
    void testGetReviewById() throws Exception {

        Long reviewId = createReview(productId, 5, "Excelente!", authToken);

        mockMvc.perform(get("/api/reviews/" + reviewId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reviewId))
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.comment").value("Excelente!"))
                .andExpect(jsonPath("$.userName").value("João Silva"));
    }

    @Test
    @DisplayName("Deve retornar 404 ao buscar avaliação inexistente")
    void testGetReviewById_NotFound() throws Exception {

        mockMvc.perform(get("/api/reviews/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deve atualizar avaliação com sucesso")
    void testUpdateReview_Success() throws Exception {

        Long reviewId = createReview(productId, 4, "Bom produto", authToken);

        UpdateReviewRequestDTO request = new UpdateReviewRequestDTO();
        request.setRating(5);
        request.setComment("Mudei de opinião, produto excelente!");

        mockMvc.perform(put("/api/reviews/" + reviewId)
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reviewId))
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.comment").value("Mudei de opinião, produto excelente!"))
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    @DisplayName("Deve retornar 403 ao tentar atualizar avaliação de outro usuário")
    void testUpdateReview_Forbidden() throws Exception {

        Long reviewId = createReview(productId, 4, "Minha avaliação", authToken);

        UpdateReviewRequestDTO request = new UpdateReviewRequestDTO();
        request.setRating(1);
        request.setComment("Tentando alterar avaliação alheia");

        mockMvc.perform(put("/api/reviews/" + reviewId)
                        .header("Authorization", "Bearer " + authTokenUser2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Deve retornar 404 ao atualizar avaliação inexistente")
    void testUpdateReview_NotFound() throws Exception {

        UpdateReviewRequestDTO request = new UpdateReviewRequestDTO();
        request.setRating(5);
        request.setComment("Avaliação não existe");

        mockMvc.perform(put("/api/reviews/99999")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deve deletar avaliação com sucesso")
    void testDeleteReview_Success() throws Exception {

        Long reviewId = createReview(productId, 4, "Vou deletar essa", authToken);

        mockMvc.perform(delete("/api/reviews/" + reviewId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNoContent());

        // Verificar que foi deletada
        mockMvc.perform(get("/api/reviews/" + reviewId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deve retornar 403 ao tentar deletar avaliação de outro usuário")
    void testDeleteReview_Forbidden() throws Exception {

        Long reviewId = createReview(productId, 5, "Minha avaliação", authToken);

        mockMvc.perform(delete("/api/reviews/" + reviewId)
                        .header("Authorization", "Bearer " + authTokenUser2))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Deve retornar 404 ao deletar avaliação inexistente")
    void testDeleteReview_NotFound() throws Exception {

        mockMvc.perform(delete("/api/reviews/99999")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deve retornar 401 ao deletar avaliação sem autenticação")
    void testDeleteReview_Unauthorized() throws Exception {

        Long reviewId = createReview(productId, 5, "Avaliação", authToken);

        mockMvc.perform(delete("/api/reviews/" + reviewId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Deve obter média de avaliações do produto")
    void testGetProductRatingAverage() throws Exception {

        createReview(productId, 5, "Excelente!", authToken);
        createReview(productId, 4, "Muito bom!", authTokenUser2);

        mockMvc.perform(get("/api/products/" + productId + "/reviews/average"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(productId))
                .andExpect(jsonPath("$.averageRating").value(4.5))
                .andExpect(jsonPath("$.totalReviews").value(2));
    }

    @Test
    @DisplayName("Deve retornar 0 quando produto não tem avaliações")
    void testGetProductRatingAverage_NoReviews() throws Exception {

        mockMvc.perform(get("/api/products/" + productId + "/reviews/average"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(productId))
                .andExpect(jsonPath("$.averageRating").value(0.0))
                .andExpect(jsonPath("$.totalReviews").value(0));
    }

    @Test
    @DisplayName("Deve filtrar avaliações por rating")
    void testGetReviewsByProductAndRating() throws Exception {

        createReview(productId, 5, "5 estrelas!", authToken);
        createReview(productId, 5, "Também 5!", authTokenUser2);
        createReview(productId, 4, "4 estrelas", authToken);

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
                .andExpect(jsonPath("$.message").value("Usuário já avaliou este produto"));
    }

    @Test
    @DisplayName("Deve listar avaliações do usuário autenticado")
    void testGetMyReviews() throws Exception {

        // Criar produto 2
        Category category = categoryRepository.findAll().get(0);
        Product product2 = new Product();
        product2.setName("Teclado Mecânico");
        product2.setPrice(new BigDecimal("300.00"));
        product2.setStockQuantity(10);
        product2.setSku("TECLADO-001");
        product2.setActive(true);
        product2.setCategory(category);
        product2 = productRepository.save(product2);

        createReview(productId, 5, "Avaliação 1", authToken);
        createReview(product2.getId(), 4, "Avaliação 2", authToken);
        createReview(productId, 3, "Outra pessoa", authTokenUser2);

        mockMvc.perform(get("/api/reviews/my-reviews")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[*].userName", everyItem(is("João Silva"))));
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
}