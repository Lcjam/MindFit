import { Navigate, Outlet } from 'react-router-dom'
import { useAuthStore } from '../store/authStore'
import { isExpired } from '../utils/jwt'

export default function PrivateRoute() {
  const accessToken = useAuthStore((s) => s.accessToken)
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)
  const ok = isAuthenticated && !isExpired(accessToken)
  return ok ? <Outlet /> : <Navigate to="/login" replace />
}
