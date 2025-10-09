package com.hammer.ecommerce.service;

import com.hammer.ecommerce.dto.CategoryRequestDTO;
import com.hammer.ecommerce.dto.CategoryResponseDTO;
import com.hammer.ecommerce.exceptions.BusinessException;
import com.hammer.ecommerce.exceptions.ResourceNotFoundException;
import com.hammer.ecommerce.model.Category;
import com.hammer.ecommerce.repositories.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ModelMapper modelMapper;

    @Transactional(readOnly = true)
    public List<CategoryResponseDTO> findAll() {
        return categoryRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CategoryResponseDTO findById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada com ID: " + id));
        return convertToDTO(category);
    }

    @Transactional
    public CategoryResponseDTO create(CategoryRequestDTO request) {
        // Verificar se já existe categoria com esse nome
        if (categoryRepository.existsByName(request.getName())) {
            throw new BusinessException("Já existe uma categoria com o nome: " + request.getName());
        }

        Category category = new Category();
        category.setName(request.getName());
        category.setDescription(request.getDescription());

        category = categoryRepository.save(category);
        return convertToDTO(category);
    }

    @Transactional
    public CategoryResponseDTO update(Long id, CategoryRequestDTO request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada com ID: " + id));

        // Verificar se o novo nome já está em uso por outra categoria
        if (!category.getName().equals(request.getName()) &&
                categoryRepository.existsByName(request.getName())) {
            throw new BusinessException("Já existe uma categoria com o nome: " + request.getName());
        }

        category.setName(request.getName());
        category.setDescription(request.getDescription());

        category = categoryRepository.save(category);
        return convertToDTO(category);
    }

    @Transactional
    public void delete(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada com ID: " + id));

        // Verificar se existem produtos nesta categoria
        if (!category.getProducts().isEmpty()) {
            throw new BusinessException("Não é possível deletar categoria com produtos associados");
        }

        categoryRepository.delete(category);
    }

    private CategoryResponseDTO convertToDTO(Category category) {
        CategoryResponseDTO dto = modelMapper.map(category, CategoryResponseDTO.class);
        dto.setProductCount(category.getProducts().size());
        return dto;
    }
}