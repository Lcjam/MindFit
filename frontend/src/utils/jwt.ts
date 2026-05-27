/**
 * JWT accessToken의 만료 여부를 검증한다. (외부 라이브러리 없이 순수 함수)
 * 디코드 실패·exp 누락·형식 오류는 모두 만료(=비인증)로 처리한다.
 */
export function isExpired(token: string | null): boolean {
  if (!token) return true
  try {
    const payload = JSON.parse(atob(token.split('.')[1]))
    if (typeof payload.exp !== 'number') return true
    return payload.exp * 1000 < Date.now()
  } catch {
    return true
  }
}
