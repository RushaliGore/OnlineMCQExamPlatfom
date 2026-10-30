package com.exampro.assessment.quiz;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.exampro.assessment.account.StudentAccount;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {

    List<QuizAttempt> findTop10ByStudentOrderBySubmittedAtDesc(StudentAccount student);
}
