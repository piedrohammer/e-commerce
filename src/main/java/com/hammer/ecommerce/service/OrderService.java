package com.hammer.ecommerce.service;

import com.hammer.ecommerce.dto.*;
import com.hammer.ecommerce.exceptions.BusinessException;
import com.hammer.ecommerce.exceptions.ResourceNotFoundException;
import com.hammer.ecommerce.model.*;
import com.hammer.ecommerce.repositories.*;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final AddressRepository addressRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    @Transactional
    public OrderResponseDTO createOrder(Long userId, CreateOrderRequestDTO request) {

        // Buscar usuário
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        // Buscar carrinho do usuário
        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new BusinessException("Carrinho não encontrado"));

        // Validar carrinho não vazio
        if (cart.getItems().isEmpty()) {
            throw new BusinessException("Carrinho está vazio");
        }

        // Buscar e validar endereço de entrega
        Address shippingAddress = addressRepository.findByIdAndUserId(request.getShippingAddressId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Endereço de entrega não encontrado"));

        // Criar pedido
        Order order = new Order();
        order.setUser(user);
        order.setShippingAddress(shippingAddress);
        order.setStatus(OrderStatus.PENDING);

        // Processar itens do carrinho
        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();

            // Validar produto ativo
            if (!product.getActive()) {
                throw new BusinessException("Produto " + product.getName() + " não está mais disponível");
            }

            // Validar estoque
            if (product.getStockQuantity() < cartItem.getQuantity()) {
                throw new BusinessException("Estoque insuficiente para o produto: " + product.getName() +
                        ". Disponível: " + product.getStockQuantity());
            }

            // Criar item do pedido
            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(product);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPrice(product.getPrice()); // Preço atual do produto
            orderItem.calculateSubtotal();

            order.addOrderItem(orderItem);

            // Diminuir estoque
            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
            productRepository.save(product);
        }

        // Calcular total
        order.setTotalAmount(order.calculateTotal());

        // Salvar pedido
        order = orderRepository.save(order);

        // Limpar carrinho
        cart.getItems().clear();
        cartRepository.save(cart);

        return convertToDTO(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderSummaryDTO> findAllByUser(Long userId, Pageable pageable) {
        return orderRepository.findByUserId(userId, pageable)
                .map(this::convertToSummaryDTO);
    }

    @Transactional(readOnly = true)
    public OrderResponseDTO findById(Long orderId, Long userId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado"));
        return convertToDTO(order);
    }

    @Transactional
    public OrderResponseDTO cancelOrder(Long orderId, Long userId) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado"));

        // Verificar se o pedido pertence ao usuário
        if (!order.getUser().getId().equals(userId)) {
            throw new BusinessException("Pedido não pertence ao usuário");
        }

        // Validar se pode cancelar
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessException("Pedido já está cancelado");
        }

        if (order.getStatus() == OrderStatus.DELIVERED) {
            throw new BusinessException("Não é possível cancelar pedido já entregue");
        }

        if (order.getStatus() == OrderStatus.SHIPPED) {
            throw new BusinessException("Não é possível cancelar pedido já enviado");
        }

        // Devolver estoque
        for (OrderItem item : order.getOrderItems()) {
            Product product = item.getProduct();
            product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
            productRepository.save(product);
        }

        // Cancelar pedido
        order.setStatus(OrderStatus.CANCELLED);
        order = orderRepository.save(order);

        return convertToDTO(order);
    }

    // -------- Métodos para ADMIN --------

    @Transactional(readOnly = true)
    public Page<OrderSummaryDTO> findAll(Pageable pageable) {
        return orderRepository.findAll(pageable)
                .map(this::convertToSummaryDTO);
    }

    @Transactional
    public OrderResponseDTO updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado"));

        // Validações de transição de status
        validateStatusTransition(order.getStatus(), newStatus);

        order.setStatus(newStatus);
        order = orderRepository.save(order);

        return convertToDTO(order);
    }

    private void validateStatusTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        // Não pode mudar de CANCELLED
        if (currentStatus == OrderStatus.CANCELLED) {
            throw new BusinessException("Não é possível alterar status de pedido cancelado");
        }

        // Não pode mudar de DELIVERED
        if (currentStatus == OrderStatus.DELIVERED) {
            throw new BusinessException("Não é possível alterar status de pedido entregue");
        }

        // PENDING só pode ir para PAID ou CANCELLED
        if (currentStatus == OrderStatus.PENDING &&
                newStatus != OrderStatus.PAID &&
                newStatus != OrderStatus.CANCELLED) {
            throw new BusinessException("Status inválido para pedido pendente");
        }
    }

    private OrderResponseDTO convertToDTO(Order order) {
        OrderResponseDTO dto = new OrderResponseDTO();
        dto.setId(order.getId());
        dto.setOrderNumber(order.getOrderNumber());
        dto.setStatus(order.getStatus().name());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setUpdatedAt(order.getUpdatedAt());

        // Endereço
        dto.setShippingAddress(modelMapper.map(order.getShippingAddress(), AddressResponseDTO.class));

        // Itens
        dto.setItems(order.getOrderItems().stream()
                .map(this::convertItemToDTO)
                .collect(Collectors.toList()));

        return dto;
    }

    private OrderSummaryDTO convertToSummaryDTO(Order order) {
        OrderSummaryDTO dto = new OrderSummaryDTO();
        dto.setId(order.getId());
        dto.setOrderNumber(order.getOrderNumber());
        dto.setStatus(order.getStatus().name());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setItemCount(order.getOrderItems().size());
        dto.setCreatedAt(order.getCreatedAt());
        return dto;
    }

    private OrderItemResponseDTO convertItemToDTO(OrderItem item) {
        OrderItemResponseDTO dto = new OrderItemResponseDTO();
        dto.setId(item.getId());
        dto.setProductId(item.getProduct().getId());
        dto.setProductName(item.getProduct().getName());
        dto.setProductImageUrl(item.getProduct().getImageUrl());
        dto.setQuantity(item.getQuantity());
        dto.setPrice(item.getPrice());
        dto.setSubtotal(item.getSubtotal());
        return dto;
    }
}