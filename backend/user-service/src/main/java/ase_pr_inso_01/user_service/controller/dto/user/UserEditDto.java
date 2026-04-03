package ase_pr_inso_01.user_service.controller.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserEditDto {
  @NotBlank(message = "First name must not be empty")
  private String firstName;

  @NotBlank(message = "Last name must not be empty")
  private String lastName;

  private String oldPassword;
  private String newPassword;
}
