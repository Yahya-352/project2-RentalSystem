package com.ga.notification_service.dto;

public record NotificationRequest(Long userId , String eventName , Object data) {

}
