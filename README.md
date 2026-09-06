# I'm Press Server

방 코드로 참여해 서로의 첫인상을 맞히는 실시간 웹 게임의 백엔드 서버입니다.

로그인 없이 방을 만들거나 참여할 수 있으며, 참가자들은 빈칸형, 개인 선택형, 공통 투표형 질문에 답하고 결과를 함께 확인합니다. 결과 화면에서 연결된 참가자의 과반수가 동의하면 다음 라운드로 진행합니다.

* 팀 구성: 백엔드 2명, 프론트엔드 2명, 디자인 2명
* 결과: 해커톤 4개 팀 중 대상

## 주요 기능

### 방 생성 및 참가

* 방 생성 및 방 코드 기반 참가
* 현재 방 상태와 참가자 정보 조회
* 방장 및 방 이름 조회
* 참가자 퇴장과 최종 게임 결과 조회
* 동일한 방 안에서 참가자 이름 중복 방지

### 실시간 대기방

* STOMP/SockJS 기반 참가자 입장 및 연결 종료 처리
* 참가자의 접속 상태를 `CONNECTED`, `DISCONNECTED`로 관리
* 참가자 목록과 방 상태를 방 전체에 실시간 전송
* 방장 권한을 확인한 참가자 강퇴
* 잘못된 요청은 요청자 개인의 오류 Queue로 전달

### 게임 진행

* 방장 권한과 연결된 참가자 수를 확인한 뒤 게임 시작
* 참가자와 질문 순서를 무작위로 구성
* 빈칸형, 개인 선택형, 공통 투표형 질문 제공
* 라운드를 `PENDING`, `ANSWERING`, `RESULT`, `COMPLETED` 상태로 관리
* 질문 유형에 따라 15초 또는 60초의 제한 시간 적용

### 답변 및 결과 처리

* 참가자의 방 소속, 라운드 상태, 제한 시간, 중복 제출 검증
* 질문 유형에 따라 텍스트, 선택지 또는 참가자 답변 저장
* 현재 연결된 참가자를 기준으로 필수 답변 완료 여부 확인
* 제한 시간이 지나면 현재까지 제출된 답변으로 결과 생성
* 결과 화면에서 과반수가 동의하면 다음 라운드 시작
* 마지막 라운드가 끝나면 게임 세션과 방 종료

## 게임 진행 방식

방장이 게임을 시작하면 참가자별 개인 질문과 전체 공통 투표 질문으로 라운드가 구성됩니다. 필수 참가자가 모두 답변하거나 제한 시간이 종료되면 결과 화면으로 전환됩니다.

결과 화면에서는 현재 연결된 참가자들이 다음 라운드 진행 여부를 투표합니다. 과반수가 동의하면 다음 라운드가 시작되며, 마지막 라운드에서는 게임이 종료됩니다.

## 참가자 식별

방 참가 시 발급된 참가자 ID를 이후 요청에 사용합니다.

REST API 요청에서는 `Participant-Id` 헤더로 전달하고, STOMP 연결 시에는 CONNECT 헤더에 동일한 값을 전달합니다. 서버는 방 코드와 참가자 ID를 함께 확인해 참가자의 방 소속과 권한을 검증합니다.

## REST API

| Method   | Endpoint                                | 설명          |
| -------- | --------------------------------------- | ----------- |
| `POST`   | `/api/rooms`                            | 방 생성        |
| `POST`   | `/api/rooms/{roomCode}/join`            | 방 참가        |
| `GET`    | `/api/rooms/{roomCode}/sync`            | 현재 방 상태 조회  |
| `DELETE` | `/api/rooms/{roomCode}/participants/me` | 방 퇴장        |
| `GET`    | `/api/rooms/{roomCode}/result`          | 최종 게임 결과 조회 |
| `GET`    | `/api/rooms/{roomCode}/host`            | 방장 조회       |
| `GET`    | `/api/rooms/{roomCode}/name`            | 방 이름 조회     |

## WebSocket

* 연결 경로: `/ws`
* 클라이언트 요청: `/app/rooms/{roomCode}`
* 방 전체 이벤트: `/topic/rooms/{roomCode}`
* 개인 오류 응답: `/user/queue/errors`

| Destination                    | 설명        |
| ------------------------------ | --------- |
| `/app/rooms/{roomCode}/enter`  | 대기방 입장    |
| `/app/rooms/{roomCode}/kick`   | 참가자 강퇴    |
| `/app/rooms/{roomCode}/start`  | 게임 시작     |
| `/app/rooms/{roomCode}/answer` | 답변 제출     |
| `/app/rooms/{roomCode}/next`   | 다음 라운드 투표 |

## 기술 스택

* Java 17
* Spring Boot
* Spring WebSocket
* STOMP/SockJS
* Spring Data JPA
* MySQL
* Gradle

## 실행 방법

Java 17과 MySQL이 필요합니다.

다음 환경 변수를 설정합니다.

```text
DB_URL=jdbc:mysql://localhost:3306/impress
DB_USERNAME=your_username
DB_PASSWORD=your_password
```

아래 명령어로 서버를 실행합니다.

```bash
./gradlew bootRun
```

## 커밋 컨벤션

| 태그         | 설명                 |
| ---------- | ------------------ |
| `feat`     | 새로운 기능 추가          |
| `fix`      | 버그 수정              |
| `docs`     | 문서 수정              |
| `style`    | 코드 포매팅 등 코드 스타일 변경 |
| `design`   | 사용자 UI 디자인 변경      |
| `test`     | 테스트 코드 추가 및 수정     |
| `refactor` | 코드 리팩토링            |
| `build`    | 빌드 파일 및 의존성 수정     |
| `ci`       | CI 설정 파일 수정        |
| `chore`    | 기타 수정              |
| `rename`   | 파일 또는 폴더명 변경       |
| `remove`   | 파일 또는 폴더 삭제        |

### 커밋 메시지 작성 형식

```text
태그: 변경 내용
```

### 작성 예시

```text
feat: 방 생성 API 구현
fix: 중복 닉네임 검증 오류 수정
docs: API 명세 업데이트
refactor: 참여자 조회 로직 분리
```
