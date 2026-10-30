package com.exampro.assessment.quiz;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuestionBankImportService {

    private static final Set<String> TOPICS = Set.of(
            "Core Java Fundamentals",
            "Object-Oriented Programming",
            "Strings and Arrays",
            "Java Collections Framework",
            "Exception Handling",
            "Java 8 Features",
            "Multithreading and Concurrency",
            "Java I/O and File Handling",
            "Generics",
            "Java Reflection API",
            "JDBC",
            "Hibernate and ORM",
            "Java Coding Output Questions",
            "Java Interview Mock Tests",
            "Mixed Core Java Assessment");

    private static final List<String> OPTION_LABELS = List.of("A", "B", "C", "D");

    private final QuestionBankRepository questionRepository;

    public QuestionBankImportService(QuestionBankRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    @Transactional
    public int importQuestions(List<QuestionBankData> questions) {
        validateBank(questions);
        Map<String, QuestionBankEntry> existing = questionRepository.findAll().stream()
                .collect(Collectors.toMap(QuestionBankEntry::getQuestionId, Function.identity()));

        for (QuestionBankData question : questions) {
            List<QuestionBankOption.Data> options = question.options().stream()
                    .map(option -> new QuestionBankOption.Data(option.label(), option.text(), option.isCorrect()))
                    .toList();
            QuestionBankEntry entry = existing.get(question.questionId());
            if (entry == null) {
                entry = new QuestionBankEntry(
                        question.questionId(),
                        question.topic(),
                        question.difficulty(),
                        question.questionType(),
                        question.questionText(),
                        question.codeSnippet(),
                        question.explanation(),
                        question.expectedOutput(),
                        question.active());
                entry.update(
                        question.topic(),
                        question.difficulty(),
                        question.questionType(),
                        question.questionText(),
                        question.codeSnippet(),
                        question.explanation(),
                        question.expectedOutput(),
                        question.active(),
                        options);
                existing.put(question.questionId(), entry);
            } else {
                entry.update(
                        question.topic(),
                        question.difficulty(),
                        question.questionType(),
                        question.questionText(),
                        question.codeSnippet(),
                        question.explanation(),
                        question.expectedOutput(),
                        question.active(),
                        options);
            }
        }
        questionRepository.saveAll(existing.values());
        return questions.size();
    }

    private static void validateBank(List<QuestionBankData> questions) {
        if (questions == null || questions.size() != 300) {
            throw new IllegalArgumentException("The question bank must contain exactly 300 questions.");
        }
        if (questions.stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException("The question bank cannot contain null entries.");
        }

        Set<String> identifiers = new HashSet<>();
        Map<String, Long> topicCounts = questions.stream()
                .filter(question -> question.topic() != null)
                .collect(Collectors.groupingBy(QuestionBankData::topic, Collectors.counting()));
        if (!topicCounts.keySet().equals(TOPICS)
                || topicCounts.values().stream().anyMatch(count -> count != 20)) {
            throw new IllegalArgumentException("The question bank must contain exactly 20 questions for each supported topic.");
        }

        for (QuestionBankData question : questions) {
            requireText(question.questionId(), "questionId");
            requireText(question.questionText(), "questionText");
            requireText(question.explanation(), "explanation");
            if (!identifiers.add(question.questionId())) {
                throw new IllegalArgumentException("Duplicate question ID: " + question.questionId());
            }
            if (!TOPICS.contains(question.topic())) {
                throw new IllegalArgumentException("Unsupported question topic: " + question.topic());
            }
            if (question.difficulty() == null
                    || !Set.of("BEGINNER", "INTERMEDIATE", "ADVANCED").contains(question.difficulty())) {
                throw new IllegalArgumentException("Unsupported difficulty for question " + question.questionId());
            }
            if (!"MULTIPLE_SELECT".equals(question.questionType())) {
                throw new IllegalArgumentException("Unsupported question type for question " + question.questionId());
            }
            if (question.active() == null) {
                throw new IllegalArgumentException("Missing active status for question " + question.questionId());
            }
            if (question.options() == null
                    || question.options().size() != 4
                    || question.options().stream().anyMatch(java.util.Objects::isNull)) {
                throw new IllegalArgumentException("Question " + question.questionId() + " must have exactly four options.");
            }

            List<String> labels = question.options().stream().map(QuestionBankData.OptionData::label).toList();
            if (!labels.equals(OPTION_LABELS)) {
                throw new IllegalArgumentException("Question " + question.questionId() + " must have options labeled A, B, C, and D in order.");
            }
            long correctCount = question.options().stream()
                    .filter(option -> option.isCorrect() != null && option.isCorrect())
                    .count();
            if (correctCount < 1 || correctCount > 3) {
                throw new IllegalArgumentException("Question " + question.questionId() + " must have one to three correct options.");
            }
            for (QuestionBankData.OptionData option : question.options()) {
                requireText(option.text(), "option text");
                if (option.isCorrect() == null) {
                    throw new IllegalArgumentException("Missing correctness flag in question " + question.questionId());
                }
            }
        }
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Question bank contains an empty " + field + ".");
        }
    }
}
