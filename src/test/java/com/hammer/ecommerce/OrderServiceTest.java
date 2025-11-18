package com.hammer.ecommerce;

import com.hammer.ecommerce.dto.order.CreateOrderRequestDTO;
import com.hammer.ecommerce.dto.order.OrderResponseDTO;
import com.hammer.ecommerce.dto.order.OrderSummaryDTO;
import com.hammer.ecommerce.exceptions.BusinessException;
import com.hammer.ecommerce.exceptions.ResourceNotFoundException;
import com.hammer.ecommerce.model.*;
import com.hammer.ecommerce.repositories.*;
import com.hammer.ecommerce.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
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
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private OrderService orderService;

    private User user;
    private Cart cart;
    private CartItem cartItem;
    private Product product;
    private Address address;
    private Order order;
    private CreateOrderRequestDTO createOrderRequest;

    @BeforeEach
    void setUp() {

        user = new User();
        user.setId(1L);
        user.setName("João Silva");

        Category category = new Category();
        category.setId(1L);

        product = new Product();
        product.setId(1L);
        product.setName("Mouse Gamer");
        product.setPrice(new BigDecimal("150.00"));
        product.setStockQuantity(20);
        product.setActive(true);
        product.setCategory(category);

        cart = new Cart();
        cart.setId(1L);
        cart.setUser(user);

        cartItem = new CartItem();
        cartItem.setId(1L);
        cartItem.setCart(cart);
        cartItem.setProduct(product);
        cartItem.setQuantity(2);

        cart.getItems().add(cartItem);

        address = new Address();
        address.setId(1L);
        address.setStreet("Rua das Flores");
        address.setCity("São Paulo");
        address.setState("SP");
        address.setUser(user);

        order = new Order();
        order.setId(1L);
        order.setOrderNumber("ORD-12345678");
        order.setUser(user);
        order.setShippingAddress(address);
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(new BigDecimal("300.00"));

        createOrderRequest = new CreateOrderRequestDTO();
        createOrderRequest.setShippingAddressId(1L);
    }

    @Test
    @DisplayName("Deve criar pedido com sucesso")
    void testCreateOrder_Success() {

        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.of(cart));
        when(addressRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(address));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(productRepository.save(any(Product.class))).thenReturn(product);
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        // Act
        OrderResponseDTO result = orderService.createOrder(1L, createOrderRequest);

        // Assert
        assertNotNull(result);
        assertEquals(18, product.getStockQuantity()); // 20 - 2
        assertTrue(cart.getItems().isEmpty());
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(productRepository, times(1)).save(product);
        verify(cartRepository, times(1)).save(cart);
    }

    @Test
    @DisplayName("Deve lançar exceção ao criar pedido com carrinho vazio")
    void testCreateOrder_EmptyCart() {

        // Arrange
        cart.getItems().clear();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.of(cart));
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            orderService.createOrder(1L, createOrderRequest);
        });

        assertEquals("Carrinho está vazio", exception.getMessage());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao criar pedido sem carrinho")
    void testCreateOrder_NoCart() {

        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.empty());

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            orderService.createOrder(1L, createOrderRequest);
        });

        assertEquals("Carrinho não encontrado", exception.getMessage());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao criar pedido com endereço inexistente")
    void testCreateOrder_AddressNotFound() {

        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.of(cart));
        when(addressRepository.findByIdAndUserId(999L, 1L)).thenReturn(Optional.empty());

        createOrderRequest.setShippingAddressId(999L);

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            orderService.createOrder(1L, createOrderRequest);
        });
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao criar pedido com produto inativo")
    void testCreateOrder_InactiveProduct() {

        // Arrange
        product.setActive(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.of(cart));
        when(addressRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(address));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            orderService.createOrder(1L, createOrderRequest);
        });

        assertTrue(exception.getMessage().contains("não está mais disponível"));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao criar pedido com estoque insuficiente")
    void testCreateOrder_InsufficientStock() {

        // Arrange
        product.setStockQuantity(1);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.of(cart));
        when(addressRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(address));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            orderService.createOrder(1L, createOrderRequest);
        });

        assertTrue(exception.getMessage().contains("Estoque insuficiente"));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("Deve listar pedidos do usuário")
    void testFindAllByUser_Success() {

        // Arrange
        Page<Order> orderPage = new PageImpl<>(Arrays.asList(order));
        when(orderRepository.findByUserId(1L, Pageable.unpaged())).thenReturn(orderPage);

        // Act
        Page<OrderSummaryDTO> result = orderService.findAllByUser(1L, Pageable.unpaged());

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(orderRepository, times(1)).findByUserId(1L, Pageable.unpaged());
    }

    @Test
    @DisplayName("Deve buscar pedido por ID")
    void testFindById_Success() {

        // Arrange
        when(orderRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(order));

        // Act
        OrderResponseDTO result = orderService.findById(1L, 1L);

        // Assert
        assertNotNull(result);
        verify(orderRepository, times(1)).findByIdAndUserId(1L, 1L);
    }

    @Test
    @DisplayName("Deve lançar exceção ao buscar pedido inexistente")
    void testFindById_NotFound() {

        // Arrange
        when(orderRepository.findByIdAndUserId(999L, 1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            orderService.findById(999L, 1L);
        });
        verify(orderRepository, times(1)).findByIdAndUserId(999L, 1L);
    }

    @Test
    @DisplayName("Deve cancelar pedido com sucesso")
    void testCancelOrder_Success() {

        // Arrange
        OrderItem orderItem = new OrderItem();
        orderItem.setProduct(product);
        orderItem.setQuantity(2);
        order.getOrderItems().add(orderItem);

        when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(order));
        when(productRepository.save(any(Product.class))).thenReturn(product);
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // Act
        OrderResponseDTO result = orderService.cancelOrder(1L, 1L);

        // Assert
        assertNotNull(result);
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertEquals(22, product.getStockQuantity()); // 20 + 2
        verify(orderRepository, times(1)).save(order);
        verify(productRepository, times(1)).save(product);
    }

    @Test
    @DisplayName("Deve lançar exceção ao cancelar pedido de outro usuário")
    void testCancelOrder_WrongUser() {

        // Arrange
        User otherUser = new User();
        otherUser.setId(2L);
        order.setUser(otherUser);

        when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(order));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            orderService.cancelOrder(1L, 1L);
        });

        assertEquals("Pedido não pertence ao usuário", exception.getMessage());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao cancelar pedido já cancelado")
    void testCancelOrder_AlreadyCancelled() {

        // Arrange
        order.setStatus(OrderStatus.CANCELLED);
        when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(order));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            orderService.cancelOrder(1L, 1L);
        });

        assertEquals("Pedido já está cancelado", exception.getMessage());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao cancelar pedido entregue")
    void testCancelOrder_Delivered() {

        // Arrange
        order.setStatus(OrderStatus.DELIVERED);
        when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(order));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            orderService.cancelOrder(1L, 1L);
        });

        assertEquals("Não é possível cancelar pedido já entregue", exception.getMessage());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao cancelar pedido enviado")
    void testCancelOrder_Shipped() {

        // Arrange
        order.setStatus(OrderStatus.SHIPPED);
        when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(order));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            orderService.cancelOrder(1L, 1L);
        });

        assertEquals("Não é possível cancelar pedido já enviado", exception.getMessage());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("Admin deve listar todos os pedidos")
    void testFindAll_Admin() {

        // Arrange
        Page<Order> orderPage = new PageImpl<>(Arrays.asList(order));
        when(orderRepository.findAll(Pageable.unpaged())).thenReturn(orderPage);

        // Act
        Page<OrderSummaryDTO> result = orderService.findAll(Pageable.unpaged());

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(orderRepository, times(1)).findAll(Pageable.unpaged());
    }

    @Test
    @DisplayName("Admin deve atualizar status do pedido")
    void testUpdateOrderStatus_Success() {

        // Arrange
        when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // Act
        OrderResponseDTO result = orderService.updateOrderStatus(1L, OrderStatus.PAID);

        // Assert
        assertNotNull(result);
        assertEquals(OrderStatus.PAID, order.getStatus());
        verify(orderRepository, times(1)).save(order);
    }

    @Test
    @DisplayName("Deve lançar exceção ao alterar status de pedido cancelado")
    void testUpdateOrderStatus_Cancelled() {

        // Arrange
        order.setStatus(OrderStatus.CANCELLED);
        when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(order));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            orderService.updateOrderStatus(1L, OrderStatus.PAID);
        });

        assertEquals("Não é possível alterar status de pedido cancelado", exception.getMessage());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao alterar status de pedido entregue")
    void testUpdateOrderStatus_Delivered() {

        // Arrange
        order.setStatus(OrderStatus.DELIVERED);
        when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(order));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            orderService.updateOrderStatus(1L, OrderStatus.CANCELLED);
        });

        assertEquals("Não é possível alterar status de pedido entregue", exception.getMessage());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar transição inválida de status")
    void testUpdateOrderStatus_InvalidTransition() {

        // Arrange
        order.setStatus(OrderStatus.PENDING);
        when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(order));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            orderService.updateOrderStatus(1L, OrderStatus.SHIPPED);
        });

        assertEquals("Status inválido para pedido pendente", exception.getMessage());
        verify(orderRepository, never()).save(any(Order.class));
    }
}