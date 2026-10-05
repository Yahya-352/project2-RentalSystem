package com.ga.RentalSystem.service;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class NotificationService {
    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long userId) throws IOException {
        //creations connection object with 0 timeout
        SseEmitter emitter = new SseEmitter(0L);
        //saves the connections
        emitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        //just to check if we are connected in postman(test)
        emitter.send(SseEmitter.event().name("CONNECTED").data("Listening for events"));

        //returns the object to spring as an open connection
        return emitter;
    }

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
