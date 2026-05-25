---
description: 새 기능 구현 시작 전 실행. 파일 위치 결정 → 테스트 계획 → TDD 사이클 진행.
---

구현 작업을 시작할 때 반드시 이 순서를 따른다. 구현 코드를 먼저 작성하지 않는다.

---

## 1단계 — 파일 위치 결정

구현할 기능이 어느 모듈에 속하는지 먼저 결정한다.

**Backend 패키지** (`com.mindfit.{module}/`):
- `auth/` — JWT, Security, 토큰 발급/검증
- `user/` — User 엔티티, 공통 유저 조회
- `counselor/` — 프로필, 자격증, 가용시간
- `session/` — 예약 생성, 상태 전이
- `questionnaire/` — 문진표 제출/열람
- `payment/` — 결제 초기화, Webhook, 환불
- `chat/` — WebSocket 설정, 채팅방, 메시지
- `review/` — 리뷰 작성/조회
- `admin/` — 관리자 심사 기능
- `common/` — ApiResponse, GlobalExceptionHandler, S3Service

**Frontend 경로** (`src/`):
- `pages/auth/` — 로그인, 회원가입
- `pages/client/` — 내담자 화면 (홈, 상담사 상세, 예약, 결제)
- `pages/counselor/` — 상담사 화면 (대시보드, 프로필, 정산)
- `pages/admin/` — 관리자 화면 (심사 대시보드)
- `api/` — API 함수 (axios 호출)
- `store/` — Zustand 스토어
- `hooks/` — 커스텀 훅

---

## 2단계 — 테스트 파일 먼저 생성 (Red)

구현 파일을 만들기 전에 테스트 파일을 먼저 생성한다.

| 구현 대상 | 테스트 파일 위치 |
|----------|----------------|
| `com.mindfit.X/XxxService.java` | `src/test/java/com/mindfit/X/XxxServiceTest.java` |
| `com.mindfit.X/XxxController.java` | `src/test/java/com/mindfit/X/XxxControllerTest.java` |
| `src/pages/X/XxxPage.tsx` | `src/pages/X/XxxPage.test.tsx` (또는 `__tests__/`) |
| `src/api/xxxApi.ts` | `src/api/xxxApi.test.ts` |
| `src/utils/xxx.ts` | `src/utils/xxx.test.ts` |

**테스트 네이밍**:
```
// BE: 메서드명_상황_기대결과
void createSession_timeConflict_throwsDuplicateTimeException()

// FE: 상황 + 기대 동작
it('calls confirmPayment API on success page load')
```

---

## 3단계 — 실패 확인

테스트를 작성한 후 실행해서 실제로 실패(Red)하는지 확인한다.

```bash
# Backend (해당 테스트 클래스만)
./gradlew test --tests "com.mindfit.X.XxxServiceTest"

# Frontend
npx vitest run src/path/to/XxxPage.test.tsx
```

컴파일 에러가 아닌 **테스트 실패**가 확인되면 다음 단계로 진행한다.

---

## 4단계 — 최소 구현 (Green)

테스트를 통과하는 최소한의 코드만 작성한다. 과도한 추상화나 미래 요구사항을 위한 코드는 금지.

**BE 구현 순서**: Entity → Repository → Service → Controller → SecurityConfig 추가
**FE 구현 순서**: API 함수(api/) → 커스텀 훅(hooks/) → 컴포넌트(pages/)

---

## 5단계 — 통과 확인 (Green)

```bash
# Backend 전체
./gradlew test

# Frontend 전체
npx vitest run
```

모든 테스트 통과 후 다음 단계 진행.

---

## 6단계 — 리팩토링 (Refactor)

테스트가 통과한 상태를 유지하면서 코드 품질을 개선한다.
리팩토링 후 테스트를 다시 실행해서 여전히 통과하는지 확인한다.

---

## 공통 체크사항

- API 엔드포인트 prefix: `/api/v1/`
- 응답 형식: `ApiResponse<T>` 래퍼 필수
- 에러: `GlobalExceptionHandler`에서 처리 (개별 try-catch 금지)
- 역할 제어: `@PreAuthorize("hasRole('X')")` 컨트롤러 메서드에 적용
