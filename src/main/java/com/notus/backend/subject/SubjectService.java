package com.notus.backend.subject;

import com.notus.backend.subject.Subject;
import com.notus.backend.subject.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SubjectService {

    private final SubjectRepository subjectRepository;

    public void seedSubjects() {

        if (subjectRepository.count() > 0) {
            return;
        }

        List<Subject> subjects = List.of(
                Subject.builder().name("Matematyka").build(),
                Subject.builder().name("Język polski").build(),
                Subject.builder().name("Język angielski").build(),
                Subject.builder().name("Informatyka").build(),
                Subject.builder().name("Fizyka").build(),
                Subject.builder().name("Chemia").build(),
                Subject.builder().name("Biologia").build(),
                Subject.builder().name("Historia").build(),
                Subject.builder().name("Geografia").build()
        );

        subjectRepository.saveAll(subjects);
    }

    public List<Subject> getAll() {
        return subjectRepository.findAll();
    }
}