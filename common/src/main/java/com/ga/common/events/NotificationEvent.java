package com.ga.common.events;

public record NotificationEvent(Long userId, String eventName,
                                Object data) {
}
