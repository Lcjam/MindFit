---
description: 기능 구현 완료 후 실행. TDD 준수 + 비즈니스 규칙 누락 + API 설계 오류를 순서대로 검토한다.
---

구현이 완료되면 아래 항목을 순서대로 확인한다. 각 항목을 실제 코드에서 눈으로 확인하고 통과/실패를 판단한다.

---

## 1. TDD 준수 확인

- [ ] 테스트 파일이 구현 파일보다 **먼저** 커밋되었는가? (또는 동시에 작성되었는가?)
- [ ] `./gradlew test` 또는 `npx vitest run`이 **모두 통과**하는가?
- [ ] 새로 작성한 Service 클래스의 테스트 커버리지가 **90% 이상**인가?

---

## 2. 결제/Webhook 관련 구현 시

`payment/` 모듈 또는 Webhook 처리 코드를 건드렸다면 반드시 확인:

- [ ] `processPaymentComplete` (또는 동등한 메서드)에서 **3가지**가 모두 처리되는가?
  1. `session.status = CONFIRMED`
  2. `ChatRoom` 생성
  3. `questionnaire.isAccessible = true`
- [ ] 동일 Webhook이 두 번 수신될 때 **중복 처리가 방지**되는가? (멱등성)
- [ ] `PaymentWebhookHandlerTest`에 위 3가지 사이드이펙트가 **각각 독립적으로 검증**되는가?

---

## 3. 문진표 접근 제어 구현 시

`questionnaire/` 모듈을 건드렸다면 반드시 확인:

- [ ] `GET /questionnaires/{sessionId}` 진입 시 **3가지 조건 모두** 검사하는가?
  1. `ROLE_COUNSELOR` 인증
  2. `questionnaire.isAccessible == true`
  3. 요청자 ID == 해당 세션의 `counselorId`
- [ ] 조건 미충족 시 **403 Forbidden** (404가 아닌 403)이 반환되는가?
- [ ] `QuestionnaireControllerTest`에 각 조건 위반 케이스가 **별도 테스트**로 존재하는가?

---

## 4. 상담사 상태 관련 구현 시

`counselor/` 모듈의 쓰기(PUT/PATCH) 작업이라면:

- [ ] `counselorProfile.status == APPROVED`일 때만 편집 허용하는가?
- [ ] `PENDING` 또는 `REJECTED` 상태에서 편집 시도 시 **403**이 반환되는가?
- [ ] 상담사 목록 조회 시 `status == APPROVED`인 상담사만 반환되는가?

---

## 5. API 설계 준수 확인

- [ ] 엔드포인트 prefix가 `/api/v1/`인가?
- [ ] 모든 응답이 `ApiResponse<T>` 래퍼를 사용하는가?
- [ ] 에러 처리가 `GlobalExceptionHandler`를 통하는가? (컨트롤러 내부 try-catch 금지)
- [ ] `@PreAuthorize` 어노테이션이 역할이 필요한 모든 엔드포인트에 붙어있는가?

---

## 6. UI 텍스트 확인 (FE 작업 시)

- [ ] "치료", "치료사", "처방", "진단" 단어가 사용되지 않았는가?
- [ ] "상담", "상담사", "상담 세션"으로 표현되었는가?

---

문제 발견 시 해당 항목을 수정하고 다시 테스트를 실행한다. 모든 항목 통과 후 구현 완료로 간주한다.
