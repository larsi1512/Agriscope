package ase_pr_inso_01.user_service.controller.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class UserCreateDto {
  @NotBlank(message = "Email must not be empty")
  @Email(message = "Invalid email format")
  private String email;

  @NotBlank(message = "First name must not be empty")
  private String firstName;

  @NotBlank(message = "Last name must not be empty")
  private String lastName;

  @NotBlank(message = "Password must not be empty")
  private String password;

  @NotBlank(message = "Password must not be empty")
  private String password2;
}
