package com.ga.notification_service.controller;

import com.ga.common.exceptions.ForbiddenException;
import com.ga.notification_service.dto.NotificationRequest;
import com.ga.notification_service.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@Tag(name = "Notifications", description = "Real-time booking notifications (Server-Sent Events)")
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Value("${internal.api-key}")
    private String internalApiKey;

    //finds logged in user then calls subscribe method which returns an SSE Emitter(open connection)
    @Operation(summary = "Open a live notification stream for the logged-in user")
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(Authentication authentication) throws IOException {
        Long userId = (Long) authentication.getPrincipal();
        return notificationService.subscribe(userId);
    }

    @Operation(summary = "Internal: push a notification to a user (service-to-service only)")
    @PostMapping
    public ResponseEntity<Void> send(@RequestHeader("X-Internal-Key") String key,
                                     @RequestBody NotificationRequest request) {
        if (!internalApiKey.equals(key)) {
            throw new ForbiddenException("Invalid internal key");
        }
        notificationService.sendEvent(request.userId(), request.eventName(), request.data());
        return ResponseEntity.accepted().build();
    }
}