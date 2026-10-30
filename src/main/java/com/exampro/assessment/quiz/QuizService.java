package com.exampro.assessment.quiz;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.exampro.assessment.account.StudentAccount;

@Service
public class QuizService {

    public static final String DEFAULT_CATEGORY = "Mixed Core Java Assessment";

    private static final List<CategoryDefinition> CATEGORIES = List.of(
            new CategoryDefinition(1, "Core Java Fundamentals", "Java syntax, types, control flow and runtime basics.", "Beginner", "20 min"),
            new CategoryDefinition(2, "Object-Oriented Programming", "Classes, inheritance, polymorphism and encapsulation.", "Intermediate", "25 min"),
            new CategoryDefinition(3, "Strings and Arrays", "String behavior, array operations and coding patterns.", "Beginner", "20 min"),
            new CategoryDefinition(4, "Java Collections Framework", "Lists, sets, maps, queues and collection contracts.", "Intermediate", "25 min"),
            new CategoryDefinition(5, "Exception Handling", "Exception types, propagation, cleanup and custom exceptions.", "Intermediate", "18 min"),
            new CategoryDefinition(6, "Java 8 Features", "Lambdas, streams, functional interfaces and Optional.", "Intermediate", "25 min"),
            new CategoryDefinition(7, "Multithreading and Concurrency", "Thread coordination, synchronization and concurrency utilities.", "Advanced", "25 min"),
            new CategoryDefinition(8, "Java I/O and File Handling", "Streams, paths, serialization and file operations.", "Intermediate", "20 min"),
            new CategoryDefinition(9, "Generics", "Type parameters, bounds, wildcards and type erasure.", "Intermediate", "18 min"),
            new CategoryDefinition(10, "Java Reflection API", "Runtime type metadata, members, annotations and access.", "Advanced", "20 min"),
            new CategoryDefinition(11, "JDBC", "Connections, statements, transactions and result handling.", "Intermediate", "20 min"),
            new CategoryDefinition(12, "Hibernate and ORM", "Entity mappings, persistence context and ORM behavior.", "Intermediate", "25 min"),
            new CategoryDefinition(13, "Java Coding Output Questions", "Predict deterministic program output and behavior.", "Intermediate", "20 min"),
            new CategoryDefinition(14, "Java Interview Mock Tests", "Practical Java questions for junior developer interviews.", "Advanced", "30 min"),
            new CategoryDefinition(15, "Mixed Core Java Assessment", "A mixed assessment across the Java curriculum.", "Advanced", "35 min"));

    private final QuizAttemptRepository attemptRepository;
    private final QuestionBankRepository questionRepository;

    public QuizService(QuizAttemptRepository attemptRepository, QuestionBankRepository questionRepository) {
        this.attemptRepository = attemptRepository;
        this.questionRepository = questionRepository;
    }

