package com.notus.backend.quiz.dto;

import java.util.List;

public class QuizResponse {
    private String title;
    private String description;
    private List<QuestionDto> questions;

    public QuizResponse() {
    }

    public QuizResponse(String title, String description, List<QuestionDto> questions) {
        this.title = title;
        this.description = description;
        this.questions = questions;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<QuestionDto> getQuestions() {
        return questions;
    }

    public void setQuestions(List<QuestionDto> questions) {
        this.questions = questions;
    }
}