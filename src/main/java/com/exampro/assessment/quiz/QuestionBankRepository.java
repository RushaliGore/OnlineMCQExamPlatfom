package com.exampro.assessment.quiz;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionBankRepository extends JpaRepository<QuestionBankEntry, String> {

    @EntityGraph(attributePaths = "options")
    List<QuestionBankEntry> findAllByActiveTrueAndTopicOrderByQuestionId(String topic);

    @EntityGraph(attributePaths = "options")
    List<QuestionBankEntry> findAllByActiveTrueOrderByTopicAscQuestionIdAsc();

    long countByTopicAndActiveTrue(String topic);

    long countByActiveTrue();
}