    @Transactional(readOnly = true)
    public List<QuestionView> getQuestionViews(String topic) {
        List<QuestionBankEntry> questions = questionRepository.findAllByActiveTrueAndTopicOrderByQuestionId(topic);
        if (questions.isEmpty()) {
            throw new IllegalArgumentException("There are no active questions for the selected category.");
        }
        return questions.stream()
                .map(question -> new QuestionView(
                        question.getQuestionId(),
                        question.getQuestionText(),
                        question.getCodeSnippet(),
                        question.getDifficulty(),
                        question.getOptions().stream()
                                .map(option -> new QuizQuestion.Option(
                                        optionId(question, option),
                                        option.getLabel(),
                                        option.getText()))
                                .toList()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<QuestionCategorySummary> getCategories() {
        return CATEGORIES.stream()
                .map(category -> new QuestionCategorySummary(
                        category.id(),
                        category.name(),
                        category.description(),
                        category.difficulty(),
                        Math.toIntExact(questionRepository.countByTopicAndActiveTrue(category.name())),
                        category.duration()))
                .toList();
    }

    public boolean isSupportedCategory(String topic) {
        return CATEGORIES.stream().anyMatch(category -> category.name().equals(topic));
    }

    public int getDurationMinutes(String topic) {
        return CATEGORIES.stream()
                .filter(category -> category.name().equals(topic))
                .findFirst()
                .map(category -> Integer.parseInt(category.duration().replace(" min", "")))
                .orElseThrow(() -> new IllegalArgumentException("Unknown assessment category."));
    }

    public int getActiveQuestionCount() {
        return Math.toIntExact(questionRepository.countByActiveTrue());
    }

    public int getBestScore(StudentAccount student) {
        return getRecentAttempts(student).stream()
                .mapToInt(QuizAttempt::getPercentage)
                .max()
                .orElse(0);
    }

    @Transactional
    public QuizResult grade(StudentAccount student, String topic, Map<String, List<String>> submittedAnswers) {
        List<QuestionBankEntry> questions = questionRepository.findAllByActiveTrueAndTopicOrderByQuestionId(topic);
        if (questions.isEmpty()) {
            throw new IllegalArgumentException("There are no active questions for the selected category.");
        }
        if (!questions.stream().map(QuestionBankEntry::getQuestionId).toList().containsAll(submittedAnswers.keySet())) {
            throw new IllegalArgumentException("The submitted answers contain a question outside the selected category.");
        }

        int score = 0;
        List<QuestionReview> reviews = new ArrayList<>();
        for (QuestionBankEntry question : questions) {
            List<String> selected = submittedAnswers.getOrDefault(question.getQuestionId(), List.of())
                    .stream()
                    .distinct()
                    .sorted()
                    .toList();
            List<String> validOptionIds = question.getOptions().stream()
                    .map(option -> optionId(question, option))
                    .toList();
            if (!validOptionIds.containsAll(selected)) {
                throw new IllegalArgumentException("The submitted answers contain an invalid option.");
            }

            List<String> correct = question.getOptions().stream()
                    .filter(QuestionBankOption::isCorrect)
                    .map(option -> optionId(question, option))
                    .sorted()
                    .toList();
            boolean isCorrect = selected.equals(correct);
            if (isCorrect) {
                score++;
            }

            reviews.add(new QuestionReview(
                    question.getQuestionId(),
                    question.getQuestionText(),
                    optionTexts(question, selected),
                    optionTexts(question, correct),
                    question.getExplanation(),
                    question.getCodeSnippet(),
                    question.getExpectedOutput(),
                    isCorrect));
        }

        attemptRepository.save(new QuizAttempt(student, topic, score, questions.size()));
        return new QuizResult(score, questions.size(), reviews);
    }

    @Transactional(readOnly = true)
    public List<QuizAttempt> getRecentAttempts(StudentAccount student) {
        return attemptRepository.findTop10ByStudentOrderBySubmittedAtDesc(student);
    }

    private static List<String> optionTexts(QuestionBankEntry question, List<String> ids) {
        return question.getOptions().stream()
                .filter(option -> ids.contains(optionId(question, option)))
                .map(QuestionBankOption::getText)
                .toList();
    }

    private static String optionId(QuestionBankEntry question, QuestionBankOption option) {
        return question.getQuestionId() + ":" + option.getLabel();
    }

    public record QuestionCategorySummary(
            int id,
            String name,
            String description,
            String difficulty,
            int questionCount,
            String duration) {
    }

    private record CategoryDefinition(
            int id,
            String name,
            String description,
            String difficulty,
            String duration) {
    }

    public record QuestionView(
            String id,
            String prompt,
            String codeSnippet,
            String difficulty,
            List<QuizQuestion.Option> options) {

        public QuestionView {
            options = List.copyOf(options);
        }

        public boolean multipleSelect() {
            return true;
        }
    }

    public record QuestionReview(
            String number,
            String prompt,
            List<String> selectedAnswers,
            List<String> correctAnswers,
            String explanation,
            String codeSnippet,
            String expectedOutput,
            boolean correct) {
    }

    public record QuizResult(int score, int total, List<QuestionReview> reviews) {

        public int getPercentage() {
            return total == 0 ? 0 : (int) Math.round(score * 100.0 / total);
        }

        public long getIncorrectCount() {
            return reviews.stream().filter(review -> !review.correct() && !review.selectedAnswers().isEmpty()).count();
        }

        public long getUnansweredCount() {
            return reviews.stream().filter(review -> review.selectedAnswers().isEmpty()).count();
        }
    }
}
