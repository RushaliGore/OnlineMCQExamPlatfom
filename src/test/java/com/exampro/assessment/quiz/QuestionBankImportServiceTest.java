package com.exampro.assessment.quiz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QuestionBankImportServiceTest {

    @Mock
    private QuestionBankRepository questionRepository;

    private QuestionBankImportService importService;

    @BeforeEach
    void setUp() {
        importService = new QuestionBankImportService(questionRepository);
        lenient().when(questionRepository.findAll()).thenReturn(List.of());
    }

    @Test
    void importsThreeHundredQuestionsAcrossAllFifteenTopics() {
        List<QuestionBankData> data = validBank();

        assertEquals(300, importService.importQuestions(data));

        verify(questionRepository).saveAll(anyCollection());
    }

    @Test
    void rejectsDuplicateQuestionIdsBeforeWriting() {
        List<QuestionBankData> data = validBank();
        QuestionBankData duplicate = data.get(0);
        QuestionBankData replaced = data.get(1);
        data.set(1, new QuestionBankData(
                duplicate.questionId(),
                replaced.topic(),
                replaced.difficulty(),
                replaced.questionType(),
                replaced.questionText(),
                replaced.codeSnippet(),
                replaced.options(),
                replaced.explanation(),
                replaced.expectedOutput(),
                replaced.active()));

        assertThrows(IllegalArgumentException.class, () -> importService.importQuestions(data));
    }

    private static List<QuestionBankData> validBank() {
        List<String> topics = List.of(
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
        List<QuestionBankData> bank = new ArrayList<>();
        for (int topicIndex = 0; topicIndex < topics.size(); topicIndex++) {
            for (int questionIndex = 1; questionIndex <= 20; questionIndex++) {
                String id = "T" + String.format("%02d", topicIndex + 1) + String.format("%03d", questionIndex);
                bank.add(new QuestionBankData(
                        id,
                        topics.get(topicIndex),
                        "BEGINNER",
                        "MULTIPLE_SELECT",
                        "Question " + id,
                        null,
                        List.of(
                                new QuestionBankData.OptionData("A", "A", true),
                                new QuestionBankData.OptionData("B", "B", false),
                                new QuestionBankData.OptionData("C", "C", false),
                                new QuestionBankData.OptionData("D", "D", false)),
                        "Explanation " + id,
                        null,
                        true));
            }
        }
        return bank;
    }
}
