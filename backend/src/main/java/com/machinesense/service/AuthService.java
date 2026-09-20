package com.machinesense.service;

import com.machinesense.dto.AuthDtos;
import com.machinesense.entity.User;
import com.machinesense.repository.UserRepository;
import com.machinesense.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    public AuthService(AuthenticationManager authenticationManager, UserRepository userRepository, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    public AuthDtos.LoginResponse login(AuthDtos.LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username, request.password));

        User user = userRepository.findByUsername(request.username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername()).password(user.getPassword()).authorities("ROLE_" + user.getRole()).build();

        String token = jwtService.generateToken(userDetails, user.getRole().name());
        return new AuthDtos.LoginResponse(token, user.getUsername(), user.getRole().name(), user.getFullName());
    }
}
