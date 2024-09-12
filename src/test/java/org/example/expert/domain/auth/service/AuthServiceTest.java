package org.example.expert.domain.auth.service;

import org.example.expert.config.JwtUtil;
import org.example.expert.config.PasswordEncoder;
import org.example.expert.domain.auth.dto.request.SigninRequest;
import org.example.expert.domain.auth.dto.request.SignupRequest;
import org.example.expert.domain.auth.dto.response.SigninResponse;
import org.example.expert.domain.auth.dto.response.SignupResponse;
import org.example.expert.domain.auth.exception.AuthException;
import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.user.entity.User;
import org.example.expert.domain.user.enums.UserRole;
import org.example.expert.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("회원가입 시 정상적으로 토큰을 반환한다")
    void signup_정상적으로_토큰을_반환한다() {
        //Given
        User user = createUser(1L);
        SignupRequest signupRequest = new SignupRequest("email@email.com", "password123", "USER");
        given(passwordEncoder.encode(anyString())).willReturn("encodedPassword");
        given(userRepository.existsByEmail(anyString())).willReturn(false);
        given(userRepository.save(any(User.class))).willReturn(user);
        given(jwtUtil.createToken(anyLong(), anyString(), any(UserRole.class))).willReturn("Bearer token");

        //When
        SignupResponse signupResponse = authService.signup(signupRequest);

        //Then
        assertNotNull(signupResponse);
        assertEquals("Bearer token", signupResponse.getBearerToken());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("회원가입 시 이미 존재하는 이메일로 예외가 발생한다")
    void signup_이미_존재하는_이메일로_예외가_발생한다() {
        //Given
        SignupRequest signupRequest = new SignupRequest("email@email.com", "password123", "USER");
        given(userRepository.existsByEmail(anyString())).willReturn(true);

        //When & Then
        InvalidRequestException exception = assertThrows(InvalidRequestException.class,
                () -> authService.signup(signupRequest));
        assertEquals("이미 존재하는 이메일입니다.", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("로그인 시 정상적으로 토큰을 반환한다")
    void signin_정상적으로_토큰을_반환한다() {
        //Given
        User user = createUser(1L);
        SigninRequest signinRequest = new SigninRequest("email@email.com", "password123");
        given(userRepository.findByEmail(anyString())).willReturn(Optional.of(user));
        given(passwordEncoder.matches(anyString(), anyString())).willReturn(true);
        given(jwtUtil.createToken(anyLong(), anyString(), any(UserRole.class))).willReturn("Bearer token");

        //When
        SigninResponse signinResponse = authService.signin(signinRequest);

        //Then
        assertNotNull(signinResponse);
        assertEquals("Bearer token", signinResponse.getBearerToken());
        verify(jwtUtil, times(1)).createToken(anyLong(), anyString(), any(UserRole.class));
    }

    @Test
    @DisplayName("로그인 시 비밀번호가 일치하지 않으면 예외가 발생한다")
    void signin_비밀번호_불일치_예외발생() {
        //Given
        User user = createUser(1L);
        SigninRequest signinRequest = new SigninRequest("email@email.com", "wrongPassword");
        given(userRepository.findByEmail(anyString())).willReturn(Optional.of(user));
        given(passwordEncoder.matches(anyString(), anyString())).willReturn(false);

        //When & Then
        AuthException exception = assertThrows(AuthException.class,
                () -> authService.signin(signinRequest));
        assertEquals("잘못된 비밀번호입니다.", exception.getMessage());
        verify(jwtUtil, never()).createToken(anyLong(), anyString(), any(UserRole.class));
    }

    private User createUser(long id) {
        User user = new User("email@email.com", "Password123", UserRole.USER);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}