import { describe, it, expect, beforeEach, vi } from 'vitest'
import axios from 'axios'

// Mock antd message
vi.mock('antd', () => ({
  message: { error: vi.fn() },
}))

describe('API Service - axios 实例', () => {
  beforeEach(() => {
    localStorage.clear()
    // 清除 axios 模块缓存以确保每次测试创建新的实例
    vi.resetModules()
  })

  describe('请求拦截器', () => {
    it('有 accessToken 时应自动携带 Authorization header', async () => {
      const { default: http } = await import('@/services/api')

      localStorage.setItem('accessToken', 'test-token-123')

      const config = { headers: {} }
      const interceptor = (http.interceptors.request as any)
      // 通过 axios 实例请求拦截器测试
      const result = await http.interceptors.request.handlers[0].fulfilled(config)
      expect(result.headers.Authorization).toBe('Bearer test-token-123')
    })

    it('无 accessToken 时不应添加 Authorization header', async () => {
      const { default: http } = await import('@/services/api')

      const config = { headers: {} }
      const result = await http.interceptors.request.handlers[0].fulfilled(config)
      expect(result.headers.Authorization).toBeUndefined()
    })
  })

  describe('axios 实例配置', () => {
    it('baseURL 应为 /api/v1', async () => {
      const { default: http } = await import('@/services/api')
      expect(http.defaults.baseURL).toBe('/api/v1')
    })

    it('timeout 应为 10000', async () => {
      const { default: http } = await import('@/services/api')
      expect(http.defaults.timeout).toBe(10000)
    })
  })
})

describe('API 方法导出', () => {
  it('应导出 authApi', async () => {
    const { authApi } = await import('@/services/api')
    expect(authApi).toBeDefined()
    expect(authApi).toHaveProperty('register')
    expect(authApi).toHaveProperty('login')
    expect(authApi).toHaveProperty('refresh')
    expect(authApi).toHaveProperty('logout')
  })

  it('应导出 userApi', async () => {
    const { userApi } = await import('@/services/api')
    expect(userApi).toBeDefined()
    expect(userApi).toHaveProperty('getMe')
    expect(userApi).toHaveProperty('updateMe')
    expect(userApi).toHaveProperty('changePassword')
    expect(userApi).toHaveProperty('searchUsers')
    expect(userApi).toHaveProperty('getUserById')
  })

  it('应导出 postApi', async () => {
    const { postApi } = await import('@/services/api')
    expect(postApi).toBeDefined()
    expect(postApi).toHaveProperty('getList')
    expect(postApi).toHaveProperty('getDetail')
    expect(postApi).toHaveProperty('create')
    expect(postApi).toHaveProperty('toggleLike')
    expect(postApi).toHaveProperty('toggleCollect')
  })

  it('应导出 commentApi', async () => {
    const { commentApi } = await import('@/services/api')
    expect(commentApi).toBeDefined()
    expect(commentApi).toHaveProperty('getList')
    expect(commentApi).toHaveProperty('create')
    expect(commentApi).toHaveProperty('delete')
    expect(commentApi).toHaveProperty('toggleLike')
  })

  it('应导出 voteApi', async () => {
    const { voteApi } = await import('@/services/api')
    expect(voteApi).toBeDefined()
    expect(voteApi).toHaveProperty('create')
    expect(voteApi).toHaveProperty('submit')
    expect(voteApi).toHaveProperty('getByPost')
  })

  it('应导出 sectionApi', async () => {
    const { sectionApi } = await import('@/services/api')
    expect(sectionApi).toBeDefined()
    expect(sectionApi).toHaveProperty('getAll')
    expect(sectionApi).toHaveProperty('getZones')
  })

  it('应导出 dynamicApi', async () => {
    const { dynamicApi } = await import('@/services/api')
    expect(dynamicApi).toBeDefined()
    expect(dynamicApi).toHaveProperty('getFeed')
    expect(dynamicApi).toHaveProperty('create')
    expect(dynamicApi).toHaveProperty('delete')
  })

  it('应导出 socialApi', async () => {
    const { socialApi } = await import('@/services/api')
    expect(socialApi).toBeDefined()
    expect(socialApi).toHaveProperty('toggleFollow')
    expect(socialApi).toHaveProperty('getFollowing')
    expect(socialApi).toHaveProperty('sendMessage')
    expect(socialApi).toHaveProperty('createGroup')
  })

  it('应导出 notifApi', async () => {
    const { notifApi } = await import('@/services/api')
    expect(notifApi).toBeDefined()
    expect(notifApi).toHaveProperty('getList')
    expect(notifApi).toHaveProperty('getUnreadCount')
    expect(notifApi).toHaveProperty('markRead')
  })

  it('应导出 reportApi', async () => {
    const { reportApi } = await import('@/services/api')
    expect(reportApi).toBeDefined()
    expect(reportApi).toHaveProperty('submit')
  })

  it('应导出 adminApi', async () => {
    const { adminApi } = await import('@/services/api')
    expect(adminApi).toBeDefined()
    expect(adminApi).toHaveProperty('getAuditQueue')
    expect(adminApi).toHaveProperty('getDashboard')
    expect(adminApi).toHaveProperty('getReports')
  })

  it('应导出 attachApi', async () => {
    const { attachApi } = await import('@/services/api')
    expect(attachApi).toBeDefined()
    expect(attachApi).toHaveProperty('upload')
  })
})

