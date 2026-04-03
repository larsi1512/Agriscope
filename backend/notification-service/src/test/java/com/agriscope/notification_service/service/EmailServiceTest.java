package com.agriscope.notification_service.service;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender emailSender;

    @Mock
    private EmailTemplateService emailTemplateService;

    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        lenient().when(emailSender.createMimeMessage()).thenReturn(mimeMessage);
    }

    @Test
    @DisplayName("Should send ALERT email (HTML MimeMessage)")
    void sendAlertEmail_Success() {
        emailService.sendAlertEmail("test@test.com", "Flood Warning", "<h1>Water level high</h1>");

        verify(emailSender, times(1)).send(mimeMessage);
    }

    @Test
    @DisplayName("Should send GENERIC/RESET email (HTML MimeMessage)")
    void sendGenericEmail_Success() {
        emailService.sendResetEmail("user@test.com", "Welcome", "Hello User");

        verify(emailSender, times(1)).send(mimeMessage);
    }

    @Test
    @DisplayName("Should send WELCOME email")
    void sendWelcomeEmail_Success() {
        when(emailTemplateService.buildWelcomeEmailHtml(anyString())).thenReturn("<html>Welcome</html>");

        emailService.sendWelcomeEmail("new@test.com", "John");
        verify(emailSender, times(1)).send(mimeMessage);
    }

    @Test
    @DisplayName("Should handle exception gracefully when sending fails")
    void sendEmail_Exception_ShouldNotThrow() {
        doThrow(new RuntimeException("SMTP unavailable")).when(emailSender).send(any(MimeMessage.class));

        assertDoesNotThrow(() ->
                emailService.sendAlertEmail("fail@test.com", "Sub", "Body")
        );
    }
}