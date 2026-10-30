package com.exampro.assessment.quiz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.exampro.assessment.account.StudentAccount;

@ExtendWith(MockitoExtension.class)
class QuizServiceTest {

    @Mock
    private QuizAttemptRepository attemptRepository;

    @Mock
    private QuestionBankRepository questionRepository;

    private QuizService quizService;
    private List<QuestionBankEntry> questions;

    @BeforeEach
    void setUp() {
        quizService = new QuizService(attemptRepository, questionRepository);
        questions = questionsFor("Mixed Core Java Assessment", "MIXED");
        when(questionRepository.findAllByActiveTrueAndTopicOrderByQuestionId("Mixed Core Java Assessment"))
                .thenReturn(questions);
    }

    @Test
    void exposesQuestionContentAndOptionsWithoutCorrectAnswerMetadata() {
        List<QuizService.QuestionView> views = quizService.getQuestionViews("Mixed Core Java Assessment");

        assertEquals(20, views.size());
        assertEquals(4, views.get(0).options().size());
        assertTrue(views.get(0).multipleSelect());
        assertEquals("MIXED001", views.get(0).id());
        assertTrue(views.get(0).options().stream().noneMatch(option -> option.text().contains("CORRECT")));
    }

    @Test
    void gradesExactOptionSetsAndPersistsCategoryAttempt() {
        Map<String, List<String>> answers = questions.stream()
                .collect(java.util.stream.Collectors.toMap(
                        QuestionBankEntry::getQuestionId,
                        question -> List.of(question.getQuestionId() + ":A", question.getQuestionId() + ":C")));
        StudentAccount student = new StudentAccount("student@example.com", "Student", "hash");

        QuizService.QuizResult result = quizService.grade(student, "Mixed Core Java Assessment", answers);

        assertEquals(20, result.score());
        assertEquals(20, result.total());
        assertEquals(100, result.getPercentage());
        assertEquals(0, result.getIncorrectCount());
        assertEquals(0, result.getUnansweredCount());
        verify(attemptRepository).save(org.mockito.ArgumentMatchers.argThat(attempt ->
                attempt.getCategory().equals("Mixed Core Java Assessment")
                        && attempt.getScore() == 20
                        && attempt.getQuestionCount() == 20));
    }

    @Test
    void treatsIncorrectAndUnansweredQuestionsSeparately() {
        StudentAccount student = new StudentAccount("student@example.com", "Student", "hash");

        QuizService.QuizResult result = quizService.grade(student, "Mixed Core Java Assessment", Map.of(
                "MIXED001", List.of("MIXED001:A"),
                "MIXED002", List.of()));

        assertEquals(0, result.score());
        assertEquals(1, result.getIncorrectCount());
        assertEquals(19, result.getUnansweredCount());
    }

    @Test
    void rejectsQuestionIdsOutsideTheSelectedCategoryAndUnknownOptions() {
        StudentAccount student = new StudentAccount("student@example.com", "Student", "hash");

        assertThrows(IllegalArgumentException.class,
                () -> quizService.grade(student, "Mixed Core Java Assessment", Map.of("CORE001", List.of("A"))));
        assertThrows(IllegalArgumentException.class,
                () -> quizService.grade(student, "Mixed Core Java Assessment", Map.of("MIXED001", List.of("MIXED001:Z"))));
        assertThrows(IllegalArgumentException.class,
                () -> quizService.grade(student, "Mixed Core Java Assessment", Map.of("MIXED001", List.of("MIXED002:A"))));
    }

    private static List<QuestionBankEntry> questionsFor(String topic, String prefix) {
        List<QuestionBankEntry> entries = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            String id = prefix + String.format("%03d", i);
            QuestionBankEntry entry = new QuestionBankEntry(
                    id, topic, "INTERMEDIATE", "MULTIPLE_SELECT", "Prompt " + id, null,
                    "Explanation " + id, null, true);
            entry.update(
                    topic,
                    "INTERMEDIATE",
                    "MULTIPLE_SELECT",
                    "Prompt " + id,
                    null,
                    "Explanation " + id,
                    null,
                    true,
                    List.of(
                            new QuestionBankOption.Data("A", "Option A", true),
                            new QuestionBankOption.Data("B", "Option B", false),
                            new QuestionBankOption.Data("C", "Option C", true),
                            new QuestionBankOption.Data("D", "Option D", false)));
            entries.add(entry);
        }
        return entries;
    }
}
