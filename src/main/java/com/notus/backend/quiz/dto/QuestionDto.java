package com.notus.backend.quiz.dto;

import java.util.List;
import com.notus.backend.quiz.QuestionType;

public class QuestionDto {
    private String question;
    private List<String> options;
    private String correctAnswer;
    private QuestionType type = QuestionType.CLOSED; // Default for AI

    public QuestionDto() {
    }

    public QuestionDto(String question, List<String> options, String correctAnswer, QuestionType type) {
        this.question = question;
        this.options = options;
        this.correctAnswer = correctAnswer;
        this.type = type;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }

    public QuestionType getType() {
        return type;
    }

    public void setType(QuestionType type) {
        this.type = type;
    }
}