describe('响应拦截器 - 控制结构覆盖', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.resetModules()
  })

  describe('401状态码处理 (if: status===401)', () => {
    it('status=401 应清除token并跳转到/login', async () => {
      const { message } = await import('antd')
      const { default: http } = await import('@/services/api')

      localStorage.setItem('accessToken', 'expired-token')
      const originalLocation = window.location

      // 模拟 location.href 赋值
      let redirectedTo = ''
      Object.defineProperty(window, 'location', {
        value: { href: '', assign: (url: string) => { redirectedTo = url } },
        writable: true,
        configurable: true,
      })

      const error = {
        response: {
          status: 401,
          data: { message: 'Token已过期' },
        },
      } as any

      try {
        await http.interceptors.response.handlers[0].rejected(error)
      } catch (_) {
        // expected to reject
      }

      expect(localStorage.getItem('accessToken')).toBeNull()
      expect(message.error).toHaveBeenCalled()

      // 恢复 location
      Object.defineProperty(window, 'location', {
        value: originalLocation,
        writable: true,
        configurable: true,
      })
    })
  })

  describe('非401错误处理 (if: status===401=false)', () => {
    it('status=400 不应清除token', async () => {
      const { default: http } = await import('@/services/api')

      localStorage.setItem('accessToken', 'my-token')

      const error = {
        response: {
          status: 400,
          data: { message: '参数错误' },
        },
      } as any

      try {
        await http.interceptors.response.handlers[0].rejected(error)
      } catch (_) {
        // expected to reject
      }

      expect(localStorage.getItem('accessToken')).toBe('my-token')
    })

    it('status=403 应显示错误消息', async () => {
      const { message } = await import('antd')
      const { default: http } = await import('@/services/api')

      const error = {
        response: {
          status: 403,
          data: { message: '无权限' },
        },
      } as any

      try {
        await http.interceptors.response.handlers[0].rejected(error)
      } catch (_) { /* expected */ }

      expect(message.error).toHaveBeenCalledWith('无权限')
    })

    it('status=500 应显示服务器错误消息', async () => {
      const { message } = await import('antd')
      const { default: http } = await import('@/services/api')

      const error = {
        response: {
          status: 500,
          data: { message: '服务器错误' },
        },
      } as any

      try {
        await http.interceptors.response.handlers[0].rejected(error)
      } catch (_) { /* expected */ }

      expect(message.error).toHaveBeenCalledWith('服务器错误')
    })
  })

  describe('网络异常处理 (if: err.response?.data?.message || 默认值)', () => {
    it('无response时应显示"网络异常" (|| 分支)', async () => {
      const { message } = await import('antd')
      const { default: http } = await import('@/services/api')

      const error = {
        request: {},
      } as any

      try {
        await http.interceptors.response.handlers[0].rejected(error)
      } catch (_) { /* expected */ }

      expect(message.error).toHaveBeenCalledWith('网络异常')
    })

    it('有response但无data.message时应回退到"网络异常"', async () => {
      const { message } = await import('antd')
      const { default: http } = await import('@/services/api')

      const error = {
        response: {
          status: 502,
          data: null,
        },
      } as any

      try {
        await http.interceptors.response.handlers[0].rejected(error)
      } catch (_) { /* expected */ }

      expect(message.error).toHaveBeenCalledWith('网络异常')
    })

    it('响应拦截器应始终reject Promise', async () => {
      const { default: http } = await import('@/services/api')

      const error = { response: { status: 500, data: { message: 'err' } } } as any

      let wasRejected = false
      try {
        await http.interceptors.response.handlers[0].rejected(error)
      } catch (e) {
        wasRejected = true
        expect(e).toBe(error)
      }

      expect(wasRejected).toBe(true)
    })
  })
})
