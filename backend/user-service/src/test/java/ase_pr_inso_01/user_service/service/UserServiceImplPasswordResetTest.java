package ase_pr_inso_01.user_service.service;

import ase_pr_inso_01.user_service.controller.dto.user.ResetPasswordDto;
import ase_pr_inso_01.user_service.controller.dto.user.UserCreateDto;
import ase_pr_inso_01.user_service.controller.dto.user.UserLoginDto;
import ase_pr_inso_01.user_service.exception.ConflictException;
import ase_pr_inso_01.user_service.exception.NotFoundException;
import ase_pr_inso_01.user_service.exception.ValidationException;
import ase_pr_inso_01.user_service.model.EmailRequest;
import ase_pr_inso_01.user_service.repository.UserRepository;
import ase_pr_inso_01.user_service.security.JwtUtils;
import ase_pr_inso_01.user_service.service.impl.PasswordResetProducer;
import ase_pr_inso_01.user_service.validation.UserValidator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
public class UserServiceImplPasswordResetTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtils jwtUtils;

    @MockBean
    private PasswordResetProducer passwordResetProducer;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }
    @Mock
    private UserValidator userValidator;
    @Mock
    private PasswordEncoder passwordEncoder;
    private final String TEST_EMAIL = "test@example.com";

    @Test
    void ShouldSendResetEmailSuccessfully() {

        final EmailRequest[] capturedRequest = new EmailRequest[1];

        RabbitTemplate fakeTemplate = new RabbitTemplate() {
            @Override
            public void convertAndSend(String exchange, String routingKey, Object message) {
                capturedRequest[0] = (EmailRequest) message;
            }
        };

        PasswordResetProducer producer = new PasswordResetProducer(fakeTemplate);

        String testEmail = "farmer@test.com";
        String testToken = "abc-123";
        producer.sendResetEmail(testEmail, testToken);

        EmailRequest result = capturedRequest[0];
        assertNotNull(result);
        assertEquals(testEmail, result.getTo());
        assertEquals("Password Reset Request - Agriscope", result.getSubject());

        assertTrue(result.getBody().contains("token=abc-123"));
        assertTrue(result.getBody().contains(testEmail));
    }
    @Test
    void shouldInitiatePasswordResetSuccessfully() throws ValidationException, ConflictException {
        String email = "reset_init@test.com";
        createUser(email, "Reset", "User", "OldPass123!");

        userService.resetPassword(email);

        verify(passwordResetProducer, times(1)).sendResetEmail(eq(email), anyString());
    }

    @Test
    void shouldThrowNotFoundWhenResettingNonExistentEmail() {
        assertThrows(NotFoundException.class, () -> {
            userService.resetPassword("ghost@test.com");
        });
    }

    @Test
    void shouldCompletePasswordResetSuccessfully() throws ValidationException, ConflictException {
        String email = "reset_complete@test.com";
        createUser(email, "Reset", "Complete", "OldPass123!");

        String token = jwtUtils.generatePasswordResetToken(email);

        ResetPasswordDto resetDto = new ResetPasswordDto();
        resetDto.setPassword("NewSecurePass1!");
        resetDto.setConfirmPassword("NewSecurePass1!");

        userService.completePasswordReset(token, resetDto);

        UserLoginDto loginDto = new UserLoginDto();
        loginDto.setEmail(email);
        loginDto.setPassword("NewSecurePass1!");

        String jwt = userService.login(loginDto);
        assertNotNull(jwt);
        assertFalse(jwt.isEmpty());

        UserLoginDto oldLoginDto = new UserLoginDto();
        oldLoginDto.setEmail(email);
        oldLoginDto.setPassword("OldPass123!");

        assertThrows(ValidationException.class, () -> userService.login(oldLoginDto));
    }

    @Test
    void shouldFailResetWithInvalidToken() {
        ResetPasswordDto resetDto = new ResetPasswordDto();
        resetDto.setPassword("NewPass123!");
        resetDto.setConfirmPassword("NewPass123!");

        String invalidToken = "invalid.token.string";

        ValidationException ex = assertThrows(ValidationException.class, () -> {
            userService.completePasswordReset(invalidToken, resetDto);
        });

        assertTrue(ex.getMessage().contains("invalid or has expired"));
    }

    @Test
    void shouldFailResetWhenNewPasswordSameAsOld() throws ValidationException, ConflictException {
        String email = "reuse_pass@test.com";
        String oldPass = "OldPass123!";
        createUser(email, "Reuse", "Test", oldPass);
        String token = jwtUtils.generatePasswordResetToken(email);

        ResetPasswordDto resetDto = new ResetPasswordDto();
        resetDto.setPassword(oldPass); // Trying to use the exact same password
        resetDto.setConfirmPassword(oldPass);

        ValidationException ex = assertThrows(ValidationException.class, () -> {
            userService.completePasswordReset(token, resetDto);
        });

        assertTrue(ex.getErrors().containsKey("password"));
        assertEquals("New password cannot be the same as your current password",
                ex.getErrors().get("password"));
    }

    @Test
    void shouldFailResetWhenPasswordTooShort() throws ValidationException, ConflictException {
        String email = "short_pass@test.com";
        createUser(email, "Short", "Pass", "Valid123!");
        String token = jwtUtils.generatePasswordResetToken(email);

        ResetPasswordDto resetDto = new ResetPasswordDto();
        resetDto.setPassword("Short1!"); // Only 7 chars
        resetDto.setConfirmPassword("Short1!");

        ValidationException ex = assertThrows(ValidationException.class, () ->
                userService.completePasswordReset(token, resetDto));

        assertEquals("Password is too short (min 8 characters)", ex.getErrors().get("password"));
    }

    @Test
    void shouldFailResetWhenPasswordMissingSpecialChar() throws ValidationException, ConflictException {
        String email = "nospecial@test.com";
        createUser(email, "No", "Special", "Valid123!");
        String token = jwtUtils.generatePasswordResetToken(email);

        ResetPasswordDto resetDto = new ResetPasswordDto();
        resetDto.setPassword("NoSpecialChar1");
        resetDto.setConfirmPassword("NoSpecialChar1");

        ValidationException ex = assertThrows(ValidationException.class, () ->
                userService.completePasswordReset(token, resetDto));

        assertEquals("Must contain at least one special character", ex.getErrors().get("password"));
    }

    @Test
    void shouldFailResetWhenPasswordsDoNotMatch() throws ValidationException, ConflictException {
        String email = "mismatch@test.com";
        createUser(email, "Mis", "Match", "Valid123!");
        String token = jwtUtils.generatePasswordResetToken(email);

        ResetPasswordDto resetDto = new ResetPasswordDto();
        resetDto.setPassword("ValidNewPass1!");
        resetDto.setConfirmPassword("DifferentPass1!");

        ValidationException ex = assertThrows(ValidationException.class, () ->
                userService.completePasswordReset(token, resetDto));

        assertEquals("Passwords do not match", ex.getErrors().get("confirmPassword"));
    }

    // --- Helper Method ---
    private void createUser(String email, String first, String last, String password) throws ValidationException, ConflictException {
        UserCreateDto dto = new UserCreateDto();
        dto.setEmail(email);
        dto.setFirstName(first);
        dto.setLastName(last);
        dto.setPassword(password);
        dto.setPassword2(password);
        userService.createUser(dto);
    }
}