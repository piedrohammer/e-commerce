package com.hammer.ecommerce.service;

import com.hammer.ecommerce.dto.CategorySummaryDTO;
import com.hammer.ecommerce.dto.ProductRequestDTO;
import com.hammer.ecommerce.dto.ProductResponseDTO;
import com.hammer.ecommerce.dto.ProductUpdateDTO;
import com.hammer.ecommerce.exceptions.BusinessException;
import com.hammer.ecommerce.exceptions.ResourceNotFoundException;
import com.hammer.ecommerce.model.Category;
import com.hammer.ecommerce.model.Product;
import com.hammer.ecommerce.repositories.CategoryRepository;
import com.hammer.ecommerce.repositories.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public Page<ProductResponseDTO> findAll(Pageable pageable) {
        return productRepository.findByActiveTrue(pageable)
                .map(this::convertToDTO);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponseDTO> findWithFilters(
            Long categoryId,
            String search,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable) {

        return productRepository.findWithFilters(categoryId, search, minPrice, maxPrice, pageable)
                .map(this::convertToDTO);
    }

    @Transactional(readOnly = true)
    public ProductResponseDTO findById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado com ID: " + id));
        return convertToDTO(product);
    }

    @Transactional
    public ProductResponseDTO create(ProductRequestDTO request) {
        // Verificar se SKU já existe
        if (productRepository.existsBySku(request.getSku())) {
            throw new BusinessException("Já existe um produto com o SKU: " + request.getSku());
        }

        // Buscar categoria
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada com ID: " + request.getCategoryId()));

        // Criar produto
        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setImageUrl(request.getImageUrl());
        product.setSku(request.getSku());
        product.setActive(request.getActive());
        product.setCategory(category);

        product = productRepository.save(product);
        return convertToDTO(product);
    }

    @Transactional
    public ProductResponseDTO update(Long id, ProductUpdateDTO request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado com ID: " + id));

        // Buscar categoria
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada com ID: " + request.getCategoryId()));

        // Atualizar dados
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setImageUrl(request.getImageUrl());
        product.setActive(request.getActive());
        product.setCategory(category);

        product = productRepository.save(product);
        return convertToDTO(product);
    }

    @Transactional
    public void delete(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado com ID: " + id));

        // Soft delete - apenas marca como inativo
        product.setActive(false);
        productRepository.save(product);
    }

    @Transactional
    public void updateStock(Long id, Integer quantity) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado com ID: " + id));

        if (product.getStockQuantity() + quantity < 0) {
            throw new BusinessException("Estoque insuficiente");
        }

        product.setStockQuantity(product.getStockQuantity() + quantity);
        productRepository.save(product);
    }

    private ProductResponseDTO convertToDTO(Product product) {
        ProductResponseDTO dto = new ProductResponseDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice());
        dto.setStockQuantity(product.getStockQuantity());
        dto.setImageUrl(product.getImageUrl());
        dto.setSku(product.getSku());
        dto.setActive(product.getActive());
        dto.setCreatedAt(product.getCreatedAt());
        dto.setUpdatedAt(product.getUpdatedAt());

        // Category summary
        CategorySummaryDTO categoryDTO = new CategorySummaryDTO();
        categoryDTO.setId(product.getCategory().getId());
        categoryDTO.setName(product.getCategory().getName());
        dto.setCategory(categoryDTO);

        // Reviews (vamos calcular depois quando criar a entidade Review)
        //dto.setReviewCount(product.getReviews().size());
        //dto.setAverageRating(calculateAverageRating(product));

        return dto;
    }

    /*private Double calculateAverageRating(Product product) {
        if (product.getReviews().isEmpty()) {
            return 0.0;
        }
        // Vamos implementar isso corretamente quando criar a entidade Review
        return 0.0;
    }*/
}