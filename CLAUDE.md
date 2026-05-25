# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Status

기획 완료, MVP 개발 단계 진입. 기획 문서 전체: `docs/` 참조.

---

## 사용 가능한 Skills & Hooks

| 커맨드 | 시점 | 역할 |
|--------|------|------|
| `/implement` | 구현 **시작 전** | 파일 위치 결정 → 테스트 먼저 작성 → TDD 사이클 진행 절차 |
| `/impl-check` | 구현 **완료 후** | TDD 준수 + 비즈니스 규칙 누락 + API 설계 오류 자기 검토 |
| **Hook (자동)** | Java/TSX 파일 작성 시 | 테스트 파일 없으면 즉시 경고 출력 |

---

## 서비스 개요

심리상담 버티컬 마켓플레이스. 임상·상담심리 석사 이상 자격의 상담사(공급)와 내담자(수요)를 연결.

**역할**: `ROLE_CLIENT` / `ROLE_COUNSELOR` / `ROLE_ADMIN`

---

## 기술 스택 (확정)

**Backend**: Java Spring Boot 3.x · Spring Security 6 · Spring Data JPA · Spring WebSocket(STOMP) · MySQL 8.x · JWT · AWS S3 · Spring Mail · Gradle

**Frontend**: React 18 + TypeScript + Vite · TanStack Query · Zustand · React Router v6 · Tailwind CSS · SockJS + @stomp/stompjs · React Hook Form + Zod · axios

**결제**: 토스페이먼츠 (Webhook 방식)

---

## 프로젝트 구조

모노레포: `backend/` + `frontend/`

```
# Backend
com.mindfit/
├── auth/           # JWT, Spring Security
├── user/           # User 엔티티
├── counselor/      # 프로필, 자격증, 가용시간
├── session/        # 예약, 상태 관리
├── questionnaire/  # 문진표
├── payment/        # 결제, Webhook
├── chat/           # WebSocket, 채팅방, 메시지
├── review/         # 리뷰
├── admin/          # 관리자
└── common/         # ApiResponse, GlobalExceptionHandler, S3Service

# Frontend src/
├── api/            # axios 인스턴스 + API 함수
├── components/     # 공통 UI
├── pages/{auth,client,counselor,admin}/
├── hooks/          # useWebSocket 등
├── store/          # Zustand (authStore.ts)
├── types/
└── utils/
```

---

## API 설계 원칙

- Prefix: `/api/v1/`
- 인증: `Authorization: Bearer {accessToken}`
- 응답: `ApiResponse<T>` 래퍼 통일
- 에러: `GlobalExceptionHandler` 단일 처리 (컨트롤러 내부 try-catch 금지)
- 역할 제어: `@PreAuthorize("hasRole('X')")` — ROLE_CLIENT / ROLE_COUNSELOR / ROLE_ADMIN
- WebSocket: STOMP, `/ws` 엔드포인트, 구독 `/topic/chat/{roomId}`, 전송 `/app/chat/{roomId}/send`

---

## 핵심 비즈니스 규칙

구현 시 반드시 준수. 세부 검토 절차는 `/impl-check` 참조.

**결제 Webhook 후처리** — 아래 3가지가 하나의 트랜잭션으로 처리되어야 한다:
1. `session.status = CONFIRMED`
2. `ChatRoom` 자동 생성
3. `questionnaire.isAccessible = true`

**문진표 접근** — 아래 3가지 조건 모두 충족해야 허용:
1. `ROLE_COUNSELOR`
2. `questionnaire.isAccessible == true`
3. 요청자 == 해당 세션의 `counselorId`

**상담사 프로필 편집** — `status == APPROVED`일 때만 허용

**환불**: 24h 이상 전 → 100% / 24h 미만 → 50% / 시작 후 → 0%

**수수료**: 세션 금액의 20% (정산액 = 금액 × 0.8)

**언어**: UI 텍스트에 "치료" 사용 금지 → "상담"으로 표현

---

## 개발 원칙: TDD 필수

구현 코드를 먼저 작성하는 것은 금지. 반드시 테스트 먼저.

| 레이어 | 커버리지 목표 | 도구 |
|--------|------------|------|
| Service | 90% | JUnit 5 + Mockito |
| Controller | 80% | `@WebMvcTest` + MockMvc |
| Repository | 70% | `@DataJpaTest` + H2 |
| Frontend | 70% | Vitest + React Testing Library + MSW |

구현 절차: `/implement` 스킬 참조

---

## Workflow: Orchestrator Protocol

모든 작업에서 Claude는 **Lead Orchestrator**로 기능한다.

작업 수신 → 분해 → 서브에이전트 배정 → 검토 → 피드백 루프 → 최종 보고

**Pro-Workflow 플러그인 활용** (`rohitg00/pro-workflow`):

| 상황 | 커맨드/스킬 |
|------|------------|
| 복잡한 기능 개발 | `/develop` |
| 멀티 에이전트 조율 | `orchestrate`, `agent-teams` |
| 계획 수립 | `planner` 에이전트 |
| 코드 리뷰 | `reviewer` 에이전트, `/commit` |
| 세션 마무리 | `/wrap-up` |

---

## 파일 형식 규칙

기획 문서 → 반드시 `.html` (`.md` 금지). `docs/{planning,requirements,design,architecture,research}/` 하위에 저장.
