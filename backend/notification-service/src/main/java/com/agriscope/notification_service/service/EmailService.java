package com.agriscope.notification_service.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender emailSender;

    private final EmailTemplateService emailTemplateService;

    @Async
    public void sendAlertEmail(String toEmail, String subject, String htmlBody) {
        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("noreply@agriscope.com");
            helper.setTo(toEmail);
            helper.setSubject("FARM ALERT: " + formatSubject(subject));
            helper.setText(htmlBody, true);

            emailSender.send(message);
            log.info("HTML alert email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send alert email. Error: {}", e.getMessage());
        }
    }

    @Async
    public void sendWelcomeEmail(String to, String firstName) {
        try {
            String htmlBody = emailTemplateService.buildWelcomeEmailHtml(firstName);

            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("noreply@agriscope.com");
            helper.setTo(to);
            helper.setSubject("Welcome to Agriscope, " + firstName + "!");
            helper.setText(htmlBody, true);

            emailSender.send(message);
            log.info("Welcome email sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send welcome email to {}. Error: {}", to, e.getMessage());
        }
    }

    @Async
    public void sendResetEmail(String to, String subject, String body) {
        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("noreply@agriscope.com");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true);

            emailSender.send(message);
            log.info("Generic email sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send generic email. Error: {}", e.getMessage());
        }
    }

    private String formatSubject(String subject) {
        if (subject == null || subject.trim().isEmpty()) {
            return "Alert";
        }

        String[] words = subject.replace("_", " ").toLowerCase().split(" ");
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1))
                        .append(" ");
            }
        }
        return result.toString().trim();
    }
}