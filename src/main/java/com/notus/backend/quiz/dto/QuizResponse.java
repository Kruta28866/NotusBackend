package com.notus.backend.quiz.dto;

import java.util.List;

public class QuizResponse {
    private String title;
    private List<QuestionDto> questions;

    public QuizResponse() {
    }

    public QuizResponse(String title, List<QuestionDto> questions) {
        this.title = title;
        this.questions = questions;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<QuestionDto> getQuestions() {
        return questions;
    }

    public void setQuestions(List<QuestionDto> questions) {
        this.questions = questions;
    }
}