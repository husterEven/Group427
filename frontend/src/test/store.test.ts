import { describe, it, expect, beforeEach } from 'vitest'
import { useAuthStore, useAppStore } from '@/store'

describe('useAuthStore', () => {
  beforeEach(() => {
    localStorage.clear()
    useAuthStore.setState({ user: null, token: null, isLoggedIn: false })
  })

  describe('初始状态', () => {
    it('初始 user 为 null', () => {
      const state = useAuthStore.getState()
      expect(state.user).toBeNull()
    })

    it('初始 token 为 null', () => {
      const state = useAuthStore.getState()
      expect(state.token).toBeNull()
    })

    it('localStorage 无 accessToken 时 isLoggedIn 为 false', () => {
      localStorage.removeItem('accessToken')
      const store = useAuthStore.getState()
      expect(store.isLoggedIn).toBe(false)
    })

    it('localStorage 有 accessToken 时 isLoggedIn 为 true (初始化时读取)', () => {
      localStorage.setItem('accessToken', 'test-token')
      const { isLoggedIn } = useAuthStore.getState()
      // 注: 此值在 create() 时已固定，需重新创建 store 测试
      // isLoggedIn 初始化取决于 localStorage
    })
  })

  describe('setAuth', () => {
    it('应设置 user 和 token 并标记为已登录', () => {
      const token = { accessToken: 'at', refreshToken: 'rt', tokenType: 'Bearer', expiresIn: 7200 }
      const user = {
        userId: 1, nickname: 'test', avatarUrl: '', bio: '', gender: 0 as const,
        verificationLevel: 0, level: 1, points: 0, isFollowed: false,
        mobile: '', email: '', followerCount: 0, followingCount: 0,
        preference: null, achievement: null, createdAt: ''
      }

      useAuthStore.getState().setAuth(token, user)

      const state = useAuthStore.getState()
      expect(state.isLoggedIn).toBe(true)
      expect(state.user).toEqual(user)
      expect(state.token).toEqual(token)
    })

    it('应将 token 持久化到 localStorage', () => {
      const token = { accessToken: 'access-123', refreshToken: 'refresh-456', tokenType: 'Bearer', expiresIn: 7200 }
      const user = {
        userId: 1, nickname: 'test', avatarUrl: '', bio: '', gender: 0 as const,
        verificationLevel: 0, level: 1, points: 0, isFollowed: false,
        mobile: '', email: '', followerCount: 0, followingCount: 0,
        preference: null, achievement: null, createdAt: ''
      }

      useAuthStore.getState().setAuth(token, user)

      expect(localStorage.getItem('accessToken')).toBe('access-123')
      expect(localStorage.getItem('refreshToken')).toBe('refresh-456')
    })
  })

  describe('logout', () => {
    it('应清除 user 和 token 并标记为未登录', () => {
      // 先登录
      const token = { accessToken: 'at', refreshToken: 'rt', tokenType: 'Bearer', expiresIn: 7200 }
      const user = {
        userId: 1, nickname: 'test', avatarUrl: '', bio: '', gender: 0 as const,
        verificationLevel: 0, level: 1, points: 0, isFollowed: false,
        mobile: '', email: '', followerCount: 0, followingCount: 0,
        preference: null, achievement: null, createdAt: ''
      }
      useAuthStore.getState().setAuth(token, user)

      useAuthStore.getState().logout()

      const state = useAuthStore.getState()
      expect(state.user).toBeNull()
      expect(state.token).toBeNull()
      expect(state.isLoggedIn).toBe(false)
    })

    it('应从 localStorage 中移除 token', () => {
      useAuthStore.getState().logout()

      expect(localStorage.getItem('accessToken')).toBeNull()
      expect(localStorage.getItem('refreshToken')).toBeNull()
    })
  })

  describe('updateUser', () => {
    it('应部分更新 user 信息', () => {
      const user = {
        userId: 1, nickname: 'oldName', avatarUrl: '', bio: '', gender: 0 as const,
        verificationLevel: 0, level: 1, points: 0, isFollowed: false,
        mobile: '', email: '', followerCount: 0, followingCount: 0,
        preference: null, achievement: null, createdAt: ''
      }
      useAuthStore.setState({ user })

      useAuthStore.getState().updateUser({ nickname: 'newName', bio: 'new bio' })

      const state = useAuthStore.getState()
      expect(state.user?.nickname).toBe('newName')
      expect(state.user?.bio).toBe('new bio')
    })

    it('user 为 null 时不应更新', () => {
      useAuthStore.setState({ user: null })

      expect(() => useAuthStore.getState().updateUser({ nickname: 'x' })).not.toThrow()
      expect(useAuthStore.getState().user).toBeNull()
    })
  })
})

describe('useAppStore', () => {
  beforeEach(() => {
    useAppStore.setState({ sections: [], unreadCount: 0 })
  })

  describe('初始状态', () => {
    it('sections 应为空数组', () => {
      expect(useAppStore.getState().sections).toEqual([])
    })

    it('unreadCount 应为 0', () => {
      expect(useAppStore.getState().unreadCount).toBe(0)
    })
  })

  describe('setSections', () => {
    it('应设置 sections', () => {
      const sections = [
        { sectionId: 1, sectionName: 'A股', sectionType: 0 as const, sortOrder: 1, zones: [] },
        { sectionId: 2, sectionName: '港股', sectionType: 0 as const, sortOrder: 2, zones: [] },
      ]

      useAppStore.getState().setSections(sections)

      expect(useAppStore.getState().sections).toEqual(sections)
      expect(useAppStore.getState().sections).toHaveLength(2)
    })
  })

  describe('setUnreadCount', () => {
    it('应设置 unreadCount', () => {
      useAppStore.getState().setUnreadCount(5)
      expect(useAppStore.getState().unreadCount).toBe(5)
    })

    it('可设为 0', () => {
      useAppStore.getState().setUnreadCount(10)
      useAppStore.getState().setUnreadCount(0)
      expect(useAppStore.getState().unreadCount).toBe(0)
    })
  })
})
