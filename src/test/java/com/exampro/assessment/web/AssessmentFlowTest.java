package com.exampro.assessment.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.exampro.assessment.account.StudentAccount;
import com.exampro.assessment.account.StudentAccountRepository;
import com.exampro.assessment.quiz.QuizAttemptRepository;
import com.exampro.assessment.quiz.QuestionBankData;
import com.exampro.assessment.quiz.QuestionBankImportService;
import com.exampro.assessment.quiz.QuestionBankRepository;
import com.exampro.assessment.quiz.QuizService;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:exampro-flow;DB_CLOSE_DELAY=-1",
        "SMTP_HOST="
})
@AutoConfigureMockMvc
class AssessmentFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StudentAccountRepository studentRepository;

    @Autowired
    private QuizAttemptRepository attemptRepository;

    @Autowired
    private QuizService quizService;

    @Autowired
    private QuestionBankImportService questionBankImportService;

    @Autowired
    private QuestionBankRepository questionBankRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void createVerifiedStudent() {
        attemptRepository.deleteAll();
        studentRepository.deleteAll();
        StudentAccount student = new StudentAccount(
                "learner@example.com",
                "Test Learner",
                passwordEncoder.encode("Correct-Horse-42"));
        student.verifyEmail();
        studentRepository.save(student);
    }

    @Test
    void protectsTheAssessmentFromSignedOutVisitors() throws Exception {
        mockMvc.perform(get("/quiz"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    void rejectsMismatchedRegistrationPasswords() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("displayName", "New Student")
                        .param("email", "new@example.com")
                        .param("password", "Correct-Horse-42")
                        .param("confirmPassword", "Different-Password-1"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(content().string(containsString("Passwords do not match")));

        assertEquals(1, studentRepository.count());
    }

    @Test
    void registersAndActivatesStudentsWithoutRequiringEmailSetup() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("displayName", "New Student")
                        .param("email", "new@example.com")
                        .param("password", "Correct-Horse-42")
                        .param("confirmPassword", "Correct-Horse-42"))
                .andExpect(status().isOk())
                .andExpect(view().name("account-created"))
                .andExpect(content().string(containsString("Email verification is turned off")));

        assertEquals(2, studentRepository.count());
        StudentAccount newStudent = studentRepository.findByEmail("new@example.com").orElseThrow();
        assertTrue(newStudent.isEnabled());
        assertTrue(passwordEncoder.matches("Correct-Horse-42", newStudent.getPasswordHash()));

        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "new@example.com")
                        .param("password", "Correct-Horse-42"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    void signsInStudentRendersAllQuestionsAndSavesServerGradedAttempt() throws Exception {
        MvcResult login = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "learner@example.com")
                        .param("password", "Correct-Horse-42"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        mockMvc.perform(get("/quiz").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("quiz"))
                .andExpect(model().attribute("questions", hasSize(20)))
                .andExpect(content().string(containsString("Mixed Core Java Assessment")))
                .andExpect(content().string(containsString("Select all that apply")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("Explanation:"))));

        mockMvc.perform(post("/quiz/submit")
                        .session(session)
                        .with(csrf())
                        .param("category", "Mixed Core Java Assessment"))
                .andExpect(status().isOk())
                .andExpect(view().name("result"))
                .andExpect(content().string(containsString("0 / 20")))
                .andExpect(content().string(containsString("20</strong> unanswered")));

        mockMvc.perform(get("/dashboard").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("0 / 20")))
                .andExpect(content().string(containsString("Mixed Core Java Assessment")));
    }

    @Test
    void categoryPagesLoadTheirOwnQuestionSetAndActiveQuestionCounts() throws Exception {
        assertEquals(300, quizService.getActiveQuestionCount());
        assertTrue(quizService.getCategories().stream().allMatch(category -> category.questionCount() == 20));

        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("categories", hasSize(15)))
                .andExpect(content().string(containsString("20 questions")));

        MvcResult login = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "learner@example.com")
                        .param("password", "Correct-Horse-42"))
                .andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);

        mockMvc.perform(get("/quiz")
                        .session(session)
                        .param("category", "Core Java Fundamentals"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("questions", hasSize(20)))
                .andExpect(content().string(containsString("Core Java Fundamentals")));
    }

    @Test
    void importingTheBundledBankAgainUpdatesRowsWithoutCreatingDuplicates() throws IOException {
        List<QuestionBankData> bank;
        try (InputStream input = getClass().getResourceAsStream("/question-bank.json")) {
            bank = objectMapper.readValue(input, new TypeReference<>() { });
        }

        assertEquals(300, questionBankImportService.importQuestions(bank));
        assertEquals(300, questionBankImportService.importQuestions(bank));
        assertEquals(300, questionBankRepository.count());
    }
}
