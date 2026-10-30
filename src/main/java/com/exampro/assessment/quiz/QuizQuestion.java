package com.exampro.assessment.quiz;

import java.util.List;

public record QuizQuestion(
        int id,
        String prompt,
        List<Option> options,
        List<String> correctOptionIds) {

    public QuizQuestion {
        options = List.copyOf(options);
        correctOptionIds = List.copyOf(correctOptionIds);
    }

    public boolean isMultipleSelect() {
        return correctOptionIds.size() > 1;
    }

    public record Option(String id, String label, String text) {
    }
}
