package ase_pr_inso_01.user_service.service;

import ase_pr_inso_01.user_service.controller.dto.user.UserCreateDto;
import ase_pr_inso_01.user_service.controller.dto.user.UserDetailsDto;
import ase_pr_inso_01.user_service.controller.dto.user.UserEditDto;
import ase_pr_inso_01.user_service.exception.ConflictException;
import ase_pr_inso_01.user_service.exception.ValidationException;
import ase_pr_inso_01.user_service.model.User;
import ase_pr_inso_01.user_service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
public class UserServiceImplEditUserTest {

    @Autowired
    private UserService userService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private UserRepository userRepository;
    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void shouldEditFirstAndLastNameSuccessfully() throws ValidationException, ConflictException, IOException {
        UserCreateDto createDto = new UserCreateDto();
        createDto.setEmail("editname@test.com");
        createDto.setFirstName("Original");
        createDto.setLastName("Name");
        createDto.setPassword("Test123!");
        createDto.setPassword2("Test123!");
        userService.createUser(createDto);

        UserEditDto editDto = new UserEditDto();
        editDto.setFirstName("NewFirst");
        editDto.setLastName("NewLast");

        User updatedUser = userService.editUser("editname@test.com", editDto, null);

        assertEquals("NewFirst", updatedUser.getFirstName());
        assertEquals("NewLast", updatedUser.getLastName());
    }

    @Test
    void shouldEditPasswordSuccessfully() throws ValidationException, ConflictException, IOException {
        UserCreateDto createDto = new UserCreateDto();
        createDto.setEmail("editpass@test.com");
        createDto.setFirstName("Pass");
        createDto.setLastName("User");
        createDto.setPassword("OldPass123!");
        createDto.setPassword2("OldPass123!");
        userService.createUser(createDto);

        UserEditDto editDto = new UserEditDto();
        editDto.setOldPassword("OldPass123!");
        editDto.setNewPassword("NewPass123!");
        User updatedUser = userService.editUser("editpass@test.com", editDto, null);

        assertTrue(passwordEncoder.matches("NewPass123!", updatedUser.getPassword()));
    }

    @Test
    void shouldFailEditPasswordWithoutOldPassword() throws ValidationException, ConflictException {
        UserCreateDto createDto = new UserCreateDto();
        createDto.setEmail("noold@test.com");
        createDto.setFirstName("Test");
        createDto.setLastName("User");
        createDto.setPassword("OldPass123!");
        createDto.setPassword2("OldPass123!");
        userService.createUser(createDto);

        UserEditDto editDto = new UserEditDto();
        editDto.setOldPassword("");
        editDto.setNewPassword("NewPass123!"); // old password missing

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            userService.editUser("noold@test.com", editDto, null);
        });

        assertEquals("Current password is required to set a new password", ex.getMessage());
    }
    @Test
    void shouldFailEditUserWithWrongOldPassword() throws ValidationException, ConflictException {
        UserCreateDto createDto = new UserCreateDto();
        createDto.setEmail("editfail@test.com");
        createDto.setFirstName("Test");
        createDto.setLastName("User");
        createDto.setPassword("Correct123!");
        createDto.setPassword2("Correct123!");
        userService.createUser(createDto);

        UserEditDto editDto = new UserEditDto();
        editDto.setOldPassword("WrongOld123!");  // wrong password
        editDto.setNewPassword("NewPass123!");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            userService.editUser("editfail@test.com", editDto, null);
        });

        assertEquals("Current password is incorrect", ex.getMessage());
    }


    @Test
    void shouldUploadAndCompressProfileImage() throws Exception {
        UserCreateDto createDto = new UserCreateDto();
        createDto.setEmail("img@test.com");
        createDto.setFirstName("Test");
        createDto.setLastName("User");
        createDto.setPassword("Test123!");
        createDto.setPassword2("Test123!");
        userService.createUser(createDto);

        BufferedImage validImage = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(validImage, "jpg", baos);
        byte[] imageBytes = baos.toByteArray();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                imageBytes
        );

        UserEditDto editDto = new UserEditDto();

        User updatedUser = userService.editUser("img@test.com", editDto, file);

        assertNotNull(updatedUser.getProfileImageBlob());
        assertTrue(updatedUser.getProfileImageBlob().length > 0);
    }

    @Test
    void shouldReturnUserWithBase64Image() throws Exception {
        UserCreateDto createDto = new UserCreateDto();
        createDto.setEmail("img@test.com");
        createDto.setFirstName("Test");
        createDto.setLastName("User");
        createDto.setPassword("Test123!");
        createDto.setPassword2("Test123!");
        userService.createUser(createDto);

        BufferedImage validImage = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(validImage, "jpg", baos);
        byte[] imageBytes = baos.toByteArray();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                imageBytes
        );

        UserEditDto editDto = new UserEditDto();

        User updatedUser = userService.editUser("img@test.com", editDto, file);

        UserDetailsDto getUser = userService.getUserByEmail(updatedUser.getEmail());
        assertNotNull(getUser.getProfilePicture());
    }
}
