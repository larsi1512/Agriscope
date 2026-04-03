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
public class UserServiceImplGetUserTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;
    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void shouldReturnUserById() throws ValidationException, ConflictException {
        UserCreateDto dto = new UserCreateDto();
        dto.setEmail("getbyid@test.com");
        dto.setFirstName("Test");
        dto.setLastName("User");
        dto.setPassword("Test123!");
        dto.setPassword2("Test123!");
        var user = userService.createUser(dto);

        UserDetailsDto details = userService.getUserById(user.getId());

        assertEquals(user.getId(), details.getId());
        assertEquals("getbyid@test.com", details.getEmail());
    }

    @Test
    void shouldThrowWhenUserNotFoundById() {
        NotFoundException ex = assertThrows(NotFoundException.class, () -> {
            userService.getUserById("non-existent-id");
        });
        assertTrue(ex.getMessage().contains("User not found"));
    }
    @Test
    void shouldReturnUserByEmail() throws ValidationException, ConflictException {
        UserCreateDto dto = new UserCreateDto();
        dto.setEmail("getbyemail@test.com");
        dto.setFirstName("Test");
        dto.setLastName("User");
        dto.setPassword("Test123!");
        dto.setPassword2("Test123!");
        var user = userService.createUser(dto);

        UserDetailsDto details = userService.getUserByEmail("getbyemail@test.com");

        assertEquals(user.getId(), details.getId());
        assertEquals("getbyemail@test.com", details.getEmail());
    }

    @Test
    void shouldThrowWhenUserNotFoundByEmail() {
        NotFoundException ex = assertThrows(NotFoundException.class, () -> {
            userService.getUserByEmail("non-existent@test.com");
        });
        assertTrue(ex.getMessage().contains("User not found"));
    }
}
