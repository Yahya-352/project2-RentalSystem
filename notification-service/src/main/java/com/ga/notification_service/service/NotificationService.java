package com.ga.notification_service.service;

import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Sends real-time notifications to users with Server-Sent Events (SSE).
 * <p>
 * A user can keep several connections open at once, for example from different devices.
 * The connections are kept in memory, so notifications are only delivered to users who are
 * connected at that moment, and nothing is stored for users who are offline.
 * <p>
 * Closed connections are removed as soon as Spring reports them (completion, timeout or
 * error). A heartbeat every 25 seconds keeps idle streams open through proxies such as
 * CloudFront, which close connections after 60 seconds of silence, and also detects clients
 * that disappeared without closing the connection.
 */
@Service
public class NotificationService {
    // a connection lives at most 30 minutes, then the frontend reconnects by itself
    private static final long TIMEOUT_MS = 30 * 60 * 1000L;

    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    /**
     * Opens a notification stream for a user and keeps it registered so that later events
     * can be pushed to it. A {@code CONNECTED} event is sent straight away to confirm that it
     * works.
     *
     * @param userId the id of the user who wants to receive notifications
     * @return the open connection, which Spring keeps alive and streams to the client
     * @throws IOException if the first event cannot be sent
     */
    public SseEmitter subscribe(Long userId) throws IOException {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        //saves the connections
        //quick note: arraylist cant be modified in a loop(concurrent modification exception)
        emitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        //drops the connection from the map as soon as it is closed, times out or fails
        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(() -> remove(userId, emitter));
        emitter.onError(e -> remove(userId, emitter));

        //just to check if we are connected in postman(test)
        try {
            emitter.send(SseEmitter.event().name("CONNECTED").data("Listening for events"));
        } catch (IOException e) {
            remove(userId, emitter);
            throw e;
        }

        //returns the object to spring as an open connection
        return emitter;
    }

    /**
     * Sends an event to every open connection of a user. If the user is not connected,
     * nothing happens. A connection that fails to receive the event is treated as closed
     * and removed.
     *
     * @param userId    the id of the user who should receive the event
     * @param eventName the name of the event, for example {@code BOOKING_APPROVED}
     * @param data      the event data, which is sent as JSON
     */
    @Async("notificationExecutor")
    public void sendEvent(Long userId, String eventName, Object data) {
        //gets all the open connections of this user
        List<SseEmitter> userEmitters = emitters.get(userId);

        //if the user is not listening, nothing to send
        if (userEmitters == null){
            return;
        }

        //pushes the event to each open connection
        for (SseEmitter emitter : userEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(data, MediaType.APPLICATION_JSON));
            } catch (IOException | IllegalStateException e) {
                //the client disconnected, so we drop this connection
                remove(userId, emitter);
            }
        }
    }

    /**
     * Sends a comment line ({@code : ping}) to every open connection. Browsers ignore it, but it
     * keeps proxies from closing idle streams and removes connections whose client is gone.
     */
    @Scheduled(fixedRate = 25_000)
    public void heartbeat() {
        emitters.forEach((userId, userEmitters) -> {
            for (SseEmitter emitter : userEmitters) {
                try {
                    emitter.send(SseEmitter.event().comment("ping"));
                } catch (IOException | IllegalStateException e) {
                    remove(userId, emitter);
                }
            }
        });
    }

    private void remove(Long userId, SseEmitter emitter) {
        emitters.computeIfPresent(userId, (id, list) -> {
            list.remove(emitter);
            return list.isEmpty() ? null : list;
        });
    }
}
