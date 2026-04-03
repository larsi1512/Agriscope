package ase_pr_inso_01.user_service.controller.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserLoginDto {
  @NotBlank(message = "Email must not be empty")
  @Email(message = "Must be a valid email")
  private String email;

  @NotBlank(message = "Password must not be empty")
  private String password;
}
