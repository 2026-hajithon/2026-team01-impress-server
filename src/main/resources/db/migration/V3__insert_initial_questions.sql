-- =========================================================
-- 1. 특정 사람에 대한 빈칸 질문
-- =========================================================

INSERT INTO questions (question_type, content)
VALUES
    ('BLANK', '이 사람은 주말에 _____을 할 것 같다!'),
    ('BLANK', '이 사람은 동아리나 팀 프로젝트를 한다면 _____ 역할을 맡을 것 같다!'),
    ('BLANK', '이 사람은 노래방에서 _____을 부를 것 같다.'),
    ('BLANK', '이 사람에게 어울리는 이모지는 _____이다.'),
    ('BLANK', '이 사람을 한 단어로 표현하면 _____이다.'),
    ('BLANK', '이 사람에게 가장 잘 어울리는 별명은 _____이다.'),
    ('BLANK', '이 사람을 동물로 표현하면 _____일 것 같다.'),
    ('BLANK', '이 사람의 첫인상을 색으로 표현하면 _____이다.');


-- =========================================================
-- 2. 특정 사람에 대한 개인 선택형 질문
-- =========================================================

INSERT INTO questions (question_type, content)
VALUES ('INDIVIDUAL_CHOICE', '이 사람의 MBTI 앞 두 글자는 무엇일까요?');

SET @mbti_question_id = LAST_INSERT_ID();

INSERT INTO question_options (
    question_id,
    content,
    display_order
)
VALUES
    (@mbti_question_id, 'IN', 1),
    (@mbti_question_id, 'IS', 2),
    (@mbti_question_id, 'EN', 3),
    (@mbti_question_id, 'ES', 4);


INSERT INTO questions (question_type, content)
VALUES ('INDIVIDUAL_CHOICE', '이 사람의 형제자매 관계는 어떻게 될까요?');

SET @sibling_question_id = LAST_INSERT_ID();

INSERT INTO question_options (
    question_id,
    content,
    display_order
)
VALUES
    (@sibling_question_id, '형제', 1),
    (@sibling_question_id, '자매', 2),
    (@sibling_question_id, '남매', 3),
    (@sibling_question_id, '외동', 4);


INSERT INTO questions (question_type, content)
VALUES ('INDIVIDUAL_CHOICE', '이 사람은 어떤 고등학교를 나왔을까요?');

SET @school_question_id = LAST_INSERT_ID();

INSERT INTO question_options (
    question_id,
    content,
    display_order
)
VALUES
    (@school_question_id, '남고/여고', 1),
    (@school_question_id, '남녀공학(분반)', 2),
    (@school_question_id, '남녀공학(합반)', 3),
    (@school_question_id, '기타', 4);


INSERT INTO questions (question_type, content)
VALUES ('INDIVIDUAL_CHOICE', '이 사람이 좋아하는 계절은 무엇일까요?');

SET @season_question_id = LAST_INSERT_ID();

INSERT INTO question_options (
    question_id,
    content,
    display_order
)
VALUES
    (@season_question_id, '봄', 1),
    (@season_question_id, '여름', 2),
    (@season_question_id, '가을', 3),
    (@season_question_id, '겨울', 4);


INSERT INTO questions (question_type, content)
VALUES ('INDIVIDUAL_CHOICE', '이 사람이 좋아할 음식은 무엇일까요?');

SET @food_question_id = LAST_INSERT_ID();

INSERT INTO question_options (
    question_id,
    content,
    display_order
)
VALUES
    (@food_question_id, '한식', 1),
    (@food_question_id, '중식', 2),
    (@food_question_id, '일식', 3),
    (@food_question_id, '양식', 4);


-- =========================================================
-- 3. 이미지에 가장 어울리는 사람 공통 투표
-- =========================================================

INSERT INTO questions (question_type, content)
VALUES
    ('COMMON_VOTE', '어렸을 때 가장 부모님 말을 안 들었을 것 같은 사람은?'),
    ('COMMON_VOTE', '좀비 사태에서 끝까지 살아남을 사람은?'),
    ('COMMON_VOTE', '학창 시절 반장을 해봤을 것 같은 사람은?'),
    ('COMMON_VOTE', '졸업식에서 가장 많이 울었을 것 같은 사람은?'),
    ('COMMON_VOTE', '학창 시절 별명이 가장 많았을 것 같은 사람은?'),
    ('COMMON_VOTE', '여행 가방을 가장 크게 챙길 사람은?'),
    ('COMMON_VOTE', '하루 종일 집에서 잘 쉴 사람은?');