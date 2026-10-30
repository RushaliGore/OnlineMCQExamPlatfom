package com.exampro.assessment.quiz;

import java.util.List;

public record QuestionBankData(
        String questionId,
        String topic,
        String difficulty,
        String questionType,
        String questionText,
        String codeSnippet,
        List<OptionData> options,
        String explanation,
        String expectedOutput,
        Boolean active) {

    public record OptionData(String label, String text, Boolean isCorrect) {
    }
}
