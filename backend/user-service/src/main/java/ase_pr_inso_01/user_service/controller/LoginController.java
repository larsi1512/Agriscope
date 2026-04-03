package ase_pr_inso_01.user_service.controller;

import ase_pr_inso_01.user_service.controller.dto.user.UserLoginDto;
import ase_pr_inso_01.user_service.exception.ConflictException;
import ase_pr_inso_01.user_service.exception.ValidationException;
import ase_pr_inso_01.user_service.service.UserService;
import jakarta.annotation.security.PermitAll;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping(value = "/api/authentication")
public class LoginController {
  private final UserService userService;

  public LoginController(UserService userService) {
    this.userService = userService;
  }

  @PermitAll
  @PostMapping
  public ResponseEntity<Map<String, String>> login(@RequestBody UserLoginDto userLoginDto) throws ValidationException, ConflictException {
    String token = userService.login(userLoginDto);
    Map<String, String> tokenResponse = new HashMap<>();
    tokenResponse.put("token", token);
    return ResponseEntity.ok(tokenResponse);
  }

}
