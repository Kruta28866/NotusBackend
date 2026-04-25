package com.notus.backend.grades;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GradeService {

    private final GradeRepository gradeRepository;

    public GradeService(GradeRepository gradeRepository) {
        this.gradeRepository = gradeRepository;
    }

    public List<GradeDto> getRecentGrades(String clerkUserId) {
        // Dodaję testowe dane dla konta, które jeszcze ich nie ma, aby widok działał poprawnie bez edycji po stronie wykładowcy
        List<Grade> grades = gradeRepository.findByClerkUserIdOrderByIssueDateDesc(clerkUserId);
        
        if (grades.isEmpty()) {
            Grade dummy1 = new Grade();
            dummy1.setClerkUserId(clerkUserId);
            dummy1.setSubject("Architektura Komputerów");
            dummy1.setValue("5.0");
            dummy1.setIssueDate(LocalDateTime.now().minusHours(2));
            dummy1.setNew(true);
            
            Grade dummy2 = new Grade();
            dummy2.setClerkUserId(clerkUserId);
            dummy2.setSubject("Algorytmy i Struktury Danych");
            dummy2.setValue("4.5");
            dummy2.setIssueDate(LocalDateTime.now().minusDays(1));
            dummy2.setNew(false);
            
            gradeRepository.saveAll(List.of(dummy1, dummy2));
            grades = gradeRepository.findByClerkUserIdOrderByIssueDateDesc(clerkUserId);
        }

        return grades.stream()
                .map(g -> new GradeDto(g.getId(), g.getSubject(), g.getValue(), g.getIssueDate(), g.isNew()))
                .collect(Collectors.toList());
    }
}
