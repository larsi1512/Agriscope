package ase_pr_inso_01.user_service.service.impl;

import ase_pr_inso_01.user_service.config.RabbitMQConfig;
import ase_pr_inso_01.user_service.controller.dto.user.*;
import ase_pr_inso_01.user_service.dto.UserRegisteredEvent;
import ase_pr_inso_01.user_service.exception.ConflictException;
import ase_pr_inso_01.user_service.exception.NotFoundException;
import ase_pr_inso_01.user_service.exception.ValidationException;
import ase_pr_inso_01.user_service.model.User;
import ase_pr_inso_01.user_service.repository.UserRepository;
import ase_pr_inso_01.user_service.security.JwtUtils;
import ase_pr_inso_01.user_service.service.UserService;
import ase_pr_inso_01.user_service.validation.UserValidator;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.cglib.core.Local;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserValidator userValidator;
    private final JwtUtils jwtUtils;
    private final RabbitTemplate rabbitTemplate;

    private final PasswordResetProducer resetProducer;

    public UserServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           UserValidator userValidator,
                           JwtUtils jwtUtils,
                           PasswordResetProducer resetProducer,
                           RabbitTemplate rabbitTemplate) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userValidator = userValidator;
        this.jwtUtils = jwtUtils;
        this.resetProducer = resetProducer;
        this.rabbitTemplate = rabbitTemplate;
    }


    @Override
    public User createUser(UserCreateDto dto) throws ValidationException, ConflictException {

        userValidator.validateForSignUp(dto);

        User user = new User();
        user.setEmail(dto.getEmail());
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());

        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setPassword2(passwordEncoder.encode(dto.getPassword2()));
        user.setDeleted_at(null);

        User savedUser = userRepository.save(user);

        try {
            UserRegisteredEvent event = new UserRegisteredEvent(
                    savedUser.getId(),
                    savedUser.getEmail(),
                    savedUser.getFirstName(),
                    savedUser.getLastName()
            );

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EMAIL_EXCHANGE,
                    RabbitMQConfig.USER_REGISTERED_ROUTING_KEY,
                    event
            );
        } catch (Exception e) {
            System.err.println("Failed to send RabbitMQ message: " + e.getMessage());
        }

        return savedUser;
    }

    @Override
    public String login(UserLoginDto dto) throws ConflictException, ValidationException {
        userValidator.validateForLogin(dto.getEmail());

        User user = userRepository.findUserByEmail(dto.getEmail())
                .orElseThrow(() -> new NotFoundException(
                        "User not found"
                ));

        String rawPassword = new String(dto.getPassword());

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new ValidationException(
                    "Login failed",
                    Map.of("password", "Wrong password")
            );
        }

        return jwtUtils.generateToken(user.getEmail());
    }

    @Override
    public UserDetailsDto getUserById(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
        String base64Image = null;

        if (user.getProfileImageBlob() != null && user.getProfileImageBlob().length > 0) {
            base64Image = Base64.getEncoder().encodeToString(user.getProfileImageBlob());
        }

        return new UserDetailsDto(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(), base64Image);
    }

    @Override
    public User editUser(String email, UserEditDto dto, MultipartFile file) throws IOException {
        User user = userRepository.findUserByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found: " + email));

        if (dto.getFirstName() != null && !dto.getFirstName().isBlank()) {
            user.setFirstName(dto.getFirstName());
        }

        if (dto.getLastName() != null && !dto.getLastName().isBlank()) {
            user.setLastName(dto.getLastName());
        }

        if(dto.getNewPassword() != null && !dto.getNewPassword().isBlank()){
            if(dto.getOldPassword() == null || dto.getOldPassword().isBlank()) {
                throw new IllegalArgumentException("Current password is required to set a new password");
            }
            if(!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
                throw new IllegalArgumentException("Current password is incorrect");
            }
            user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        }

        if (file != null && !file.isEmpty()) {
            byte[] compressedBytes = compressImage(file);
            user.setProfileImageBlob(compressedBytes);
        }
        return userRepository.save(user);
    }
   @Override
   public User deleteUser(String email) {
        User user = userRepository.findUserByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found: " + email));

        user.setDeleted_at(LocalDate.now());
        userRepository.save(user);
        return user;
    }


    public UserDetailsDto getUserByEmail(String email) {
        User user = userRepository.findUserByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found: " + email));

        String base64Image = null;

        if (user.getProfileImageBlob() != null && user.getProfileImageBlob().length > 0) {
            base64Image = Base64.getEncoder().encodeToString(user.getProfileImageBlob());
        }

        return new UserDetailsDto(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(), base64Image);
    }

    @Override
    public void resetPassword(String email) {
        User user = userRepository.findUserByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found: " + email));

        String resetToken = jwtUtils.generatePasswordResetToken(email);
        resetProducer.sendResetEmail(email, resetToken);
    }

    @Override
    public void completePasswordReset(String token, ResetPasswordDto dto) throws ValidationException{

        if (!jwtUtils.validateJwtToken(token)) {
            throw new ValidationException(
                    "The password reset link is invalid or has expired.",
                    "token",
                    "Expired"
            );
        }

        String email = jwtUtils.getUsernameFromJwt(token);

        userValidator.ValidateForPasswordReset(email, dto);

        User user = userRepository.findUserByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));

        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        userRepository.save(user);
    }

    private byte[] compressImage(MultipartFile file) throws IOException {
        BufferedImage originalImage = ImageIO.read(file.getInputStream());

        int targetWidth = 400;
        int targetHeight = (originalImage.getHeight() * targetWidth) / originalImage.getWidth();

        Image resultingImage = originalImage.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);
        BufferedImage outputImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);

        Graphics2D g2d = outputImage.createGraphics();
        g2d.drawImage(resultingImage, 0, 0, null);
        g2d.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        ImageOutputStream ios = ImageIO.createImageOutputStream(baos);
        writer.setOutput(ios);

        ImageWriteParam param = writer.getDefaultWriteParam();
        if (param.canWriteCompressed()) {
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(0.7f); // Adjust this to balance quality vs size
        }

        writer.write(null, new IIOImage(outputImage, null, null), param);

        writer.dispose();
        ios.close();

        return baos.toByteArray();
    }
}
