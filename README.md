# mero-backend

> **Memory + Road** — 여행길을 기록하는 여행 일기 앱의 백엔드 API 서버

![Status](https://img.shields.io/badge/status-in%20development-yellow)
![Java](https://img.shields.io/badge/Java-17-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-brightgreen)

## 프로젝트 개요

여행 중 사용할 모바일 앱 **Mero**의 백엔드입니다.
발자취(일기) 작성, 경비 관리, 사진 업로드, 네이버 블로그 발행 등의 기능을 제공합니다.

## 기술 스택

| 분류 | 기술 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 3.5 |
| Database | PostgreSQL (운영), H2 (로컬) |
| ORM | JPA / Hibernate |
| Auth | JWT (Access + Refresh Token) |
| Security | Spring Security |
| Storage | AWS S3 |
| Docs | SpringDoc OpenAPI (Swagger UI) |
| Deploy | Railway |

## 주요 기능

### 인증 (`/api/auth`)
- 회원가입 / 로그인
- Access Token 재발급 (Refresh Token 기반)
- 로그아웃 (Refresh Token 무효화)

### 여행 (`/api/trips`)
- 여행 CRUD
- 대표 이미지 업로드/삭제 (S3)
- 여행 문서 첨부 (PDF 등, S3)
- 여행 메모 CRUD

### 발자취 (`/api/trips/{tripId}/footprints`)
- 발자취(일기) CRUD — 제목, 내용, 날짜, 날씨, GPS 위치 기록
- 사진 업로드/삭제 (S3, 장당 제한 적용)

### 경비 (`/api/trips/{tripId}/expenses`)
- 경비 CRUD — 공식 환율 또는 커스텀 환율 적용
- 카테고리별 분류 (기본 카테고리 제공 + 사용자 정의 카테고리)
- 화폐별 사용량 집계

### 예산 (`/api/trips/{tripId}/budgets`)
- 통화별 예산 설정 (같은 통화 1개 제한)
- 예산 CRUD

### 네이버 소셜 연동 (`/api/social/naver`)
- 네이버 OAuth 2.0 연동 / 해제
- 발자취를 네이버 블로그에 발행 (`/api/trips/{tripId}/footprints/{footprintId}/publish/naver`)

## 로컬 실행

**사전 요구사항:** JDK 17, Gradle

**1. 환경변수 설정**

`src/main/resources/application-local.yaml`을 직접 작성하거나 아래 항목을 참고해 설정합니다.

| 변수명 | 설명 |
|--------|------|
| `JWT_SECRET` | JWT 서명 키 |
| `JWT_ACCESS_TOKEN_VALIDITY` | Access Token 만료 시간 (ms, 기본 1시간) |
| `JWT_REFRESH_TOKEN_VALIDITY` | Refresh Token 만료 시간 (ms, 기본 24시간) |
| `NAVER_CLIENT_ID` | 네이버 OAuth 클라이언트 ID |
| `NAVER_CLIENT_SECRET` | 네이버 OAuth 클라이언트 시크릿 |
| `NAVER_REDIRECT_URI` | 네이버 OAuth 리다이렉트 URI |
| `AWS_ACCESS_KEY` | AWS 액세스 키 |
| `AWS_SECRET_KEY` | AWS 시크릿 키 |
| `S3_BUCKET_NAME` | S3 버킷 이름 |

**2. 실행**

```bash
./gradlew bootRun
```

**3. API 문서 확인**

Swagger UI: `http://localhost:8080/swagger-ui/index.html`

## 개발 기간

2025.11 ~ 2026.03