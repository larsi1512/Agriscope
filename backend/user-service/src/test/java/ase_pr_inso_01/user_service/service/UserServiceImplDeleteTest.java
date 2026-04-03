package ase_pr_inso_01.user_service.service;


import ase_pr_inso_01.user_service.controller.dto.user.UserCreateDto;
import ase_pr_inso_01.user_service.exception.ConflictException;
import ase_pr_inso_01.user_service.exception.NotFoundException;
import ase_pr_inso_01.user_service.exception.ValidationException;
import ase_pr_inso_01.user_service.model.User;
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
public class UserServiceImplDeleteTest {

  @Autowired
  private UserService userService;

  @Autowired
  private UserRepository userRepository;
  @BeforeEach
  void setUp() {
    userRepository.deleteAll();
  }
  @Test
  void shouldSoftDeleteUserSuccessfully() throws ValidationException, ConflictException {
    UserCreateDto dto = new UserCreateDto();
    dto.setEmail("delete@test.com");
    dto.setFirstName("To");
    dto.setLastName("Delete");
    dto.setPassword("Test123!");
    dto.setPassword2("Test123!");
    userService.createUser(dto);

    User deletedUser = userService.deleteUser("delete@test.com");

    assertNotNull(deletedUser.getDeleted_at());

    User userInDb = userRepository.findUserByEmail("delete@test.com").orElseThrow();
    assertNotNull(userInDb.getDeleted_at());
  }

  @Test
  void shouldFailDeleteNonExistentUser() {
    assertThrows(NotFoundException.class, () -> userService.deleteUser("ghost@test.com"));
  }
}
