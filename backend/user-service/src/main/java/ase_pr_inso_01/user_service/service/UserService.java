package ase_pr_inso_01.user_service.service;

import ase_pr_inso_01.user_service.controller.dto.user.*;
import ase_pr_inso_01.user_service.exception.ConflictException;
import ase_pr_inso_01.user_service.exception.ValidationException;
import ase_pr_inso_01.user_service.model.User;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface UserService {
  User createUser(UserCreateDto dto) throws ValidationException, ConflictException;

  String login(UserLoginDto dto) throws ConflictException, ValidationException;

    UserDetailsDto getUserByEmail(String email);

    UserDetailsDto getUserById(String userId);

    User editUser(String email, UserEditDto updateUserDto, MultipartFile file) throws IOException;
    User deleteUser(String email);
    void resetPassword(String email);
    void completePasswordReset(String token, ResetPasswordDto dto) throws ValidationException;
}
