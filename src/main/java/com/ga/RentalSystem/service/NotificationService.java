package com.ga.RentalSystem.service;

import org.springframework.http.MediaType;
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
 */
@Service
public class NotificationService {
    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    /**
     * Opens a notification stream for a user and keeps it registered so that later events
     * can be pushed to it. The connection has no timeout, and a {@code CONNECTED} event is
     * sent straight away to confirm that it works.
     *
     * @param userId the id of the user who wants to receive notifications
     * @return the open connection, which Spring keeps alive and streams to the client
     * @throws IOException if the first event cannot be sent
     */
    public SseEmitter subscribe(Long userId) throws IOException {
        //creations connection object with 0 timeout
        SseEmitter emitter = new SseEmitter(0L);
        //saves the connections
        //quick note: arraylist cant be modified in a loop(concurrent modification exception)
        emitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        //just to check if we are connected in postman(test)
        emitter.send(SseEmitter.event().name("CONNECTED").data("Listening for events"));

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
            } catch (IOException e) {
                //the client disconnected, so we drop this connection
                userEmitters.remove(emitter);
            }
        }


    }
}