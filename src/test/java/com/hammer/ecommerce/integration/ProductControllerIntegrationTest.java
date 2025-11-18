package com.hammer.ecommerce.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hammer.ecommerce.dto.product.ProductRequestDTO;
import com.hammer.ecommerce.model.Category;
import com.hammer.ecommerce.model.Product;
import com.hammer.ecommerce.repositories.CategoryRepository;
import com.hammer.ecommerce.repositories.ProductRepository;
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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProductControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    private Category category;

    @BeforeEach
    void setUp() {

        productRepository.deleteAll();
        categoryRepository.deleteAll();

        // Criar categoria para testes
        category = new Category();
        category.setName("Eletrônicos Test");
        category.setDescription("Categoria de teste");
        category = categoryRepository.save(category);
    }

    @Test
    @DisplayName("Deve listar produtos sem autenticação")
    void testListProducts_Public() throws Exception {

        // Criar produtos
        createProduct("Mouse", new BigDecimal("50.00"), 10);
        createProduct("Teclado", new BigDecimal("100.00"), 5);

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(2))));
    }

    @Test
    @DisplayName("Deve filtrar produtos por categoria")
    void testFilterProductsByCategory() throws Exception {

        createProduct("Mouse", new BigDecimal("50.00"), 10);

        mockMvc.perform(get("/api/products")
                        .param("categoryId", category.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("Deve buscar produtos por nome")
    void testSearchProductsByName() throws Exception {

        createProduct("Mouse Gamer", new BigDecimal("50.00"), 10);
        createProduct("Teclado Mecânico", new BigDecimal("100.00"), 5);

        mockMvc.perform(get("/api/products")
                        .param("search", "mouse"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Mouse Gamer"));
    }

    @Test
    @DisplayName("Deve filtrar produtos por faixa de preço")
    void testFilterProductsByPriceRange() throws Exception {

        createProduct("Barato", new BigDecimal("50.00"), 10);
        createProduct("Caro", new BigDecimal("500.00"), 5);

        mockMvc.perform(get("/api/products")
                        .param("minPrice", "40")
                        .param("maxPrice", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Barato"));
    }

    @Test
    @DisplayName("Deve criar produto como ADMIN")
    @WithMockUser(roles = "ADMIN")
    void testCreateProduct_AsAdmin() throws Exception {

        ProductRequestDTO request = new ProductRequestDTO();
        request.setName("Notebook Test");
        request.setDescription("Notebook para testes");
        request.setPrice(new BigDecimal("3000.00"));
        request.setStockQuantity(10);
        request.setSku("NB-TEST-001");
        request.setCategoryId(category.getId());
        request.setActive(true);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Notebook Test"))
                .andExpect(jsonPath("$.price").value(3000.00))
                .andExpect(jsonPath("$.sku").value("NB-TEST-001"));
    }

    @Test
    @DisplayName("Deve retornar 401 ao criar produto sem autenticação")
    void testCreateProduct_Unauthorized() throws Exception {

        ProductRequestDTO request = new ProductRequestDTO();
        request.setName("Teste");
        request.setPrice(new BigDecimal("100.00"));
        request.setStockQuantity(10);
        request.setSku("TEST-001");
        request.setCategoryId(category.getId());

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Deve retornar 400 ao criar produto com dados inválidos")
    @WithMockUser(roles = "ADMIN")
    void testCreateProduct_InvalidData() throws Exception {

        ProductRequestDTO request = new ProductRequestDTO();
        request.setName(""); // Nome vazio
        request.setPrice(new BigDecimal("-10.00")); // Preço negativo
        request.setStockQuantity(-5); // Quantidade negativa
        request.setSku("TEST-002");
        request.setCategoryId(category.getId());

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").exists());
    }

    @Test
    @DisplayName("Deve buscar produto por ID")
    void testGetProductById() throws Exception {

        Product product = createProduct("Mouse Test", new BigDecimal("50.00"), 10);

        mockMvc.perform(get("/api/products/" + product.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Mouse Test"))
                .andExpect(jsonPath("$.price").value(50.00));
    }

    @Test
    @DisplayName("Deve retornar 404 ao buscar produto inexistente")
    void testGetProductById_NotFound() throws Exception {
        mockMvc.perform(get("/api/products/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deve atualizar produto como ADMIN")
    @WithMockUser(roles = "ADMIN")
    void testUpdateProduct() throws Exception {

        Product product = createProduct("Mouse Original", new BigDecimal("50.00"), 10);

        ProductRequestDTO request = new ProductRequestDTO();
        request.setName("Mouse Atualizado");
        request.setDescription("Descrição atualizada");
        request.setPrice(new BigDecimal("45.00"));
        request.setStockQuantity(15);
        request.setSku(product.getSku());
        request.setCategoryId(category.getId());
        request.setActive(true);

        mockMvc.perform(put("/api/products/" + product.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Mouse Atualizado"))
                .andExpect(jsonPath("$.price").value(45.00));
    }

    @Test
    @DisplayName("Deve deletar produto (soft delete) como ADMIN")
    @WithMockUser(roles = "ADMIN")
    void testDeleteProduct() throws Exception {

        Product product = createProduct("Mouse Delete", new BigDecimal("50.00"), 10);

        mockMvc.perform(delete("/api/products/" + product.getId()))
                .andExpect(status().isNoContent());

        // Produto deve estar inativo, não aparecer na listagem pública
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == " + product.getId() + ")]").doesNotExist());
    }

    @Test
    @DisplayName("Deve atualizar estoque como ADMIN")
    @WithMockUser(roles = "ADMIN")
    void testUpdateStock() throws Exception {

        Product product = createProduct("Mouse Stock", new BigDecimal("50.00"), 10);

        mockMvc.perform(patch("/api/products/" + product.getId() + "/stock")
                        .param("quantity", "5"))
                .andExpect(status().isOk());

        // Verificar estoque atualizado
        mockMvc.perform(get("/api/products/" + product.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockQuantity").value(15));
    }

    @Test
    @DisplayName("Deve ordenar produtos por preço")
    void testSortProductsByPrice() throws Exception {

        createProduct("Barato", new BigDecimal("10.00"), 10);
        createProduct("Médio", new BigDecimal("50.00"), 10);
        createProduct("Caro", new BigDecimal("100.00"), 10);

        mockMvc.perform(get("/api/products")
                        .param("sortBy", "price")
                        .param("direction", "ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Barato"))
                .andExpect(jsonPath("$.content[2].name").value("Caro"));
    }

    @Test
    @DisplayName("Deve paginar produtos corretamente")
    void testPaginateProducts() throws Exception {

        // Criar vários produtos
        for (int i = 1; i <= 15; i++) {
            createProduct("Produto " + i, new BigDecimal("10.00"), 10);
        }

        mockMvc.perform(get("/api/products")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(5)))
                .andExpect(jsonPath("$.totalElements").value(greaterThanOrEqualTo(15)));
    }

    private Product createProduct(String name, BigDecimal price, int stock) {

        Product product = new Product();
        product.setName(name);
        product.setDescription("Descrição de " + name);
        product.setPrice(price);
        product.setStockQuantity(stock);
        product.setSku("SKU-" + System.currentTimeMillis());
        product.setActive(true);
        product.setCategory(category);
        return productRepository.save(product);
    }
}