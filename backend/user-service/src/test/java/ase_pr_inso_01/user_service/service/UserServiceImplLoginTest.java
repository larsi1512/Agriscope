package ase_pr_inso_01.user_service.service;

import ase_pr_inso_01.user_service.controller.dto.user.*;
import ase_pr_inso_01.user_service.exception.ConflictException;
import ase_pr_inso_01.user_service.exception.NotFoundException;
import ase_pr_inso_01.user_service.exception.ValidationException;
import ase_pr_inso_01.user_service.repository.UserRepository;
import ase_pr_inso_01.user_service.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;



@SpringBootTest
@ActiveProfiles("test")
public class UserServiceImplLoginTest {
    // TESTING LOGIN

    @Autowired
    private UserRepository userRepository;
    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Autowired
    private UserService userService;
    @Test
    void shouldLoginSuccessfully() throws ValidationException, ConflictException {
        // Arrange - create user first
        UserCreateDto dto = new UserCreateDto();
        dto.setEmail("login123@test.com");
        dto.setFirstName("Test");
        dto.setLastName("User");
        dto.setPassword("Test123!");
        dto.setPassword2("Test123!");
        userService.createUser(dto);

        UserLoginDto loginDto = new UserLoginDto();
        loginDto.setEmail("login123@test.com");
        loginDto.setPassword("Test123!");

        String token = userService.login(loginDto);

        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void shouldFailLoginWrongPassword() throws ValidationException, ConflictException {
        UserCreateDto dto = new UserCreateDto();
        dto.setEmail("wrongpass@test.com");
        dto.setFirstName("Test");
        dto.setLastName("User");
        dto.setPassword("Test123!");
        dto.setPassword2("Test123!");
        userService.createUser(dto);

        UserLoginDto loginDto = new UserLoginDto();
        loginDto.setEmail("wrongpass@test.com");
        loginDto.setPassword("Wrong123!");

        ValidationException ex = assertThrows(ValidationException.class, () -> {
            userService.login(loginDto);
        });

        assertEquals("Wrong password", ex.getErrors().get("password"));
    }

    @Test
    void shouldFailLoginEmptyEmail() {
        UserLoginDto loginDto = new UserLoginDto();
        loginDto.setEmail("");
        loginDto.setPassword("Test123!");

        NotFoundException ex = assertThrows(NotFoundException.class, () -> {
            userService.login(loginDto);
        });

        assertEquals("User with given e-mail was not found!", ex.getMessage());
    }

    @Test
    void shouldFailLoginNonExistentEmail() {
        UserLoginDto loginDto = new UserLoginDto();
        loginDto.setEmail("nonexistent@test.com");
        loginDto.setPassword("Test123!");

        NotFoundException ex = assertThrows(NotFoundException.class, () -> {
            userService.login(loginDto);
        });

        assertEquals("User with given e-mail was not found!", ex.getMessage());
    }

}
