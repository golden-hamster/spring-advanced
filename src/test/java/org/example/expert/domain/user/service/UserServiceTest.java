package org.example.expert.domain.user.service;

import org.example.expert.config.PasswordEncoder;
import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.user.dto.request.UserChangePasswordRequest;
import org.example.expert.domain.user.dto.response.UserResponse;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @DisplayName("유저를 정상적으로 가져온다.")
    @Test
    void user를_정상적으로_가져온다() {
        //Given
        long userId = 1L;
        User user = createUser(userId);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        //When
        UserResponse userResponse = userService.getUser(userId);

        //Then
        assertEquals(userId, userResponse.getId());
        assertEquals("email@email.com", userResponse.getEmail());
        verify(userRepository, times(1)).findById(userId);
    }

    @DisplayName("비밀번호를 변경할 때 유저가 존해하지 않으면 예외가 발생한다.")
    @Test
    void user가_존재하지_않으면_예외가_발생한다() {
        //Given
        long userId = 1L;
        UserChangePasswordRequest userChangePasswordRequest = new UserChangePasswordRequest("oldPassword123", "newPassword123");

        given(userRepository.findById(userId)).willReturn(Optional.empty());

        //When & Then
        InvalidRequestException exception = assertThrows(InvalidRequestException.class, () -> userService.changePassword(userId, userChangePasswordRequest));
        assertEquals("User not found", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @DisplayName("비밀번호를 변경할 때 새 비밀번호가 유효하지 않으면 예외가 발생한다.")
    @Test
    void password가_유효하지_않으면_예외가_발생한다() {
        // Given
        long userId = 1L;
        UserChangePasswordRequest request = new UserChangePasswordRequest("oldPassword123", "short1");

        // When & Then
        InvalidRequestException exception = assertThrows(InvalidRequestException.class,
                () -> userService.changePassword(userId, request));

        assertEquals("새 비밀번호는 8자 이상이어야 하고, 숫자와 대문자를 포함해야 합니다.", exception.getMessage());
        verify(passwordEncoder, never()).encode(any());
    }

    @DisplayName("비밀번호를 변경할 때 새 비밀번호가 기존 비밀번호와 동일하면 예외가 발생한다.")
    @Test
    void password가_동일하면_예외가_발생한다() {
        // Given
        long userId = 1L;
        User user = createUser(1L);
        UserChangePasswordRequest request = new UserChangePasswordRequest("oldPassword123", "samePassword123");

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(passwordEncoder.matches(request.getNewPassword(), user.getPassword())).willReturn(true);

        // When & Then
        InvalidRequestException exception = assertThrows(InvalidRequestException.class,
                () -> userService.changePassword(userId, request));

        assertEquals("새 비밀번호는 기존 비밀번호와 같을 수 없습니다.", exception.getMessage());
        verify(passwordEncoder, never()).encode(any());
    }

    @DisplayName("비밀번호를 변경할 때 해당 유저의 비밀번호와 다르면 예외가 발생한다")
    @Test
    void password가_잘못된_경우_예외가_발생한다() {
        // Given
        long userId = 1L;
        User user = createUser(userId);
        UserChangePasswordRequest request = new UserChangePasswordRequest("wrongOldPassword", "newPassword123");

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(passwordEncoder.matches(request.getNewPassword(), user.getPassword())).willReturn(false);
        given(passwordEncoder.matches(request.getOldPassword(), user.getPassword())).willReturn(false);

        // When & Then
        InvalidRequestException exception = assertThrows(InvalidRequestException.class,
                () -> userService.changePassword(userId, request));

        assertEquals("잘못된 비밀번호입니다.", exception.getMessage());
        verify(passwordEncoder, never()).encode(any());
    }

    @DisplayName("비밀번호를 정상적으로 변경한다")
    @Test
    void password를_정상적으로_변경한다() {
        // Given
        long userId = 1L;
        User user = createUser(userId);
        UserChangePasswordRequest request = new UserChangePasswordRequest("oldPassword123", "newPassword123");

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(passwordEncoder.matches("oldPassword123", user.getPassword())).willReturn(true);
        given(passwordEncoder.matches("newPassword123", user.getPassword())).willReturn(false);
        given(passwordEncoder.encode("newPassword123")).willReturn("encodedNewPassword");

        // When
        userService.changePassword(userId, request);

        // Then
        assertEquals("encodedNewPassword", user.getPassword());  // 유저의 비밀번호가 변경되었는지 확인
    }

    private User createUser(long userId) {
        User user = new User("email@email.com", "oldPassword123", UserRole.USER);
        ReflectionTestUtils.setField(user, "id", userId);
        return user;
    }
}