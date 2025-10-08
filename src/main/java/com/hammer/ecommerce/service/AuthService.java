package com.hammer.ecommerce.service;

import com.hammer.ecommerce.dto.AuthResponseDTO;
import com.hammer.ecommerce.dto.LoginRequestDTO;
import com.hammer.ecommerce.dto.RegisterRequestDTO;
import com.hammer.ecommerce.exceptions.BusinessException;
import com.hammer.ecommerce.model.Role;
import com.hammer.ecommerce.model.User;
import com.hammer.ecommerce.repositories.UserRepository;
import com.hammer.ecommerce.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponseDTO register(RegisterRequestDTO request) {
        // Verificar se email já existe
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email já cadastrado");
        }

        // Verificar se CPF já existe
        if (userRepository.existsByCpf(request.getCpf())) {
            throw new BusinessException("CPF já cadastrado");
        }

        // Criar novo usuário
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setCpf(request.getCpf());
        user.setPhone(request.getPhone());
        user.setRole(Role.CUSTOMER);

        // Salvar usuário
        user = userRepository.save(user);

        // Gerar token JWT
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());

        return new AuthResponseDTO(
                token,
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name()
        );
    }

    public AuthResponseDTO login(LoginRequestDTO request) {
        // Autenticar usuário
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // Buscar usuário
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException("Usuário não encontrado"));

        // Gerar token JWT
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());

        return new AuthResponseDTO(
                token,
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name()
        );
    }
}