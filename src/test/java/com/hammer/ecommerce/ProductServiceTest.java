package com.hammer.ecommerce;

import com.hammer.ecommerce.dto.product.ProductRequestDTO;
import com.hammer.ecommerce.dto.product.ProductResponseDTO;
import com.hammer.ecommerce.dto.product.ProductUpdateDTO;
import com.hammer.ecommerce.exceptions.BusinessException;
import com.hammer.ecommerce.exceptions.ResourceNotFoundException;
import com.hammer.ecommerce.model.Category;
import com.hammer.ecommerce.model.Product;
import com.hammer.ecommerce.repositories.CategoryRepository;
import com.hammer.ecommerce.repositories.ProductRepository;
import com.hammer.ecommerce.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductService productService;

    private Category category;
    private Product product;
    private ProductRequestDTO productRequest;
    private ProductUpdateDTO productUpdate;

    @BeforeEach
    void setUp() {

        // Setup Category
        category = new Category();
        category.setId(1L);
        category.setName("Eletrônicos");
        category.setDescription("Produtos eletrônicos");

        // Setup Product
        product = new Product();
        product.setId(1L);
        product.setName("Notebook");
        product.setDescription("Notebook Dell");
        product.setPrice(new BigDecimal("3000.00"));
        product.setStockQuantity(10);
        product.setSku("DELL-001");
        product.setActive(true);
        product.setCategory(category);

        // Setup Request DTO
        productRequest = new ProductRequestDTO();
        productRequest.setName("Notebook");
        productRequest.setDescription("Notebook Dell");
        productRequest.setPrice(new BigDecimal("3000.00"));
        productRequest.setStockQuantity(10);
        productRequest.setSku("DELL-001");
        productRequest.setCategoryId(1L);
        productRequest.setActive(true);

        // Setup Update DTO
        productUpdate = new ProductUpdateDTO();
        productUpdate.setName("Notebook Atualizado");
        productUpdate.setDescription("Notebook Dell Atualizado");
        productUpdate.setPrice(new BigDecimal("2800.00"));
        productUpdate.setStockQuantity(15);
        productUpdate.setCategoryId(1L);
        productUpdate.setActive(true);
    }

    @Test
    @DisplayName("Deve listar produtos com sucesso")
    void testFindAll_Success() {

        // Arrange
        Page<Product> productPage = new PageImpl<>(Arrays.asList(product));
        when(productRepository.findByActiveTrue(any(Pageable.class))).thenReturn(productPage);

        // Act
        Page<ProductResponseDTO> result = productService.findAll(Pageable.unpaged());

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Notebook", result.getContent().get(0).getName());
        verify(productRepository, times(1)).findByActiveTrue(any(Pageable.class));
    }

    @Test
    @DisplayName("Deve buscar produtos com filtros")
    void testFindWithFilters_Success() {

        // Arrange
        Page<Product> productPage = new PageImpl<>(Arrays.asList(product));
        when(productRepository.findWithFilters(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(productPage);

        // Act
        Page<ProductResponseDTO> result = productService.findWithFilters(
                1L, "notebook", new BigDecimal("1000"), new BigDecimal("5000"), Pageable.unpaged());

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(productRepository, times(1)).findWithFilters(any(), any(), any(), any(), any(Pageable.class));
    }

    @Test
    @DisplayName("Deve buscar produto por ID com sucesso")
    void testFindById_Success() {

        // Arrange
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        // Act
        ProductResponseDTO result = productService.findById(1L);

        // Assert
        assertNotNull(result);
        assertEquals("Notebook", result.getName());
        assertEquals(new BigDecimal("3000.00"), result.getPrice());
        verify(productRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Deve lançar exceção ao buscar produto inexistente")
    void testFindById_NotFound() {

        // Arrange
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            productService.findById(999L);
        });
        verify(productRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("Deve criar produto com sucesso")
    void testCreate_Success() {

        // Arrange
        when(productRepository.existsBySku(productRequest.getSku())).thenReturn(false);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        // Act
        ProductResponseDTO result = productService.create(productRequest);

        // Assert
        assertNotNull(result);
        assertEquals("Notebook", result.getName());
        verify(productRepository, times(1)).existsBySku(productRequest.getSku());
        verify(categoryRepository, times(1)).findById(1L);
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao criar produto com SKU duplicado")
    void testCreate_DuplicateSku() {

        // Arrange
        when(productRepository.existsBySku(productRequest.getSku())).thenReturn(true);

        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            productService.create(productRequest);
        });
        verify(productRepository, times(1)).existsBySku(productRequest.getSku());
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao criar produto com categoria inexistente")
    void testCreate_CategoryNotFound() {

        // Arrange
        when(productRepository.existsBySku(productRequest.getSku())).thenReturn(false);
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            productService.create(productRequest);
        });
        verify(categoryRepository, times(1)).findById(1L);
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("Deve atualizar produto com sucesso")
    void testUpdate_Success() {

        // Arrange
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        // Act
        ProductResponseDTO result = productService.update(1L, productUpdate);

        // Assert
        assertNotNull(result);
        verify(productRepository, times(1)).findById(1L);
        verify(categoryRepository, times(1)).findById(1L);
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    @DisplayName("Deve atualizar estoque adicionando quantidade")
    void testUpdateStock_AddQuantity() {

        // Arrange
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        // Act
        productService.updateStock(1L, 5);

        // Assert
        assertEquals(15, product.getStockQuantity());
        verify(productRepository, times(1)).findById(1L);
        verify(productRepository, times(1)).save(product);
    }

    @Test
    @DisplayName("Deve atualizar estoque removendo quantidade")
    void testUpdateStock_RemoveQuantity() {

        // Arrange
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        // Act
        productService.updateStock(1L, -3);

        // Assert
        assertEquals(7, product.getStockQuantity());
        verify(productRepository, times(1)).findById(1L);
        verify(productRepository, times(1)).save(product);
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar estoque negativo")
    void testUpdateStock_InsufficientStock() {

        // Arrange
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            productService.updateStock(1L, -20);
        });
        verify(productRepository, times(1)).findById(1L);
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("Deve fazer soft delete do produto")
    void testDelete_Success() {

        // Arrange
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        // Act
        productService.delete(1L);

        // Assert
        assertFalse(product.getActive());
        verify(productRepository, times(1)).findById(1L);
        verify(productRepository, times(1)).save(product);
    }

    @Test
    @DisplayName("Deve lançar exceção ao deletar produto inexistente")
    void testDelete_NotFound() {

        // Arrange
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            productService.delete(999L);
        });
        verify(productRepository, times(1)).findById(999L);
        verify(productRepository, never()).save(any(Product.class));
    }
}