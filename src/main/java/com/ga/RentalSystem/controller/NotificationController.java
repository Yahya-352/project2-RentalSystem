package com.ga.RentalSystem.controller;

import com.ga.RentalSystem.exceptions.InformationNotFoundException;
import com.ga.RentalSystem.model.User;
import com.ga.RentalSystem.repository.UserRepository;
import com.ga.RentalSystem.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@Tag(name = "Notifications", description = "Real-time booking notifications (Server-Sent Events)")
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    //finds logged in user then calls subscribe method which returns an SSE Emitter(open connection)
    @Operation(summary = "Open a live notification stream for the logged-in user")
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(Authentication authentication) throws IOException {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new InformationNotFoundException("User not found"));
        return notificationService.subscribe(user.getId());
    }
}