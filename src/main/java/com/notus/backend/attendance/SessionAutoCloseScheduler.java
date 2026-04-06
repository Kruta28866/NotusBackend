package com.notus.backend.attendance;

import com.notus.backend.quiz.QuizAssignment;
import com.notus.backend.quiz.QuizAssignmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionAutoCloseScheduler {

    private final AttendanceSessionRepository sessionRepo;
    private final QuizAssignmentRepository assignmentRepo;

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void closeExpiredSessions() {
        List<AttendanceSession> expired = sessionRepo.findByActiveTrueAndEndsAtLessThanEqual(Instant.now());

        int closedSessions = 0;
        int closedAssignments = 0;

        for (AttendanceSession session : expired) {
            session.setActive(false);
            sessionRepo.save(session);
            closedSessions++;

            List<QuizAssignment> activeAssignments = assignmentRepo.findAllBySessionIdAndActiveTrue(session.getId());
            for (QuizAssignment assignment : activeAssignments) {
                assignment.setActive(false);
                assignmentRepo.save(assignment);
                closedAssignments++;
            }
        }

        if (closedSessions > 0) {
            log.info("Auto-closed {} session(s) and {} quiz assignment(s)", closedSessions, closedAssignments);
        }
    }
}
