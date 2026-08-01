package com.impress.server.question.repository;

import com.impress.server.question.domain.Question;
import com.impress.server.question.domain.QuestionOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QuestionOptionRepository
        extends JpaRepository<QuestionOption, Long> {

    Optional<QuestionOption> findByIdAndQuestion(
            Long id,
            Question question
    );
}