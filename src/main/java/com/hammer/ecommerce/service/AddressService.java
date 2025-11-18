package com.hammer.ecommerce.service;

import com.hammer.ecommerce.dto.address.AddressRequestDTO;
import com.hammer.ecommerce.dto.address.AddressResponseDTO;
import com.hammer.ecommerce.exceptions.BusinessException;
import com.hammer.ecommerce.exceptions.ResourceNotFoundException;
import com.hammer.ecommerce.model.Address;
import com.hammer.ecommerce.model.User;
import com.hammer.ecommerce.repositories.AddressRepository;
import com.hammer.ecommerce.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    @Transactional(readOnly = true)
    public List<AddressResponseDTO> findAllByUser(Long userId) {
        return addressRepository.findByUserId(userId).stream()
                .map(address -> modelMapper.map(address, AddressResponseDTO.class))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AddressResponseDTO findById(Long id, Long userId) {
        Address address = addressRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Endereço não encontrado"));
        return modelMapper.map(address, AddressResponseDTO.class);
    }

    @Transactional
    public AddressResponseDTO create(AddressRequestDTO request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        // Se for marcado como padrão, desmarcar os outros
        if (request.getIsDefault()) {
            unsetDefaultAddress(userId);
        }

        // Se for o primeiro endereço, marcar como padrão automaticamente
        List<Address> existingAddresses = addressRepository.findByUserId(userId);
        if (existingAddresses.isEmpty()) {
            request.setIsDefault(true);
        }

        Address address = new Address();
        address.setStreet(request.getStreet());
        address.setNumber(request.getNumber());
        address.setComplement(request.getComplement());
        address.setNeighborhood(request.getNeighborhood());
        address.setCity(request.getCity());
        address.setState(request.getState().toUpperCase());
        address.setZipCode(formatZipCode(request.getZipCode()));
        address.setIsDefault(request.getIsDefault());
        address.setUser(user);

        address = addressRepository.save(address);
        return modelMapper.map(address, AddressResponseDTO.class);
    }

    @Transactional
    public AddressResponseDTO update(Long id, AddressRequestDTO request, Long userId) {
        Address address = addressRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Endereço não encontrado"));

        // Se for marcado como padrão, desmarcar os outros
        if (request.getIsDefault() && !address.getIsDefault()) {
            unsetDefaultAddress(userId);
        }

        address.setStreet(request.getStreet());
        address.setNumber(request.getNumber());
        address.setComplement(request.getComplement());
        address.setNeighborhood(request.getNeighborhood());
        address.setCity(request.getCity());
        address.setState(request.getState().toUpperCase());
        address.setZipCode(formatZipCode(request.getZipCode()));
        address.setIsDefault(request.getIsDefault());

        address = addressRepository.save(address);
        return modelMapper.map(address, AddressResponseDTO.class);
    }

    @Transactional
    public void delete(Long id, Long userId) {
        Address address = addressRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Endereço não encontrado"));

        boolean wasDefault = address.getIsDefault();
        addressRepository.delete(address);

        // Se era o endereço padrão, marcar outro como padrão
        if (wasDefault) {
            List<Address> remainingAddresses = addressRepository.findByUserId(userId);
            if (!remainingAddresses.isEmpty()) {
                Address newDefault = remainingAddresses.get(0);
                newDefault.setIsDefault(true);
                addressRepository.save(newDefault);
            }
        }
    }

    @Transactional
    public AddressResponseDTO setAsDefault(Long id, Long userId) {
        Address address = addressRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Endereço não encontrado"));

        // Desmarcar todos os outros como padrão
        unsetDefaultAddress(userId);

        // Marcar este como padrão
        address.setIsDefault(true);
        address = addressRepository.save(address);

        return modelMapper.map(address, AddressResponseDTO.class);
    }

    private void unsetDefaultAddress(Long userId) {
        addressRepository.findByUserIdAndIsDefaultTrue(userId)
                .ifPresent(defaultAddress -> {
                    defaultAddress.setIsDefault(false);
                    addressRepository.save(defaultAddress);
                });
    }

    private String formatZipCode(String zipCode) {
        // Remove tudo que não é número
        String cleaned = zipCode.replaceAll("\\D", "");

        // Valida se tem 8 dígitos
        if (cleaned.length() != 8) {
            throw new BusinessException("CEP inválido");
        }

        // Formata como 00000-000
        return cleaned.substring(0, 5) + "-" + cleaned.substring(5);
    }
}
