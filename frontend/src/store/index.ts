import { create } from 'zustand'
import type { UserDetail, TokenResponse, Section, Notification } from '@/types'

interface AuthState {
  user: UserDetail | null
  token: TokenResponse | null
  isLoggedIn: boolean
  setAuth: (token: TokenResponse, user: UserDetail) => void
  logout: () => void
  updateUser: (user: Partial<UserDetail>) => void
}

export const useAuthStore = create<AuthState>((set, get) => ({
  user: null,
  token: null,
  isLoggedIn: !!localStorage.getItem('accessToken'),
  setAuth: (token, user) => {
    localStorage.setItem('accessToken', token.accessToken)
    localStorage.setItem('refreshToken', token.refreshToken)
    set({ user, token, isLoggedIn: true })
  },
  logout: () => {
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    set({ user: null, token: null, isLoggedIn: false })
  },
  updateUser: (partial) => {
    const cur = get().user
    if (cur) set({ user: { ...cur, ...partial } as UserDetail })
  },
}))

interface AppState {
  sections: Section[]
  unreadCount: number
  dmUnreadCount: number
  setSections: (sections: Section[]) => void
  setUnreadCount: (n: number) => void
  setDmUnreadCount: (n: number) => void
}

export const useAppStore = create<AppState>((set) => ({
  sections: [],
  unreadCount: 0,
  dmUnreadCount: 0,
  setSections: (sections) => set({ sections }),
  setUnreadCount: (n) => set({ unreadCount: n }),
  setDmUnreadCount: (n) => set({ dmUnreadCount: n }),
}))
