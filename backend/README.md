# ShiftRhythm Backend

교대근무 리듬 코칭 앱의 REST API 서버입니다. Java 21, Spring Boot, MySQL, Redis를 사용합니다.

## 사전 요구사항

- 실행 중인 Docker Desktop (Docker Compose v2 포함)
- Java 21 (컨테이너 밖에서 Gradle 테스트를 실행할 경우)

## 로컬 실행

1. `.env.example`을 복사해 `.env` 파일을 만듭니다.
2. `.env`의 `MYSQL_PASSWORD`, `MYSQL_ROOT_PASSWORD`, `JWT_SECRET`, `GOOGLE_CLIENT_ID`를 실제 로컬 값으로 교체합니다.
3. 다음 명령을 실행합니다.

```powershell
docker compose up --build
```

API는 `http://localhost:8080`에서 실행됩니다. MySQL과 Redis 데이터는 Docker 볼륨에 보존됩니다.

## 환경변수

| 변수 | 설명 |
| --- | --- |
| `MYSQL_DATABASE` | MySQL 데이터베이스 이름 |
| `MYSQL_USER` / `MYSQL_PASSWORD` | 애플리케이션 MySQL 계정 |
| `MYSQL_ROOT_PASSWORD` | MySQL root 비밀번호 |
| `JWT_SECRET` | 32바이트 이상을 Base64로 인코딩한 JWT 서명 키 |
| `GOOGLE_CLIENT_ID` | Google OAuth 웹 클라이언트 ID |
| `JWT_ISSUER` | JWT issuer 값 |

`.env` 파일은 Git에서 제외됩니다. 비밀값을 저장소에 커밋하지 마세요.

## 검증

```powershell
.\gradlew.bat test
docker compose --env-file .env.example config
```

## 현재 기반 기능

- Flyway V1 사용자 스키마
- MySQL·Redis Docker Compose 구성
- `/api/v1/**` 보호 및 표준 401 JSON 응답
- 향후 Google ID Token 검증 및 자체 JWT 발급을 위한 설정 계약
