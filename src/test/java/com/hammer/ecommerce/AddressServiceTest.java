package com.hammer.ecommerce;

import com.hammer.ecommerce.dto.address.AddressRequestDTO;
import com.hammer.ecommerce.dto.address.AddressResponseDTO;
import com.hammer.ecommerce.exceptions.BusinessException;
import com.hammer.ecommerce.exceptions.ResourceNotFoundException;
import com.hammer.ecommerce.model.Address;
import com.hammer.ecommerce.model.User;
import com.hammer.ecommerce.repositories.AddressRepository;
import com.hammer.ecommerce.repositories.UserRepository;
import com.hammer.ecommerce.service.AddressService;
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
class AddressServiceTest {

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private AddressService addressService;

    private User user;
    private Address address;
    private AddressRequestDTO addressRequest;
    private AddressResponseDTO addressResponse;

    @BeforeEach
    void setUp() {

        user = new User();
        user.setId(1L);
        user.setName("João Silva");
        user.setEmail("joao@email.com");

        address = new Address();
        address.setId(1L);
        address.setStreet("Rua das Flores");
        address.setNumber("123");
        address.setComplement("Apto 45");
        address.setNeighborhood("Centro");
        address.setCity("São Paulo");
        address.setState("SP");
        address.setZipCode("01234-567");
        address.setIsDefault(true);
        address.setUser(user);

        addressRequest = new AddressRequestDTO();
        addressRequest.setStreet("Rua das Flores");
        addressRequest.setNumber("123");
        addressRequest.setComplement("Apto 45");
        addressRequest.setNeighborhood("Centro");
        addressRequest.setCity("São Paulo");
        addressRequest.setState("SP");
        addressRequest.setZipCode("01234567");
        addressRequest.setIsDefault(true);

        addressResponse = new AddressResponseDTO();
        addressResponse.setId(1L);
        addressResponse.setStreet("Rua das Flores");
        addressResponse.setNumber("123");
        addressResponse.setIsDefault(true);
    }

