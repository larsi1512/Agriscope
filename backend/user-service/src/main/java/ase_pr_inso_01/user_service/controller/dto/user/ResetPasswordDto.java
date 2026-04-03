package ase_pr_inso_01.user_service.controller.dto.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Setter
public class ResetPasswordDto {
  private String password;
  private String confirmPassword;
}
