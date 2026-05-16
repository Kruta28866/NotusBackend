package com.notus.backend.realtime;

import com.notus.backend.realtime.dto.TeacherRealtimeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class TeacherRealtimeService {

    private static final Logger log = LoggerFactory.getLogger(TeacherRealtimeService.class);
    private static final long TIMEOUT_MS = 30L * 60L * 1000L;

    private final Map<String, CopyOnWriteArrayList<SseEmitter>> emittersByTeacherUid = new ConcurrentHashMap<>();

    public SseEmitter subscribe(String teacherUid) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        emittersByTeacherUid.computeIfAbsent(teacherUid, ignored -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> remove(teacherUid, emitter));
        emitter.onTimeout(() -> remove(teacherUid, emitter));
        emitter.onError(error -> remove(teacherUid, emitter));

        sendToEmitter(teacherUid, emitter, "realtime.connected", TeacherRealtimeEvent.of(
                "realtime.connected",
                Map.of("teacherUid", teacherUid)
        ));

        return emitter;
    }

    public void publishToTeacher(String teacherUid, String eventName, TeacherRealtimeEvent event) {
        List<SseEmitter> emitters = emittersByTeacherUid.getOrDefault(teacherUid, new CopyOnWriteArrayList<>());
        for (SseEmitter emitter : emitters) {
            sendToEmitter(teacherUid, emitter, eventName, event);
        }
    }

    private void sendToEmitter(String teacherUid, SseEmitter emitter, String eventName, TeacherRealtimeEvent event) {
        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(event));
        } catch (IOException | IllegalStateException ex) {
            log.debug("Removing broken realtime emitter for teacher {}", teacherUid);
            remove(teacherUid, emitter);
        }
    }

    private void remove(String teacherUid, SseEmitter emitter) {
        List<SseEmitter> emitters = emittersByTeacherUid.get(teacherUid);
        if (emitters == null) {
            return;
        }
        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            emittersByTeacherUid.remove(teacherUid);
        }
    }
}
