package ase_pr_inso_01.user_service.validation;

import ase_pr_inso_01.user_service.controller.dto.user.ResetPasswordDto;
import ase_pr_inso_01.user_service.controller.dto.user.UserCreateDto;
import ase_pr_inso_01.user_service.exception.ConflictException;
import ase_pr_inso_01.user_service.exception.NotFoundException;
import ase_pr_inso_01.user_service.exception.ValidationException;
import ase_pr_inso_01.user_service.model.User;
import ase_pr_inso_01.user_service.repository.UserRepository;
import jakarta.validation.Validation;
import org.springframework.cglib.core.Local;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;

@Component
public class UserValidator {
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  public UserValidator(UserRepository userRepository, PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
  }

  public void validateForLogin(String email) throws ValidationException {
    Map<String, String> validationErrors = new HashMap<>();
    if (email.isEmpty()) {
      validationErrors.put("email", "No email given");
    }
    Optional<User> userOptional = userRepository.findUserByEmail(email);
    if (userOptional.isEmpty()) {
      throw new NotFoundException("User with given e-mail was not found!");
    }
    User user = userOptional.get();
    if (user.getDeleted_at() != null) {
      validationErrors.put("user", "Account is set for deletion");
    }
    if (!validationErrors.isEmpty()) {
      throw new ValidationException("Validation failed", validationErrors);
    }
  }

  public void validateForSignUp(UserCreateDto toCreate) throws ValidationException, ConflictException {
    Map<String, String> validationErrors = new HashMap<>();
    Map<String, String> conflictErrors = new HashMap<>();
    if (toCreate.getEmail().isEmpty()) {
      validationErrors.put("email", "No email given");
    } else if (userRepository.existsByEmail(toCreate.getEmail())) {
      conflictErrors.put("email", "User with the given email already exists");
    }
    if (toCreate.getLastName().isEmpty()) {
      validationErrors.put("lastName", "No last name given");
    }
    if (toCreate.getFirstName().isEmpty()) {
      validationErrors.put("firstName", "No first name given");
    }

    if (new String(toCreate.getPassword()).isEmpty()) {
      validationErrors.put("password", "No password given");
    } else if (new String(toCreate.getPassword()).length() < 8) {
      validationErrors.put("password", "Password is too short");
    } else if (!new String(toCreate.getPassword()).matches("(?=.*[A-Z]).*")) {
      validationErrors.put("password", "Password must contain at least one uppercase letter");
    } else if (!new String(toCreate.getPassword()).matches("(?=.*\\d).*")) {
      validationErrors.put("password", "Password must contain at least one number");
    } else if (!new String(toCreate.getPassword()).matches("(?=.*[.,\\-_!\"§$%&/()=?`*+\\\\]).*")) {
      validationErrors.put("password", "Password must contain at least one special character");
    }

    if (new String(toCreate.getPassword2()).isEmpty()) {
      validationErrors.put("password2", "No password given");
    } else if (new String(toCreate.getPassword2()).length() < 8) {
      validationErrors.put("password2", "Password is too short");
    } else if (!new String(toCreate.getPassword2()).matches("(?=.*[A-Z]).*")) {
      validationErrors.put("password2", "Password must contain at least one uppercase letter");
    } else if (!new String(toCreate.getPassword2()).matches("(?=.*\\d).*")) {
      validationErrors.put("password2", "Password must contain at least one number");
    } else if (!new String(toCreate.getPassword2()).matches("(?=.*[.,\\-_!\"§$%&/()=?`*+\\\\]).*")) {
      validationErrors.put("password2", "Password must contain at least one special character");
    }
    if(!toCreate.getPassword().equals(toCreate.getPassword2())) {
      validationErrors.put("password2", "Password not matching");
    }
    if (!validationErrors.isEmpty()) {
      throw new ValidationException("Validation failed", validationErrors);
    }
    if (!conflictErrors.isEmpty()) {
      throw new ConflictException("Validation failed", conflictErrors);
    }

  }

  public void ValidateForPasswordReset(String email, ResetPasswordDto dto) throws ValidationException {
    Map<String, String> validationErrors = new HashMap<>();

    if (!dto.getPassword().equals(dto.getConfirmPassword())) {
      validationErrors.put("confirmPassword", "Passwords do not match");
    }

    String newPassword = dto.getPassword();
    if (newPassword.isEmpty()) {
      validationErrors.put("password", "No password given");
    } else if (newPassword.length() < 8) {
      validationErrors.put("password", "Password is too short (min 8 characters)");
    } else if (!newPassword.matches("(?=.*[A-Z]).*")) {
      validationErrors.put("password", "Must contain at least one uppercase letter");
    } else if (!newPassword.matches("(?=.*\\d).*")) {
      validationErrors.put("password", "Must contain at least one number");
    } else if (!newPassword.matches("(?=.*[.,\\-_!\"§$%&/()=?`*+\\\\]).*")) {
      validationErrors.put("password", "Must contain at least one special character");
    }

    User user = userRepository.findUserByEmail(email)
            .orElseThrow(() -> new NotFoundException("User not found"));

    if (passwordEncoder.matches(newPassword, user.getPassword())) {
      validationErrors.put("password", "New password cannot be the same as your current password");
    }

    if (!validationErrors.isEmpty()) {
      throw new ValidationException("Validation failed", validationErrors);
    }
  }
}
