package com.hammer.ecommerce;

import com.hammer.ecommerce.dto.category.CategoryRequestDTO;
import com.hammer.ecommerce.dto.category.CategoryResponseDTO;
import com.hammer.ecommerce.exceptions.BusinessException;
import com.hammer.ecommerce.exceptions.ResourceNotFoundException;
import com.hammer.ecommerce.model.Category;
import com.hammer.ecommerce.repositories.CategoryRepository;
import com.hammer.ecommerce.service.CategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private CategoryService categoryService;

    private Category category;
    private CategoryRequestDTO categoryRequest;
    private CategoryResponseDTO categoryResponse;

    @BeforeEach
    void setUp() {

        category = new Category();
        category.setId(1L);
        category.setName("Eletrônicos");
        category.setDescription("Produtos eletrônicos");

        categoryRequest = new CategoryRequestDTO();
        categoryRequest.setName("Eletrônicos");
        categoryRequest.setDescription("Produtos eletrônicos");

        categoryResponse = new CategoryResponseDTO();
        categoryResponse.setId(1L);
        categoryResponse.setName("Eletrônicos");
        categoryResponse.setDescription("Produtos eletrônicos");
        categoryResponse.setProductCount(0);
    }

    @Test
    @DisplayName("Deve listar todas as categorias")
    void testFindAll_Success() {

        // Arrange
        List<Category> categories = Arrays.asList(category);
        when(categoryRepository.findAll()).thenReturn(categories);
        when(modelMapper.map(category, CategoryResponseDTO.class)).thenReturn(categoryResponse);

        // Act
        List<CategoryResponseDTO> result = categoryService.findAll();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Eletrônicos", result.get(0).getName());
        verify(categoryRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Deve buscar categoria por ID com sucesso")
    void testFindById_Success() {

        // Arrange
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(modelMapper.map(category, CategoryResponseDTO.class)).thenReturn(categoryResponse);

        // Act
        CategoryResponseDTO result = categoryService.findById(1L);

        // Assert
        assertNotNull(result);
        assertEquals("Eletrônicos", result.getName());
        verify(categoryRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Deve lançar exceção ao buscar categoria inexistente")
    void testFindById_NotFound() {

        // Arrange
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            categoryService.findById(999L);
        });

        assertEquals("Categoria não encontrada com ID: 999", exception.getMessage());
        verify(categoryRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("Deve criar categoria com sucesso")
    void testCreate_Success() {

        // Arrange
        when(categoryRepository.existsByName(categoryRequest.getName())).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(category);
        when(modelMapper.map(category, CategoryResponseDTO.class)).thenReturn(categoryResponse);

        // Act
        CategoryResponseDTO result = categoryService.create(categoryRequest);

        // Assert
        assertNotNull(result);
        assertEquals("Eletrônicos", result.getName());
        verify(categoryRepository, times(1)).existsByName(categoryRequest.getName());
        verify(categoryRepository, times(1)).save(any(Category.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao criar categoria com nome duplicado")
    void testCreate_DuplicateName() {

        // Arrange
        when(categoryRepository.existsByName(categoryRequest.getName())).thenReturn(true);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            categoryService.create(categoryRequest);
        });

        assertEquals("Já existe uma categoria com o nome: Eletrônicos", exception.getMessage());
        verify(categoryRepository, times(1)).existsByName(categoryRequest.getName());
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    @DisplayName("Deve atualizar categoria com sucesso")
    void testUpdate_Success() {

        // Arrange
        CategoryRequestDTO updateRequest = new CategoryRequestDTO();
        updateRequest.setName("Eletrônicos e Informática");
        updateRequest.setDescription("Produtos de tecnologia");

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.existsByName(updateRequest.getName())).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(category);
        when(modelMapper.map(category, CategoryResponseDTO.class)).thenReturn(categoryResponse);

        // Act
        CategoryResponseDTO result = categoryService.update(1L, updateRequest);

        // Assert
        assertNotNull(result);
        verify(categoryRepository, times(1)).findById(1L);
        verify(categoryRepository, times(1)).save(any(Category.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao atualizar com nome duplicado")
    void testUpdate_DuplicateName() {

        // Arrange
        CategoryRequestDTO updateRequest = new CategoryRequestDTO();
        updateRequest.setName("Outro Nome");
        updateRequest.setDescription("Nova descrição");

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.existsByName(updateRequest.getName())).thenReturn(true);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            categoryService.update(1L, updateRequest);
        });

        assertEquals("Já existe uma categoria com o nome: Outro Nome", exception.getMessage());
        verify(categoryRepository, times(1)).findById(1L);
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    @DisplayName("Deve deletar categoria sem produtos")
    void testDelete_Success() {

        // Arrange
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        // Act
        categoryService.delete(1L);

        // Assert
        verify(categoryRepository, times(1)).findById(1L);
        verify(categoryRepository, times(1)).delete(category);
    }

    @Test
    @DisplayName("Deve lançar exceção ao deletar categoria com produtos")
    void testDelete_WithProducts() {

        // Arrange
        category.getProducts().add(new com.hammer.ecommerce.model.Product());
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            categoryService.delete(1L);
        });

        assertEquals("Não é possível deletar categoria com produtos associados", exception.getMessage());
        verify(categoryRepository, times(1)).findById(1L);
        verify(categoryRepository, never()).delete(any(Category.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao deletar categoria inexistente")
    void testDelete_NotFound() {

        // Arrange
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            categoryService.delete(999L);
        });

        assertEquals("Categoria não encontrada com ID: 999", exception.getMessage());
        verify(categoryRepository, times(1)).findById(999L);
        verify(categoryRepository, never()).delete(any(Category.class));
    }
}