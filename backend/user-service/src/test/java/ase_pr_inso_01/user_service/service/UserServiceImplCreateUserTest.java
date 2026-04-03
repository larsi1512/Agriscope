package ase_pr_inso_01.user_service.service;

import ase_pr_inso_01.user_service.controller.dto.user.UserCreateDto;
import ase_pr_inso_01.user_service.exception.ConflictException;
import ase_pr_inso_01.user_service.exception.ValidationException;
import ase_pr_inso_01.user_service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;



@SpringBootTest
@ActiveProfiles("test")
public class UserServiceImplCreateUserTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;
    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }
    @Test
    void shouldCreateUserAndStoreInDatabase() throws ValidationException, ConflictException {
        UserCreateDto dto = new UserCreateDto();
        dto.setEmail("real@test.com");
        dto.setFirstName("Ana");
        dto.setLastName("Real");
        dto.setPassword("Test123!");
        dto.setPassword2("Test123!");

        var user = userService.createUser(dto);

        assertNotNull(user.getId());
        assertEquals("real@test.com", user.getEmail());
        assertNotEquals("password", user.getPassword());
    }

    @Test
    void shouldFailForInvalidPassword() {
        UserCreateDto dto = new UserCreateDto();
        dto.setEmail("invalid@test.com");
        dto.setFirstName("Ana");
        dto.setLastName("Invalid");
        dto.setPassword("nopass");
        dto.setPassword2("nopass");

        ValidationException exception = assertThrows(ValidationException.class,
                () -> userService.createUser(dto));

        assertTrue(exception.getErrors().containsKey("password"));
        assertTrue(exception.getErrors().containsKey("password2"));
        System.out.println("Validation errors: " + exception.getErrors());
    }

    @Test
    void shouldFailForDuplicateEmail() throws ValidationException, ConflictException {
        UserCreateDto dto1 = new UserCreateDto();
        dto1.setEmail("duplicate@test.com");
        dto1.setFirstName("First");
        dto1.setLastName("User");
        dto1.setPassword("Test123!");
        dto1.setPassword2("Test123!");
        userService.createUser(dto1);

        UserCreateDto dto2 = new UserCreateDto();
        dto2.setEmail("duplicate@test.com");
        dto2.setFirstName("Second");
        dto2.setLastName("User");
        dto2.setPassword("Test123!");
        dto2.setPassword2("Test123!");

        ConflictException exception = assertThrows(ConflictException.class,
                () -> userService.createUser(dto2));

        assertTrue(exception.getErrors().containsKey("email"));
        assertEquals("User with the given email already exists",
                exception.getErrors().get("email"));
    }


    @Test
    void shouldFailForEmptyEmail() {
        UserCreateDto dto = new UserCreateDto();
        dto.setEmail("");
        dto.setFirstName("Ana");
        dto.setLastName("EmptyEmail");
        dto.setPassword("Test123!");
        dto.setPassword2("Test123!");

        ValidationException exception = assertThrows(ValidationException.class,
                () -> userService.createUser(dto));

        assertTrue(exception.getErrors().containsKey("email"));
        assertEquals("No email given",
                exception.getErrors().get("email"));
    }

    @Test
    void shouldFailForEmptyFirstName() {
        UserCreateDto dto = new UserCreateDto();
        dto.setEmail("emptyName@test.com");
        dto.setFirstName("");
        dto.setLastName("EmptyName");
        dto.setPassword("Test123!");
        dto.setPassword2("Test123!");

        ValidationException exception = assertThrows(ValidationException.class,
                () -> userService.createUser(dto));

        assertTrue(exception.getErrors().containsKey("firstName"));
        assertEquals("No first name given",
                exception.getErrors().get("firstName"));
    }


    @Test
    void shouldFailForEmptyLastName() {
        UserCreateDto dto = new UserCreateDto();
        dto.setEmail("emptyLastName@test.com");
        dto.setFirstName("EmptyLast");
        dto.setLastName("");
        dto.setPassword("Test123!");
        dto.setPassword2("Test123!");

        ValidationException exception = assertThrows(ValidationException.class,
                () -> userService.createUser(dto));

        assertTrue(exception.getErrors().containsKey("lastName"));
        assertEquals("No last name given",
                exception.getErrors().get("lastName"));
    }



}
