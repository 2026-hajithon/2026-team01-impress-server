CREATE TABLE rooms
(
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    name       VARCHAR(20) NOT NULL,
    code       CHAR(4)     NOT NULL,
    status     VARCHAR(20) NOT NULL DEFAULT 'WAITING',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_rooms PRIMARY KEY (id),
    CONSTRAINT uk_rooms_code UNIQUE (code)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;


CREATE TABLE participants
(
    id                BIGINT      NOT NULL AUTO_INCREMENT,
    room_id           BIGINT      NOT NULL,
    name              VARCHAR(20) NOT NULL,
    role              VARCHAR(10) NOT NULL,
    connection_status VARCHAR(20) NOT NULL DEFAULT 'DISCONNECTED',
    joined_at         DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    last_seen_at      DATETIME(6) NULL,

    CONSTRAINT pk_participants PRIMARY KEY (id),

    CONSTRAINT uk_participants_room_name
        UNIQUE (room_id, name),

    CONSTRAINT fk_participants_room
        FOREIGN KEY (room_id)
            REFERENCES rooms (id)
            ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;