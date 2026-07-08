import { describe, it, expect } from 'vitest'
import { isExpired } from './jwt'

// payload만 의미 있는 3파트 JWT를 만든다 (서명은 검증하지 않음)
function makeToken(payload: Record<string, unknown>): string {
  const encoded = btoa(JSON.stringify(payload))
  return `eyJhbGciOiJIUzI1NiJ9.${encoded}.sig`
}

describe('isExpired', () => {
  it('토큰이 null이면 만료로 처리한다', () => {
    expect(isExpired(null)).toBe(true)
  })

  it('exp가 과거인 토큰은 만료로 처리한다', () => {
    const token = makeToken({ exp: 1 }) // 1970년 직후
    expect(isExpired(token)).toBe(true)
  })

  it('exp가 미래인 토큰은 만료가 아니다', () => {
    const token = makeToken({ exp: Math.floor(Date.now() / 1000) + 3600 })
    expect(isExpired(token)).toBe(false)
  })

  it('디코드할 수 없는 깨진 토큰은 만료로 처리한다', () => {
    expect(isExpired('not-a-jwt')).toBe(true)
  })

  it('exp 클레임이 없으면 만료로 처리한다', () => {
    const token = makeToken({ sub: 'user' })
    expect(isExpired(token)).toBe(true)
  })

  it('exp가 숫자가 아니면 만료로 처리한다', () => {
    const token = makeToken({ exp: 'soon' })
    expect(isExpired(token)).toBe(true)
  })
})
