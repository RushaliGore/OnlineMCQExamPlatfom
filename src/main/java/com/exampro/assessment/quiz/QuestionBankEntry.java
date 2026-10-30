package com.exampro.assessment.quiz;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@Entity
@Table(name = "question_bank")
public class QuestionBankEntry {

    @Id
    @Column(name = "question_id", nullable = false, length = 40)
    private String questionId;

    @Column(nullable = false, length = 100)
    private String topic;

    @Column(nullable = false, length = 20)
    private String difficulty;

    @Column(nullable = false, length = 30)
    private String questionType;

    @Column(nullable = false, length = 2000)
    private String questionText;

    @Column(length = 10000)
    private String codeSnippet;

    @Column(nullable = false, length = 10000)
    private String explanation;

    @Column(length = 10000)
    private String expectedOutput;

    @Column(nullable = false)
    private boolean active;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("label ASC")
    private List<QuestionBankOption> options = new ArrayList<>();

    protected QuestionBankEntry() {
    }

    public QuestionBankEntry(
            String questionId,
            String topic,
            String difficulty,
            String questionType,
            String questionText,
            String codeSnippet,
            String explanation,
            String expectedOutput,
            boolean active) {
        this.questionId = questionId;
        update(topic, difficulty, questionType, questionText, codeSnippet, explanation, expectedOutput, active, List.of());
    }

    public void update(
            String topic,
            String difficulty,
            String questionType,
            String questionText,
            String codeSnippet,
            String explanation,
            String expectedOutput,
            boolean active,
            List<QuestionBankOption.Data> optionData) {
        this.topic = topic;
        this.difficulty = difficulty;
        this.questionType = questionType;
        this.questionText = questionText;
        this.codeSnippet = codeSnippet;
        this.explanation = explanation;
        this.expectedOutput = expectedOutput;
        this.active = active;
        Map<String, QuestionBankOption> previousOptions = options.stream()
                .collect(Collectors.toMap(QuestionBankOption::getLabel, option -> option));
        Set<String> importedLabels = optionData.stream()
                .map(QuestionBankOption.Data::label)
                .collect(Collectors.toSet());
        for (QuestionBankOption.Data option : optionData) {
            QuestionBankOption existing = previousOptions.get(option.label());
            if (existing == null) {
                options.add(new QuestionBankOption(this, option.label(), option.text(), option.correct()));
            } else {
                existing.update(option.text(), option.correct());
                options.add(existing);
            }
        }
        options.removeIf(option -> !importedLabels.contains(option.getLabel()));
    }

    public String getQuestionId() {
        return questionId;
    }

    public String getTopic() {
        return topic;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public String getQuestionType() {
        return questionType;
    }

    public String getQuestionText() {
        return questionText;
    }

    public String getCodeSnippet() {
        return codeSnippet;
    }

    public String getExplanation() {
        return explanation;
    }

    public String getExpectedOutput() {
        return expectedOutput;
    }

    public boolean isActive() {
        return active;
    }

    public List<QuestionBankOption> getOptions() {
        return List.copyOf(options);
    }
}
