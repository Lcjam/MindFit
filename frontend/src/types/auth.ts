export type UserRole = 'ROLE_CLIENT' | 'ROLE_COUNSELOR' | 'ROLE_ADMIN'

export interface SignupRequest {
  email: string
  password: string
  name: string
  birthDate?: string   // ISO date string "YYYY-MM-DD"
  role: UserRole
}

export interface LoginRequest {
  email: string
  password: string
}

export interface RefreshRequest {
  refreshToken: string
}

export interface TokenResponse {
  accessToken: string
  refreshToken: string
  userId: number
  role: string
}

export interface AuthUser {
  userId: number
  role: string
}

export interface ApiResponse<T> {
  success: boolean
  message?: string
  data: T
}
