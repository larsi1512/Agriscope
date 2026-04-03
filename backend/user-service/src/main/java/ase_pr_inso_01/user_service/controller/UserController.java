package ase_pr_inso_01.user_service.controller;

import ase_pr_inso_01.user_service.controller.dto.user.ResetPasswordDto;
import ase_pr_inso_01.user_service.controller.dto.user.UserCreateDto;
import ase_pr_inso_01.user_service.controller.dto.user.UserDetailsDto;
import ase_pr_inso_01.user_service.controller.dto.user.UserEditDto;
import ase_pr_inso_01.user_service.exception.ConflictException;
import ase_pr_inso_01.user_service.exception.ValidationException;
import ase_pr_inso_01.user_service.model.User;
import ase_pr_inso_01.user_service.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;

@RestController
@RequestMapping(value = "/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public void createUser(@RequestBody UserCreateDto user) throws ValidationException, ConflictException {
        //LOGGER.info("POST /api/users {}", user);
        userService.createUser(user);
    }

    @GetMapping("/me")
    public ResponseEntity<UserDetailsDto> getCurrentUser(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        String email = principal.getName();

        UserDetailsDto user = userService.getUserByEmail(email);

        return ResponseEntity.ok(user);
    }

    // TODO: Probably unnecessary
    @GetMapping("/by-email/{email}")
    public ResponseEntity<UserDetailsDto> getUserByEmail(@PathVariable String email) {
        //TODO: Add exception handling
        UserDetailsDto userDto = userService.getUserByEmail(email);
        return ResponseEntity.ok(userDto);
    }

    // TODO: Probably unnecessary
    @GetMapping("/{userId}")
    public ResponseEntity<UserDetailsDto> getUser(@PathVariable String userId) {
        UserDetailsDto userDto = userService.getUserById(userId);
        return ResponseEntity.ok(userDto);
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateProfile(Principal principal,
                                           @RequestPart("userData") UserEditDto dto,
                                           @RequestPart(value = "file", required = false) MultipartFile file) {
        try {
            String email = principal.getName();
            User updatedUser = userService.editUser(email, dto, file);
            return ResponseEntity.ok(updatedUser);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("Error with Image upload");
        }
        catch (Exception e) {
            return ResponseEntity.internalServerError().body("An error occurred while updating profile");
        }
    }

    @PutMapping("/me/delete")
    public ResponseEntity<?> deleteProfile(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        try {
            String email = principal.getName();
            User deleteUser = userService.deleteUser(email);
            return ResponseEntity.ok(deleteUser);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("An error occurred while deleting profile");
        }
    }

    @GetMapping("/password-reset")
    public ResponseEntity<?> getResetPasswordEmail(@RequestParam String email) {
        try {
            UserDetailsDto applicationUser = userService.getUserByEmail(email);
            if(applicationUser == null)
                return ResponseEntity.status(401).build();
            userService.resetPassword(email);
            return ResponseEntity.ok(null);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("An error occurred while resetting password");
        }
    }

    @PostMapping("/password-reset")
    public ResponseEntity<?> resetUserPassword(@RequestParam("token") String token, @RequestBody ResetPasswordDto dto) throws ValidationException {
        userService.completePasswordReset(token, dto);
        return ResponseEntity.ok("Password updated successfully");
    }
}
