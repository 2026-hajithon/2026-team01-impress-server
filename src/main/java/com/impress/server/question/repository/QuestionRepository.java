package com.impress.server.question.repository;

import com.impress.server.question.domain.Question;
import com.impress.server.question.domain.QuestionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionRepository
        extends JpaRepository<Question, Long> {

    List<Question> findAllByQuestionTypeOrderByIdAsc(
            QuestionType questionType
    );
}