    @Test
    @DisplayName("Deve listar todos os endereços do usuário")
    void testFindAllByUser_Success() {

        // Arrange
        List<Address> addresses = Arrays.asList(address);
        when(addressRepository.findByUserId(1L)).thenReturn(addresses);
        when(modelMapper.map(address, AddressResponseDTO.class)).thenReturn(addressResponse);

        // Act
        List<AddressResponseDTO> result = addressService.findAllByUser(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Rua das Flores", result.get(0).getStreet());
        verify(addressRepository, times(1)).findByUserId(1L);
    }

    @Test
    @DisplayName("Deve buscar endereço por ID com sucesso")
    void testFindById_Success() {

        // Arrange
        when(addressRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(address));
        when(modelMapper.map(address, AddressResponseDTO.class)).thenReturn(addressResponse);

        // Act
        AddressResponseDTO result = addressService.findById(1L, 1L);

        // Assert
        assertNotNull(result);
        assertEquals("Rua das Flores", result.getStreet());
        verify(addressRepository, times(1)).findByIdAndUserId(1L, 1L);
    }

    @Test
    @DisplayName("Deve lançar exceção ao buscar endereço inexistente")
    void testFindById_NotFound() {

        // Arrange
        when(addressRepository.findByIdAndUserId(999L, 1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            addressService.findById(999L, 1L);
        });
        verify(addressRepository, times(1)).findByIdAndUserId(999L, 1L);
    }

    @Test
    @DisplayName("Deve criar endereço com sucesso")
    void testCreate_Success() {

        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(addressRepository.findByUserId(1L)).thenReturn(Arrays.asList());
        when(addressRepository.save(any(Address.class))).thenReturn(address);
        when(modelMapper.map(address, AddressResponseDTO.class)).thenReturn(addressResponse);

        // Act
        AddressResponseDTO result = addressService.create(addressRequest, 1L);

        // Assert
        assertNotNull(result);
        assertEquals("Rua das Flores", result.getStreet());
        verify(userRepository, times(1)).findById(1L);
        verify(addressRepository, times(1)).save(any(Address.class));
    }

    @Test
    @DisplayName("Deve marcar primeiro endereço como padrão automaticamente")
    void testCreate_FirstAddressAsDefault() {

        // Arrange
        addressRequest.setIsDefault(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(addressRepository.findByUserId(1L)).thenReturn(Arrays.asList());
        when(addressRepository.save(any(Address.class))).thenReturn(address);
        when(modelMapper.map(address, AddressResponseDTO.class)).thenReturn(addressResponse);

        // Act
        AddressResponseDTO result = addressService.create(addressRequest, 1L);

        // Assert
        assertNotNull(result);
        assertTrue(addressRequest.getIsDefault());
        verify(addressRepository, times(1)).save(any(Address.class));
    }

    @Test
    @DisplayName("Deve desmarcar endereço padrão anterior ao criar novo padrão")
    void testCreate_UnsetPreviousDefault() {

        // Arrange
        Address oldDefault = new Address();
        oldDefault.setId(2L);
        oldDefault.setIsDefault(true);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(addressRepository.findByUserId(1L)).thenReturn(Arrays.asList(oldDefault));
        when(addressRepository.findByUserIdAndIsDefaultTrue(1L)).thenReturn(Optional.of(oldDefault));
        when(addressRepository.save(any(Address.class))).thenReturn(address);
        when(modelMapper.map(address, AddressResponseDTO.class)).thenReturn(addressResponse);

        // Act
        AddressResponseDTO result = addressService.create(addressRequest, 1L);

        // Assert
        assertNotNull(result);
        assertFalse(oldDefault.getIsDefault());
        verify(addressRepository, times(2)).save(any(Address.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao criar endereço com usuário inexistente")
    void testCreate_UserNotFound() {

        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            addressService.create(addressRequest, 999L);
        });
        verify(userRepository, times(1)).findById(999L);
        verify(addressRepository, never()).save(any(Address.class));
    }

    @Test
    @DisplayName("Deve atualizar endereço com sucesso")
    void testUpdate_Success() {

        // Arrange
        when(addressRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(address));
        when(addressRepository.save(any(Address.class))).thenReturn(address);
        when(modelMapper.map(address, AddressResponseDTO.class)).thenReturn(addressResponse);

        // Act
        AddressResponseDTO result = addressService.update(1L, addressRequest, 1L);

        // Assert
        assertNotNull(result);
        verify(addressRepository, times(1)).findByIdAndUserId(1L, 1L);
        verify(addressRepository, times(1)).save(any(Address.class));
    }

    @Test
    @DisplayName("Deve formatar CEP corretamente")
    void testCreate_FormatZipCode() {

        // Arrange
        addressRequest.setZipCode("01234567"); // Sem hífen
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(addressRepository.findByUserId(1L)).thenReturn(Arrays.asList());
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> {
            Address saved = invocation.getArgument(0);
            assertEquals("01234-567", saved.getZipCode());
            return saved;
        });
        when(modelMapper.map(any(Address.class), eq(AddressResponseDTO.class))).thenReturn(addressResponse);

        // Act
        AddressResponseDTO result = addressService.create(addressRequest, 1L);

        // Assert
        assertNotNull(result);
        verify(addressRepository, times(1)).save(any(Address.class));
    }

    @Test
    @DisplayName("Deve lançar exceção para CEP inválido")
    void testCreate_InvalidZipCode() {

        // Arrange
        addressRequest.setZipCode("123"); // CEP com menos de 8 dígitos
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(addressRepository.findByUserId(1L)).thenReturn(Arrays.asList());

        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            addressService.create(addressRequest, 1L);
        });
        verify(addressRepository, never()).save(any(Address.class));
    }

    @Test
    @DisplayName("Deve deletar endereço com sucesso")
    void testDelete_Success() {

        // Arrange
        address.setIsDefault(false);
        when(addressRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(address));

        // Act
        addressService.delete(1L, 1L);

        // Assert
        verify(addressRepository, times(1)).findByIdAndUserId(1L, 1L);
        verify(addressRepository, times(1)).delete(address);
    }

    @Test
    @DisplayName("Deve marcar outro endereço como padrão ao deletar o padrão")
    void testDelete_SetNewDefault() {

        // Arrange
        Address otherAddress = new Address();
        otherAddress.setId(2L);
        otherAddress.setIsDefault(false);

        address.setIsDefault(true);
        when(addressRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(address));
        when(addressRepository.findByUserId(1L)).thenReturn(Arrays.asList(otherAddress));

        // Act
        addressService.delete(1L, 1L);

        // Assert
        assertTrue(otherAddress.getIsDefault());
        verify(addressRepository, times(1)).delete(address);
        verify(addressRepository, times(1)).save(otherAddress);
    }

    @Test
    @DisplayName("Deve definir endereço como padrão")
    void testSetAsDefault_Success() {

        // Arrange
        address.setIsDefault(false);
        Address oldDefault = new Address();
        oldDefault.setId(2L);
        oldDefault.setIsDefault(true);

        when(addressRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(address));
        when(addressRepository.findByUserIdAndIsDefaultTrue(1L)).thenReturn(Optional.of(oldDefault));
        when(addressRepository.save(any(Address.class))).thenReturn(address);
        when(modelMapper.map(address, AddressResponseDTO.class)).thenReturn(addressResponse);

        // Act
        AddressResponseDTO result = addressService.setAsDefault(1L, 1L);

        // Assert
        assertNotNull(result);
        assertTrue(address.getIsDefault());
        assertFalse(oldDefault.getIsDefault());
        verify(addressRepository, times(2)).save(any(Address.class));
    }
}