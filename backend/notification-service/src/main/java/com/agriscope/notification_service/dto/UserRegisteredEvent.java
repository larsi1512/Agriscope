package com.agriscope.notification_service.dto;

import lombok.Data;

@Data
public class UserRegisteredEvent {
    private String userId;
    private String email;
    private String firstName;
    private String lastName;
}