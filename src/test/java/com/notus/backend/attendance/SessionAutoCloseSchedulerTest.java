package com.notus.backend.attendance;

import com.notus.backend.quiz.QuizAssignment;
import com.notus.backend.quiz.QuizAssignmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionAutoCloseSchedulerTest {

    @Mock
    private AttendanceSessionRepository sessionRepo;

    @Mock
    private QuizAssignmentRepository assignmentRepo;

    @InjectMocks
    private SessionAutoCloseScheduler scheduler;

    @Test
    void closeExpiredSessions_closesActiveSessionAndDeactivatesAssignments() {
        AttendanceSession session = new AttendanceSession();
        session.setId(10L);
        session.setActive(true);
        session.setEndsAt(Instant.now().minusSeconds(60));

        QuizAssignment assignment = new QuizAssignment();
        assignment.setId(20L);
        assignment.setActive(true);
        assignment.setSessionId(10L);

        when(sessionRepo.findByActiveTrueAndEndsAtLessThanEqual(any(Instant.class)))
                .thenReturn(List.of(session));
        when(assignmentRepo.findAllBySessionIdAndActiveTrue(10L))
                .thenReturn(List.of(assignment));

        scheduler.closeExpiredSessions();

        assert !session.isActive();
        verify(sessionRepo).save(session);

        assert !assignment.isActive();
        verify(assignmentRepo).save(assignment);
    }

    @Test
    void closeExpiredSessions_noExpiredSessions_doesNothing() {
        when(sessionRepo.findByActiveTrueAndEndsAtLessThanEqual(any(Instant.class)))
                .thenReturn(List.of());

        scheduler.closeExpiredSessions();

        verify(sessionRepo, never()).save(any());
        verify(assignmentRepo, never()).findAllBySessionIdAndActiveTrue(any());
        verify(assignmentRepo, never()).save(any());
    }

    @Test
    void closeExpiredSessions_sessionWithNoAssignments_closesSessionOnly() {
        AttendanceSession session = new AttendanceSession();
        session.setId(11L);
        session.setActive(true);
        session.setEndsAt(Instant.now().minusSeconds(120));

        when(sessionRepo.findByActiveTrueAndEndsAtLessThanEqual(any(Instant.class)))
                .thenReturn(List.of(session));
        when(assignmentRepo.findAllBySessionIdAndActiveTrue(11L))
                .thenReturn(List.of());

        scheduler.closeExpiredSessions();

        assert !session.isActive();
        verify(sessionRepo).save(session);
        verify(assignmentRepo, never()).save(any());
    }
}
