import React, { useEffect } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { useAuthStore } from '@/store'
import { userApi } from '@/services/api'

interface Props {
  children: React.ReactNode
  requireAdmin?: boolean
}

const AuthGuard: React.FC<Props> = ({ children, requireAdmin }) => {
  const { isLoggedIn, user, setAuth, logout } = useAuthStore()
  const location = useLocation()

  useEffect(() => {
    if (isLoggedIn && !user) {
      userApi.getMe()
        .then((res) => {
          useAuthStore.setState({ user: res.data.data })
        })
        .catch(() => logout())
    }
  }, [isLoggedIn, user])

  if (!isLoggedIn) {
    return <Navigate to="/login" state={{ from: location }} replace />
  }

  if (requireAdmin && user && user.verificationLevel < 3) {
    return <Navigate to="/" replace />
  }

  return <>{children}</>
}

export default AuthGuard
