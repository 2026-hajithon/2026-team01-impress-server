CREATE TABLE questions (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '질문 고유 ID',
    question_type VARCHAR(30) NOT NULL
        COMMENT 'BLANK, INDIVIDUAL_CHOICE, COMMON_VOTE',
    content VARCHAR(255) NOT NULL COMMENT '질문 원문',

    CONSTRAINT pk_questions PRIMARY KEY (id),
    INDEX idx_questions_type (question_type)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;


CREATE TABLE question_options (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '질문 선택지 고유 ID',
    question_id BIGINT NOT NULL COMMENT '선택지가 속한 질문 ID',
    content VARCHAR(100) NOT NULL COMMENT '선택지 내용',
    display_order INT NOT NULL COMMENT '선택지 표시 순서',

    CONSTRAINT pk_question_options PRIMARY KEY (id),
    CONSTRAINT uk_question_option_order
        UNIQUE (question_id, display_order),
    CONSTRAINT fk_question_options_question
        FOREIGN KEY (question_id)
        REFERENCES questions (id)
        ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;


CREATE TABLE game_sessions (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '게임 한 판의 고유 ID',
    room_id BIGINT NOT NULL COMMENT '게임이 진행되는 방 ID',
    status VARCHAR(20) NOT NULL DEFAULT 'PLAYING'
        COMMENT 'PLAYING, FINISHED',
    total_rounds INT NOT NULL COMMENT '해당 게임의 전체 라운드 수',
    started_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    finished_at DATETIME(6) NULL,

    CONSTRAINT pk_game_sessions PRIMARY KEY (id),
    CONSTRAINT fk_game_sessions_room
        FOREIGN KEY (room_id)
        REFERENCES rooms (id),
    INDEX idx_game_sessions_room_status (room_id, status, id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;


CREATE TABLE game_rounds (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '게임 라운드 고유 ID',
    game_session_id BIGINT NOT NULL COMMENT '라운드가 속한 게임 ID',
    question_id BIGINT NOT NULL COMMENT '라운드 질문 ID',
    target_participant_id BIGINT NULL
        COMMENT '개인 질문 대상자, COMMON_VOTE는 NULL',
    round_order INT NOT NULL COMMENT '게임 안에서 1부터 시작하는 순서',
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING'
        COMMENT 'PENDING, ANSWERING, RESULT, COMPLETED',
    deadline_at DATETIME(6) NULL COMMENT '답변 제출 마감 시각',

    CONSTRAINT pk_game_rounds PRIMARY KEY (id),
    CONSTRAINT uk_game_round_order
        UNIQUE (game_session_id, round_order),
    CONSTRAINT fk_game_rounds_session
        FOREIGN KEY (game_session_id)
        REFERENCES game_sessions (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_game_rounds_question
        FOREIGN KEY (question_id)
        REFERENCES questions (id),
    CONSTRAINT fk_game_rounds_target
        FOREIGN KEY (target_participant_id)
        REFERENCES participants (id),
    INDEX idx_game_rounds_session_status
        (game_session_id, status, round_order)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;


CREATE TABLE answers (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '제출 답변 고유 ID',
    game_round_id BIGINT NOT NULL COMMENT '답변이 속한 라운드 ID',
    respondent_participant_id BIGINT NOT NULL COMMENT '답변 제출자 ID',
    text_content VARCHAR(255) NULL COMMENT 'BLANK 답변',
    selected_option_id BIGINT NULL
        COMMENT 'INDIVIDUAL_CHOICE에서 선택한 옵션 ID',
    selected_participant_id BIGINT NULL
        COMMENT 'COMMON_VOTE에서 선택한 참가자 ID',
    submitted_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_answers PRIMARY KEY (id),
    CONSTRAINT uk_answer_round_respondent
        UNIQUE (game_round_id, respondent_participant_id),
    CONSTRAINT fk_answers_round
        FOREIGN KEY (game_round_id)
        REFERENCES game_rounds (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_answers_respondent
        FOREIGN KEY (respondent_participant_id)
        REFERENCES participants (id),
    CONSTRAINT fk_answers_option
        FOREIGN KEY (selected_option_id)
        REFERENCES question_options (id),
    CONSTRAINT fk_answers_selected_participant
        FOREIGN KEY (selected_participant_id)
        REFERENCES participants (id),
    INDEX idx_answers_round (game_round_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;


CREATE TABLE next_round_votes (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '다음 라운드 진행 동의 ID',
    game_round_id BIGINT NOT NULL COMMENT '결과를 확인 중인 라운드 ID',
    participant_id BIGINT NOT NULL COMMENT '동의한 참가자 ID',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_next_round_votes PRIMARY KEY (id),
    CONSTRAINT uk_next_vote_round_participant
        UNIQUE (game_round_id, participant_id),
    CONSTRAINT fk_next_votes_round
        FOREIGN KEY (game_round_id)
        REFERENCES game_rounds (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_next_votes_participant
        FOREIGN KEY (participant_id)
        REFERENCES participants (id),
    INDEX idx_next_votes_round (game_round_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;