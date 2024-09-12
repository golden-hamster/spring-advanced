package org.example.expert.domain.todo.service;

import org.example.expert.client.WeatherClient;
import org.example.expert.domain.common.dto.AuthUser;
import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.todo.dto.request.TodoSaveRequest;
import org.example.expert.domain.todo.dto.response.TodoResponse;
import org.example.expert.domain.todo.dto.response.TodoSaveResponse;
import org.example.expert.domain.todo.entity.Todo;
import org.example.expert.domain.todo.repository.TodoRepository;
import org.example.expert.domain.user.entity.User;
import org.example.expert.domain.user.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class TodoServiceTest {

    @Mock
    private TodoRepository todoRepository;

    @Mock
    private WeatherClient weatherClient;

    @InjectMocks
    private TodoService todoService;

    @Test
    @DisplayName("투두를 정상적으로 저장한다")
    void todo를_정상적으로_저장한다() {
        //Given
        User user = createUser(1L);
        AuthUser authUser = new AuthUser(user.getId(), user.getEmail(), user.getUserRole());
        TodoSaveRequest request = new TodoSaveRequest("title", "contents");
        Todo todo = new Todo("title", "contents", "Sunny", user);
        ReflectionTestUtils.setField(todo, "id", 1L);

        given(weatherClient.getTodayWeather()).willReturn("Sunny");
        given(todoRepository.save(any())).willReturn(todo);

        //When
        TodoSaveResponse response = todoService.saveTodo(authUser, request);


        //Then
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Sunny", response.getWeather());
        assertEquals("title", response.getTitle());
        verify(todoRepository, times(1)).save(any(Todo.class));
    }

    @Test
    @DisplayName("투두 목록을 정상적으로 가져온다")
    void todo_목록을_정상적으로_가져온다() {
        //Given
        User user = createUser(1L);
        Todo todo1 = new Todo("title1", "contents1", "Sunny", user);
        Todo todo2 = new Todo("title2", "contents2", "Rainy", user);
        List<Todo> todos = Arrays.asList(todo1, todo2);
        Page<Todo> todoPage = new PageImpl<>(todos);

        given(todoRepository.findAllByOrderByModifiedAtDesc(any(Pageable.class))).willReturn(todoPage);

        //When
        Page<TodoResponse> response = todoService.getTodos(1, 10);

        //Then
        assertNotNull(response);
        assertEquals(2, response.getTotalElements());
        assertEquals("title1", response.getContent().get(0).getTitle());
        assertEquals("Sunny", response.getContent().get(0).getWeather());
        verify(todoRepository, times(1)).findAllByOrderByModifiedAtDesc(any(Pageable.class));
    }

    @Test
    @DisplayName("투두를 ID로 정상적으로 가져온다")
    void todo를_ID로_정상적으로_가져온다() {
        //Given
        User user = createUser(1L);
        Todo todo = new Todo("title", "contents", "Cloudy", user);
        ReflectionTestUtils.setField(todo, "id", 1L);

        given(todoRepository.findByIdWithUser(1L)).willReturn(Optional.of(todo));

        //When
        TodoResponse response = todoService.getTodo(1L);

        //Then
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("title", response.getTitle());
        assertEquals("Cloudy", response.getWeather());
        verify(todoRepository, times(1)).findByIdWithUser(1L);
    }

    @Test
    @DisplayName("투두를 찾지 못하면 예외를 발생시킨다")
    void todo를_찾지_못하면_예외를_발생시킨다() {
        //Given
        given(todoRepository.findByIdWithUser(1L)).willReturn(Optional.empty());

        //When & Then
        InvalidRequestException exception = assertThrows(InvalidRequestException.class, () -> todoService.getTodo(1L));
        assertEquals("Todo not found", exception.getMessage());
        verify(todoRepository, times(1)).findByIdWithUser(1L);
    }

    private User createUser(long userId) {
        User user = new User("email@email.com", "oldPassword123", UserRole.USER);
        ReflectionTestUtils.setField(user, "id", userId);
        return user;
    }


}