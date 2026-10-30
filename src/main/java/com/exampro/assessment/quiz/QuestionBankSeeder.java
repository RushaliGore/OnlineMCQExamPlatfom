package com.exampro.assessment.quiz;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class QuestionBankSeeder implements ApplicationRunner {

    private final ObjectMapper objectMapper;
    private final QuestionBankImportService importService;

    public QuestionBankSeeder(ObjectMapper objectMapper, QuestionBankImportService importService) {
        this.objectMapper = objectMapper;
        this.importService = importService;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        ClassPathResource resource = new ClassPathResource("question-bank.json");
        try (InputStream input = resource.getInputStream()) {
            List<QuestionBankData> questions = objectMapper.readValue(input, new TypeReference<>() { });
            importService.importQuestions(questions);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("The bundled question bank is invalid: " + exception.getMessage(), exception);
        }
    }
}
