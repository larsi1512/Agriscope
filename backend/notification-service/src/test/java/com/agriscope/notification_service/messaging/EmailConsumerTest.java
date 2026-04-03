package com.agriscope.notification_service.messaging;

import com.agriscope.notification_service.model.EmailRequest;
import com.agriscope.notification_service.service.EmailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailConsumerTest {

    @Mock
    private EmailService emailService;

    @InjectMocks
    private EmailConsumer consumer;

    @Test
    @DisplayName("Should pass email request to service")
    void handleEmailRequest_Success() {
        EmailRequest request = new EmailRequest("test@test.com", "Subject", "Body");

        consumer.handleEmailRequest(request);

        verify(emailService).sendResetEmail("test@test.com", "Subject", "Body");
    }
}