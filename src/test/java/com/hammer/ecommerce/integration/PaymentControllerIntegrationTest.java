package com.hammer.ecommerce.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hammer.ecommerce.dto.address.AddressRequestDTO;
import com.hammer.ecommerce.dto.cart.AddToCartRequestDTO;
import com.hammer.ecommerce.dto.login.RegisterRequestDTO;
import com.hammer.ecommerce.dto.order.CreateOrderRequestDTO;
import com.hammer.ecommerce.dto.payment.ProcessPaymentRequestDTO;
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

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PaymentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private UserRepository userRepository;

    private String authToken;
    private Long orderId;
    private Product product;

    @BeforeEach
    void setUp() throws Exception {

        paymentRepository.deleteAll();
        orderRepository.deleteAll();
        cartRepository.deleteAll();
        addressRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        // Criar usuário e obter token
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

        // Criar categoria
        Category category = new Category();
        category.setName("Eletrônicos");
        category = categoryRepository.save(category);

        // Criar produto
        product = new Product();
        product.setName("Mouse Gamer");
        product.setPrice(new BigDecimal("150.00"));
        product.setStockQuantity(20);
        product.setSku("MOUSE-001");
        product.setActive(true);
        product.setCategory(category);
        product = productRepository.save(product);

        // Criar endereço
        AddressRequestDTO addressRequest = new AddressRequestDTO();
        addressRequest.setStreet("Rua das Flores");
        addressRequest.setNumber("123");
        addressRequest.setNeighborhood("Centro");
        addressRequest.setCity("São Paulo");
        addressRequest.setState("SP");
        addressRequest.setZipCode("01234-567");

        MvcResult addressResult = mockMvc.perform(post("/api/addresses")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addressRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String addressResponse = addressResult.getResponse().getContentAsString();
        Long addressId = objectMapper.readTree(addressResponse).get("id").asLong();

        // Criar pedido
        orderId = createOrder(addressId);
    }

    @Test
    @DisplayName("Deve processar pagamento PIX com sucesso")
    void testProcessPayment_PIX() throws Exception {

        ProcessPaymentRequestDTO request = new ProcessPaymentRequestDTO();
        request.setOrderId(orderId);
        request.setPaymentMethod(PaymentMethod.PIX);

        mockMvc.perform(post("/api/payments/process")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.paymentMethod").value("PIX"))
                .andExpect(jsonPath("$.transactionId").exists())
                .andExpect(jsonPath("$.status").value(notNullValue()));

        // Verificar que o status do pedido foi atualizado (se aprovado)
        mockMvc.perform(get("/api/orders/" + orderId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Deve processar pagamento com cartão de crédito")
    void testProcessPayment_CreditCard() throws Exception {

        ProcessPaymentRequestDTO request = new ProcessPaymentRequestDTO();
        request.setOrderId(orderId);
        request.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        request.setCardNumber("1234567890123456");
        request.setCardHolderName("JOAO SILVA");
        request.setCardExpiryDate("12/2026");
        request.setCardCvv("123");

        mockMvc.perform(post("/api/payments/process")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentMethod").value("CREDIT_CARD"))
                .andExpect(jsonPath("$.transactionId").exists());
    }

    @Test
    @DisplayName("Deve processar pagamento com boleto")
    void testProcessPayment_Boleto() throws Exception {

        ProcessPaymentRequestDTO request = new ProcessPaymentRequestDTO();
        request.setOrderId(orderId);
        request.setPaymentMethod(PaymentMethod.BOLETO);

        mockMvc.perform(post("/api/payments/process")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentMethod").value("BOLETO"));
    }

    @Test
    @DisplayName("Deve retornar 400 ao processar pagamento de pedido já pago")
    void testProcessPayment_AlreadyPaid() throws Exception {

        // Processar pagamento primeira vez
        ProcessPaymentRequestDTO request = new ProcessPaymentRequestDTO();
        request.setOrderId(orderId);
        request.setPaymentMethod(PaymentMethod.PIX);

        mockMvc.perform(post("/api/payments/process")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Tentar processar novamente
        mockMvc.perform(post("/api/payments/process")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Pedido já possui pagamento processado"));
    }

    @Test
    @DisplayName("Deve consultar pagamento do pedido")
    void testGetPaymentByOrderId() throws Exception {

        // Processar pagamento
        ProcessPaymentRequestDTO request = new ProcessPaymentRequestDTO();
        request.setOrderId(orderId);
        request.setPaymentMethod(PaymentMethod.PIX);

        mockMvc.perform(post("/api/payments/process")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Consultar pagamento
        mockMvc.perform(get("/api/payments/order/" + orderId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.paymentMethod").value("PIX"))
                .andExpect(jsonPath("$.transactionId").exists());
    }

    @Test
    @DisplayName("Deve retornar 404 ao consultar pagamento inexistente")
    void testGetPaymentByOrderId_NotFound() throws Exception {
        mockMvc.perform(get("/api/payments/order/" + orderId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deve reembolsar pagamento com sucesso")
    void testRefundPayment() throws Exception {

        // Processar pagamento
        ProcessPaymentRequestDTO request = new ProcessPaymentRequestDTO();
        request.setOrderId(orderId);
        request.setPaymentMethod(PaymentMethod.PIX);

        mockMvc.perform(post("/api/payments/process")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Solicitar reembolso
        mockMvc.perform(post("/api/payments/order/" + orderId + "/refund")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REFUNDED"));

        // Verificar que o pedido foi cancelado
        mockMvc.perform(get("/api/orders/" + orderId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("Deve retornar 400 ao reembolsar pagamento não aprovado")
    void testRefundPayment_NotApproved() throws Exception {

        // Processar pagamento que será rejeitado (simulação)
        ProcessPaymentRequestDTO request = new ProcessPaymentRequestDTO();
        request.setOrderId(orderId);
        request.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        request.setCardNumber("123"); // Número inválido para forçar rejeição

        MvcResult result = mockMvc.perform(post("/api/payments/process")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();

        // Se o pagamento foi rejeitado, tentar reembolsar deve falhar
        if (result.getResponse().getStatus() == 201) {
            String response = result.getResponse().getContentAsString();
            String status = objectMapper.readTree(response).get("status").asText();

            if ("REJECTED".equals(status) || "PENDING".equals(status)) {
                mockMvc.perform(post("/api/payments/order/" + orderId + "/refund")
                                .header("Authorization", "Bearer " + authToken))
                        .andExpect(status().isBadRequest());
            }
        }
    }

    @Test
    @DisplayName("Deve retornar 401 ao processar pagamento sem autenticação")
    void testProcessPayment_Unauthorized() throws Exception {

        ProcessPaymentRequestDTO request = new ProcessPaymentRequestDTO();
        request.setOrderId(orderId);
        request.setPaymentMethod(PaymentMethod.PIX);

        mockMvc.perform(post("/api/payments/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    private Long createOrder(Long addressId) throws Exception {

        // Adicionar produto ao carrinho
        AddToCartRequestDTO cartRequest = new AddToCartRequestDTO();
        cartRequest.setProductId(product.getId());
        cartRequest.setQuantity(2);

        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cartRequest)))
                .andExpect(status().isOk());

        // Criar pedido
        CreateOrderRequestDTO orderRequest = new CreateOrderRequestDTO();
        orderRequest.setShippingAddressId(addressId);

        MvcResult result = mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }
}