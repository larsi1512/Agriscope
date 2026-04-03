package com.agriscope.notification_service.messaging;

import com.agriscope.notification_service.config.RabbitMQConfig;
import com.agriscope.notification_service.dto.UserRegisteredEvent;
import com.agriscope.notification_service.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationConsumer {

    private final EmailService emailService;

    @RabbitListener(queues = RabbitMQConfig.USER_REGISTERED_QUEUE)
    public void handleUserRegistration(UserRegisteredEvent event) {
        log.info("Processing welcome email for new user: {}", event.getEmail());

        emailService.sendWelcomeEmail(
                event.getEmail(),
                event.getFirstName()
        );
    }
}