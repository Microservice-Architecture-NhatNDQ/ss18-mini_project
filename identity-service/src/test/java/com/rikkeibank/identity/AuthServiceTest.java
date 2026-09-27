package com.rikkeibank.identity;

import com.rikkeibank.identity.dto.LoginRequest;
import com.rikkeibank.identity.dto.RegisterRequest;
import com.rikkeibank.identity.dto.AuthResponse;
import com.rikkeibank.identity.entity.Role;
import com.rikkeibank.identity.entity.User;
import com.rikkeibank.identity.repository.UserRepository;
import com.rikkeibank.identity.security.JwtService;
import com.rikkeibank.identity.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private AuthService authService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .email("test@rikkeibank.com")
                .password("encoded_pass")
                .fullName("Test User")
                .role(Role.ROLE_CUSTOMER)
                .status("ACTIVE")
                .build();
    }

    @Test
    void testRegister_Success() {
        RegisterRequest req = RegisterRequest.builder()
                .email("test@rikkeibank.com")
                .password("raw_pass")
                .fullName("Test User")
                .role(Role.ROLE_CUSTOMER)
                .build();

        when(userRepository.existsByEmail("test@rikkeibank.com")).thenReturn(false);
        when(passwordEncoder.encode("raw_pass")).thenReturn("encoded_pass");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(jwtService.generateAccessToken(sampleUser)).thenReturn("access_token");
        when(jwtService.generateRefreshToken(sampleUser)).thenReturn("refresh_token");
        when(jwtService.getExpirationMs()).thenReturn(86400000L);

        AuthResponse res = authService.register(req);

        assertNotNull(res);
        assertEquals("access_token", res.getAccessToken());
        assertEquals("refresh_token", res.getRefreshToken());
        assertEquals("test@rikkeibank.com", res.getUser().getEmail());
    }

    @Test
    void testLogin_Success() {
        LoginRequest req = LoginRequest.builder()
                .email("test@rikkeibank.com")
                .password("raw_pass")
                .build();

        when(userRepository.findByEmail("test@rikkeibank.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("raw_pass", "encoded_pass")).thenReturn(true);
        when(jwtService.generateAccessToken(sampleUser)).thenReturn("access_token");
        when(jwtService.generateRefreshToken(sampleUser)).thenReturn("refresh_token");
        when(jwtService.getExpirationMs()).thenReturn(86400000L);

        AuthResponse res = authService.login(req);

        assertNotNull(res);
        assertEquals("access_token", res.getAccessToken());
    }

    @Test
    void testLogin_WrongPassword_ThrowsException() {
        LoginRequest req = LoginRequest.builder()
                .email("test@rikkeibank.com")
                .password("wrong_pass")
                .build();

        when(userRepository.findByEmail("test@rikkeibank.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("wrong_pass", "encoded_pass")).thenReturn(false);

        assertThrows(ResponseStatusException.class, () -> authService.login(req));
    }
}
