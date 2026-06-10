# mero-backend

> **Memory + Road** — 여행 일기 앱 Mero의 백엔드 API 서버

![Java](https://img.shields.io/badge/Java-17-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-brightgreen)

발자취(일기) · 다중 통화 경비/예산 · 사진/문서 관리 기능을 제공하며,
오프라인에서 만든 기록을 네트워크 복구 후 **중복 없이 동기화**하는 데 초점을 맞췄습니다.

## 기술 스택

Java 17 · Spring Boot 3.5 · JPA/Hibernate · PostgreSQL · Spring Security + JWT
· Apple/Google 소셜 로그인 · Supabase Storage(S3 호환, Presigned URL) · Railway

## 설계 포인트

- **오프라인 우선 동기화** — `clientId`(UUID) unique 제약으로 동일 요청 멱등성 보장
- **Presigned URL 스토리지** — 비공개 버킷에 시간 제한 URL로 접근
- **Soft Delete** — `@SQLRestriction` + DB FK 정책(`CASCADE`/`SET NULL`)으로 정합성 유지

## 주요 기능

| 도메인 | 기능 |
|--------|------|
| 인증 | 이메일 인증 회원가입 · 비밀번호 재설정 · Apple/Google 로그인 · JWT(Access+Refresh) |
| 여행 | 여행 CRUD · 대표 이미지 · 문서 첨부 · 메모 |
| 발자취 | 일기 CRUD(제목·내용·날씨·다중 GPS) · 사진 업로드(정렬 보존) |
| 경비 | 42개 통화 · 카테고리(기본+커스텀) · 화폐별 집계 |
| 예산 | 통화별 예산 · 환율 적용 |

## 실행

```bash
./gradlew bootRun   # 기본 프로필 local
```
API 문서: `http://localhost:8080/swagger-ui/index.html`

## 개발 기간

2025.11 ~ 2026.06
