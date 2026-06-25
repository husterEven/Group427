import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import AuthGuard from '@/components/AuthGuard'
import { useAuthStore } from '@/store'

vi.mock('@/services/api', () => ({
  userApi: {
    getMe: vi.fn(),
    updateMe: vi.fn(),
    changePassword: vi.fn(),
    getPreference: vi.fn(),
    updatePreference: vi.fn(),
    getPrivacy: vi.fn(),
    updatePrivacy: vi.fn(),
    getAchievement: vi.fn(),
    getRiskAssessment: vi.fn(),
    submitRiskAssessment: vi.fn(),
    getVerification: vi.fn(),
    submitVerification: vi.fn(),
    searchUsers: vi.fn(),
    getUserById: vi.fn(),
  },
}))

import { userApi } from '@/services/api'

const TestChild = () => <div>Protected Content</div>

function renderGuard(requireAdmin = false) {
  return render(
    <MemoryRouter initialEntries={['/protected']}>
      <Routes>
        <Route path="/login" element={<div>Login Page</div>} />
        <Route path="/" element={<div>Home Page</div>} />
        <Route
          path="/protected"
          element={
            <AuthGuard requireAdmin={requireAdmin}>
              <TestChild />
            </AuthGuard>
          }
        />
      </Routes>
    </MemoryRouter>
  )
}

describe('AuthGuard 路由守卫', () => {
  beforeEach(() => {
    useAuthStore.setState({ user: null, token: null, isLoggedIn: false })
    vi.clearAllMocks()
  })

  afterEach(() => {
    useAuthStore.setState({ user: null, token: null, isLoggedIn: false })
  })

  describe('未登录状态', () => {
    it('应重定向到 /login', () => {
      renderGuard()
      expect(screen.getByText('Login Page')).toBeInTheDocument()
    })

    it('未登录不应显示受保护内容', () => {
      renderGuard()
      expect(screen.queryByText('Protected Content')).not.toBeInTheDocument()
    })
  })

  describe('已登录状态', () => {
    it('应显示受保护内容', () => {
      useAuthStore.setState({
        isLoggedIn: true,
        user: {
          userId: 1, nickname: 'test', avatarUrl: '', bio: '', gender: 0,
          verificationLevel: 0, level: 1, points: 0, isFollowed: false,
          mobile: '', email: '', followerCount: 0, followingCount: 0,
          preference: null, achievement: null, createdAt: ''
        },
        token: { accessToken: 'at', refreshToken: 'rt', tokenType: 'Bearer', expiresIn: 7200 }
      })

      renderGuard()
      expect(screen.getByText('Protected Content')).toBeInTheDocument()
    })

    it('已登录不应重定向到 /login', () => {
      useAuthStore.setState({
        isLoggedIn: true,
        user: {
          userId: 1, nickname: 'test', avatarUrl: '', bio: '', gender: 0,
          verificationLevel: 0, level: 1, points: 0, isFollowed: false,
          mobile: '', email: '', followerCount: 0, followingCount: 0,
          preference: null, achievement: null, createdAt: ''
        },
        token: { accessToken: 'at', refreshToken: 'rt', tokenType: 'Bearer', expiresIn: 7200 }
      })

      renderGuard()
      expect(screen.queryByText('Login Page')).not.toBeInTheDocument()
    })
  })

  describe('管理员权限检查', () => {
    it('requireAdmin=true 且非管理员(verificationLevel<3)应重定向到首页', () => {
      useAuthStore.setState({
        isLoggedIn: true,
        user: {
          userId: 1, nickname: 'test', avatarUrl: '', bio: '', gender: 0,
          verificationLevel: 1, level: 1, points: 0, isFollowed: false,
          mobile: '', email: '', followerCount: 0, followingCount: 0,
          preference: null, achievement: null, createdAt: ''
        },
        token: { accessToken: 'at', refreshToken: 'rt', tokenType: 'Bearer', expiresIn: 7200 }
      })

      renderGuard(true)
      expect(screen.getByText('Home Page')).toBeInTheDocument()
      expect(screen.queryByText('Protected Content')).not.toBeInTheDocument()
    })

    it('requireAdmin=true 且管理员(verificationLevel>=3)应显示内容', () => {
      useAuthStore.setState({
        isLoggedIn: true,
        user: {
          userId: 1, nickname: 'admin', avatarUrl: '', bio: '', gender: 0,
          verificationLevel: 3, level: 5, points: 100, isFollowed: false,
          mobile: '', email: '', followerCount: 0, followingCount: 0,
          preference: null, achievement: null, createdAt: ''
        },
        token: { accessToken: 'at', refreshToken: 'rt', tokenType: 'Bearer', expiresIn: 7200 }
      })

      renderGuard(true)
      expect(screen.getByText('Protected Content')).toBeInTheDocument()
    })

    it('requireAdmin=true 且 user 为 null 时应正常处理(不报错)', () => {
      vi.mocked(userApi.getMe).mockResolvedValue({
        data: { code: 200, message: 'ok', data: null }
      } as any)

      useAuthStore.setState({
        isLoggedIn: true,
        user: null,
        token: { accessToken: 'at', refreshToken: 'rt', tokenType: 'Bearer', expiresIn: 7200 }
      })

      renderGuard(true)
      // user为null时isLoggedIn=true会触发getMe，因为有mock所以不报错
    })
  })

  describe('自动拉取用户信息', () => {
    it('isLoggedIn=true 但 user=null 时应调用 getMe', async () => {
      const mockUser = {
        userId: 2, nickname: 'fetched', avatarUrl: '', bio: '', gender: 0 as const,
        verificationLevel: 1, level: 2, points: 50, isFollowed: false,
        mobile: '', email: '', followerCount: 0, followingCount: 0,
        preference: null, achievement: null, createdAt: ''
      }
      vi.mocked(userApi.getMe).mockResolvedValue({
        data: { code: 200, message: 'ok', data: mockUser }
      } as any)

      useAuthStore.setState({
        isLoggedIn: true,
        user: null,
        token: { accessToken: 'at', refreshToken: 'rt', tokenType: 'Bearer', expiresIn: 7200 }
      })

      renderGuard()

      await waitFor(() => {
        expect(userApi.getMe).toHaveBeenCalled()
      })

      expect(screen.getByText('Protected Content')).toBeInTheDocument()
    })
  })
})
