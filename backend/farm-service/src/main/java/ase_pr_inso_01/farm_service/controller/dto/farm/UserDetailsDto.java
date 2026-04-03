package ase_pr_inso_01.farm_service.controller.dto.farm;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Setter
public class UserDetailsDto {
    private String id;

    @NotBlank(message = "Email must not be empty")
    @Email(message = "Invalid email format")
    private String email;
}
