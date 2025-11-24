package com.hammer.ecommerce.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hammer.ecommerce.dto.category.CategoryRequestDTO;
import com.hammer.ecommerce.model.Category;
import com.hammer.ecommerce.repositories.CategoryRepository;
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

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CategoryControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CategoryRepository categoryRepository;

    @BeforeEach
    void setUp() {
        categoryRepository.deleteAll();
    }

    @Test
    @DisplayName("Deve listar categorias sem autenticação")
    void testListCategories_Public() throws Exception {

        // Criar categorias
        Category cat1 = new Category();
        cat1.setName("Eletrônicos");
        cat1.setDescription("Produtos eletrônicos");
        categoryRepository.save(cat1);

        Category cat2 = new Category();
        cat2.setName("Livros");
        cat2.setDescription("Livros e publicações");
        categoryRepository.save(cat2);

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name").value("Eletrônicos"))
                .andExpect(jsonPath("$[1].name").value("Livros"));
    }

    @Test
    @DisplayName("Deve buscar categoria por ID")
    void testGetCategoryById() throws Exception {

        Category category = new Category();
        category.setName("Eletrônicos");
        category.setDescription("Produtos eletrônicos");
        category = categoryRepository.save(category);

        mockMvc.perform(get("/api/categories/" + category.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Eletrônicos"))
                .andExpect(jsonPath("$.description").value("Produtos eletrônicos"));
    }

    @Test
    @DisplayName("Deve retornar 404 ao buscar categoria inexistente")
    void testGetCategoryById_NotFound() throws Exception {

        mockMvc.perform(get("/api/categories/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Categoria não encontrada com ID: 999"));
    }

    @Test
    @DisplayName("Deve criar categoria como ADMIN")
    @WithMockUser(roles = "ADMIN")
    void testCreateCategory_AsAdmin() throws Exception {

        CategoryRequestDTO request = new CategoryRequestDTO();
        request.setName("Eletrônicos");
        request.setDescription("Produtos eletrônicos");

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Eletrônicos"))
                .andExpect(jsonPath("$.description").value("Produtos eletrônicos"))
                .andExpect(jsonPath("$.productCount").value(0));
    }

    @Test
    @DisplayName("Deve retornar 401 ao criar categoria sem autenticação")
    void testCreateCategory_Unauthorized() throws Exception {

        CategoryRequestDTO request = new CategoryRequestDTO();
        request.setName("Eletrônicos");
        request.setDescription("Produtos eletrônicos");

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Deve retornar 403 ao criar categoria como CUSTOMER")
    @WithMockUser(roles = "CUSTOMER")
    void testCreateCategory_Forbidden() throws Exception {

        CategoryRequestDTO request = new CategoryRequestDTO();
        request.setName("Eletrônicos");
        request.setDescription("Produtos eletrônicos");

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Deve retornar 400 ao criar categoria com nome duplicado")
    @WithMockUser(roles = "ADMIN")
    void testCreateCategory_DuplicateName() throws Exception {

        // Criar primeira categoria
        Category existing = new Category();
        existing.setName("Eletrônicos");
        existing.setDescription("Produtos eletrônicos");
        categoryRepository.save(existing);

        // Tentar criar duplicata
        CategoryRequestDTO request = new CategoryRequestDTO();
        request.setName("Eletrônicos");
        request.setDescription("Outra descrição");

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Já existe uma categoria com o nome: Eletrônicos"));
    }

    @Test
    @DisplayName("Deve atualizar categoria como ADMIN")
    @WithMockUser(roles = "ADMIN")
    void testUpdateCategory() throws Exception {

        Category category = new Category();
        category.setName("Eletrônicos");
        category.setDescription("Produtos eletrônicos");
        category = categoryRepository.save(category);

        CategoryRequestDTO request = new CategoryRequestDTO();
        request.setName("Eletrônicos e Informática");
        request.setDescription("Produtos de tecnologia");

        mockMvc.perform(put("/api/categories/" + category.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Eletrônicos e Informática"))
                .andExpect(jsonPath("$.description").value("Produtos de tecnologia"));
    }

    @Test
    @DisplayName("Deve deletar categoria como ADMIN")
    @WithMockUser(roles = "ADMIN")
    void testDeleteCategory() throws Exception {

        Category category = new Category();
        category.setName("Eletrônicos");
        category.setDescription("Produtos eletrônicos");
        category = categoryRepository.save(category);

        mockMvc.perform(delete("/api/categories/" + category.getId()))
                .andExpect(status().isNoContent());

        // Verificar se foi deletada
        mockMvc.perform(get("/api/categories/" + category.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deve retornar 400 ao criar categoria com dados inválidos")
    @WithMockUser(roles = "ADMIN")
    void testCreateCategory_InvalidData() throws Exception {

        CategoryRequestDTO request = new CategoryRequestDTO();
        request.setName("AB"); // Nome muito curto
        request.setDescription("Descrição");

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());
    }
}