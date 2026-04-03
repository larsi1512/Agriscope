package com.agriscope.notification_service.messaging;

import com.agriscope.notification_service.config.RabbitMQConfig;
import com.agriscope.notification_service.model.EmailRequest;
import com.agriscope.notification_service.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailConsumer {

    private final EmailService emailService;

    @RabbitListener(queues = RabbitMQConfig.EMAIL_QUEUE)
    public void handleEmailRequest(EmailRequest request) {
        log.info("Received  email request for: {}", request.getTo());

        emailService.sendResetEmail(
                request.getTo(),
                request.getSubject(),
                request.getBody()
        );
    }
}