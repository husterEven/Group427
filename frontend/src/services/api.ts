import axios, { AxiosError } from 'axios'
import { message } from 'antd'
import type {
  ApiResponse,
  PaginatedData,
  TokenResponse,
  LoginRequest,
  RegisterRequest,
  UserDetail,
  UserPreference,
  PrivacySetting,
  UserAchievement,
  RiskAssessment,
  VerificationRecord,
  FollowUser,
  Post,
  PostCreate,
  Comment,
  Attachment,
  Dynamic,
  Section,
  Zone,
  VoteDetail,
  VoteCreate,
  PrivateMessage,
  Conversation,
  Group,
  GroupMember,
  GroupPost,
  Notification,
  AuditItem,
  ReportItem,
  Punishment,
  DashboardStats,
} from '@/types'

const http = axios.create({
  baseURL: '/api/v1',
  timeout: 10000,
})

// 请求拦截器: 自动携带Token
http.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// 响应拦截器: 统一错误处理
http.interceptors.response.use(
  (res) => res,
  (err: AxiosError<ApiResponse>) => {
    const info = err.response?.data?.message || '网络异常'
    if (err.response?.status === 401) {
      localStorage.removeItem('accessToken')
      window.location.href = '/login'
    }
    message.error(info)
    return Promise.reject(err)
  }
)

// ============================================================
// API 方法
// ============================================================

// -- Auth --
export const authApi = {
  register: (data: RegisterRequest) =>
    http.post<ApiResponse<TokenResponse>>('/auth/register', data),
  login: (data: LoginRequest) =>
    http.post<ApiResponse<TokenResponse>>('/auth/login', data),
  refresh: (refreshToken: string) =>
    http.post<ApiResponse<TokenResponse>>('/auth/refresh', { refreshToken }),
  logout: () => http.post('/auth/logout'),
}

// -- Users --
export const userApi = {
  getMe: () => http.get<ApiResponse<UserDetail>>('/users/me'),
  updateMe: (data: Partial<UserDetail>) =>
    http.put<ApiResponse<UserDetail>>('/users/me', data),
  changePassword: (oldPwd: string, newPwd: string) =>
    http.put('/users/me/password', { oldPassword: oldPwd, newPassword: newPwd }),
  getPreference: () => http.get<ApiResponse<UserPreference>>('/users/me/preference'),
  updatePreference: (data: UserPreference) => http.put('/users/me/preference', data),
  getPrivacy: () => http.get<ApiResponse<PrivacySetting>>('/users/me/privacy'),
  updatePrivacy: (data: PrivacySetting) => http.put('/users/me/privacy', data),
  getAchievement: () => http.get<ApiResponse<UserAchievement>>('/users/me/achievement'),
  getRiskAssessment: () => http.get<ApiResponse<RiskAssessment>>('/users/me/risk-assessment'),
  submitRiskAssessment: (resultLevel: string) =>
    http.post('/users/me/risk-assessment', { resultLevel }),
  getVerification: () => http.get<ApiResponse<VerificationRecord[]>>('/users/me/verification'),
  submitVerification: (verificationType: number) =>
    http.post('/users/me/verification', { verificationType }),
  searchUsers: (keyword: string, page = 1, pageSize = 20) =>
    http.get<ApiResponse<UserBrief[]>>('/users/search', {
      params: { keyword, page, pageSize },
    }),
  getUserById: (userId: number) =>
    http.get<ApiResponse<UserDetail>>(`/users/${userId}`),
}

// -- Posts --
export const postApi = {
  getList: (params: Record<string, unknown>) =>
    http.get<ApiResponse<PaginatedData<Post>>>('/posts', { params }),
  getDetail: (postId: number) =>
    http.get<ApiResponse<Post>>(`/posts/${postId}`),
  create: (data: PostCreate) =>
    http.post<ApiResponse<Post>>('/posts', data),
  update: (postId: number, data: Partial<PostCreate>) =>
    http.put<ApiResponse<Post>>(`/posts/${postId}`, data),
  delete: (postId: number) => http.delete(`/posts/${postId}`),
  toggleLike: (postId: number) => http.post(`/posts/${postId}/like`),
  toggleCollect: (postId: number) => http.post(`/posts/${postId}/collect`),
  getCollections: (page = 1, pageSize = 20) =>
    http.get<ApiResponse<PaginatedData<Post>>>('/posts/collections', { params: { page, pageSize } }),
  togglePin: (postId: number, isPinned: boolean) =>
    http.put(`/posts/${postId}/pin`, { isPinned }),
  toggleEssence: (postId: number, isEssence: boolean) =>
    http.put(`/posts/${postId}/essence`, { isEssence }),
}

// -- Comments --
export const commentApi = {
  getList: (postId: number, page = 1, pageSize = 20, sort = 'latest') =>
    http.get<ApiResponse<PaginatedData<Comment>>>(`/posts/${postId}/comments`, {
      params: { page, pageSize, sort },
    }),
  create: (postId: number, data: { content: string; parentCommentId?: number }) =>
    http.post<ApiResponse<Comment>>(`/posts/${postId}/comments`, data),
  delete: (commentId: number) => http.delete(`/comments/${commentId}`),
  toggleLike: (commentId: number) => http.post(`/comments/${commentId}/like`),
  getReplies: (commentId: number, page = 1, pageSize = 10) =>
    http.get<ApiResponse<PaginatedData<Comment>>>(`/comments/${commentId}/replies`, {
      params: { page, pageSize },
    }),
}

// -- Attachments --
export const attachApi = {
  upload: (file: File) => {
    const fd = new FormData()
    fd.append('file', file)
    return http.post<ApiResponse<Attachment>>('/attachments/upload', fd, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },
}

// -- Votes --
export const voteApi = {
  create: (postId: number, data: VoteCreate) =>
    http.post<ApiResponse<VoteDetail>>(`/posts/${postId}/vote`, data),
  getByPost: (postId: number) =>
    http.get<ApiResponse<VoteDetail>>(`/posts/${postId}/vote`),
  submit: (voteId: number, optionIndex: number) =>
    http.post<ApiResponse<VoteDetail>>(`/votes/${voteId}/submit`, { optionIndex }),
}

// -- Sections --
export const sectionApi = {
  getAll: () => http.get<ApiResponse<Section[]>>('/sections'),
  getZones: (sectionId: number) =>
    http.get<ApiResponse<Zone[]>>(`/sections/${sectionId}/zones`),
}

// -- Dynamics --
export const dynamicApi = {
  getFeed: (feed: string, page = 1, pageSize = 20) =>
    http.get<ApiResponse<PaginatedData<Dynamic>>>('/dynamics', {
      params: { feed, page, pageSize },
    }),
  create: (content: string) =>
    http.post<ApiResponse<Dynamic>>('/dynamics', { content }),
  delete: (dynamicId: number) => http.delete(`/dynamics/${dynamicId}`),
  getByUser: (userId: number, page = 1, pageSize = 20) =>
    http.get<ApiResponse<PaginatedData<Dynamic>>>(`/users/${userId}/dynamics`, {
      params: { page, pageSize },
    }),
}

// -- Social --
export const socialApi = {
  // 关注
  toggleFollow: (userId: number) => http.post(`/users/${userId}/follow`),
  setStar: (userId: number, isStarred: boolean) =>
    http.put(`/users/${userId}/star`, { isStarred }),
  getFollowing: (userId: number | 'me') =>
    http.get<ApiResponse<FollowUser[]>>(`/users/${userId}/following`),
  getFollowers: (userId: number | 'me') =>
    http.get<ApiResponse<FollowUser[]>>(`/users/${userId}/followers`),
  // 私信
  getConversations: (page = 1, pageSize = 20) =>
    http.get<ApiResponse<Conversation[]>>('/messages/conversations', {
      params: { page, pageSize },
    }),
  getDmUnreadCount: () => http.get<ApiResponse<{ total: number }>>('/messages/unread-count'),
  getMessages: (userId: number, page = 1, pageSize = 20) =>
    http.get<ApiResponse<PaginatedData<PrivateMessage>>>(`/messages/with/${userId}`, {
      params: { page, pageSize },
    }),
  sendMessage: (userId: number, content: string) =>
    http.post<ApiResponse<PrivateMessage>>(`/messages/with/${userId}`, { content }),
  markRead: (messageId: number) => http.put(`/messages/${messageId}/read`),
  markAllRead: () => http.put('/messages/read-all'),
  // 群组
  getGroups: () => http.get<ApiResponse<Group[]>>('/groups'),
  createGroup: (data: { groupName: string; mode?: number }) =>
    http.post<ApiResponse<Group>>('/groups', data),
  getGroupDetail: (groupId: number) => http.get<ApiResponse<Group>>(`/groups/${groupId}`),
  updateGroup: (groupId: number, data: Record<string, unknown>) =>
    http.put(`/groups/${groupId}`, data),
  joinGroup: (groupId: number) => http.post(`/groups/${groupId}/join`),
  leaveGroup: (groupId: number) => http.post(`/groups/${groupId}/leave`),
  getMembers: (groupId: number) =>
    http.get<ApiResponse<GroupMember[]>>(`/groups/${groupId}/members`),
  setRole: (groupId: number, userId: number, role: number) =>
    http.put(`/groups/${groupId}/members/${userId}/role`, { role }),
  kickMember: (groupId: number, userId: number) =>
      http.post(`/groups/${groupId}/members/${userId}/kick`),
  inviteMember: (groupId: number, userId: number) =>
      http.post(`/groups/${groupId}/invite/${userId}`),
  getGroupPosts: (groupId: number, page = 1, pageSize = 20) =>
    http.get<ApiResponse<PaginatedData<GroupPost>>>(`/groups/${groupId}/posts`, {
      params: { page, pageSize },
    }),
  createGroupPost: (groupId: number, content: string) =>
    http.post<ApiResponse<GroupPost>>(`/groups/${groupId}/posts`, { content }),
  deleteGroupPost: (groupId: number, groupPostId: number) => http.delete(`/groups/${groupId}/posts/${groupPostId}`),
}

// -- Notifications --
export const notifApi = {
  getList: (page = 1, pageSize = 20, type?: number) =>
    http.get<ApiResponse<PaginatedData<Notification>>>('/notifications', {
      params: { page, pageSize, type },
    }),
  getUnreadCount: () => http.get<ApiResponse<{ total: number }>>('/notifications/unread-count'),
  markRead: (id: number) => http.put(`/notifications/${id}/read`),
  markAllRead: () => http.put('/notifications/read-all'),
}

// -- Reports (普通用户提交) --
export const reportApi = {
  submit: (data: { targetType: number; targetId: number; reason: string }) =>
    http.post<ApiResponse<ReportItem>>('/reports', data),
}

// -- Admin --
export const adminApi = {
  // 审核
  getAuditQueue: (params: Record<string, unknown>) =>
    http.get<ApiResponse<PaginatedData<AuditItem>>>('/admin/audit-queue', { params }),
  audit: (id: number, data: { auditStatus: 1 | 2; auditComment: string }) =>
    http.put(`/admin/audit-queue/${id}`, data),
  // 举报
  getReports: (params: Record<string, unknown>) =>
    http.get<ApiResponse<PaginatedData<ReportItem>>>('/admin/reports', { params }),
  handleReport: (id: number, data: Record<string, unknown>) =>
    http.put(`/admin/reports/${id}/handle`, data),
  // 处罚
  getPunishments: (params: Record<string, unknown>) =>
    http.get<ApiResponse<PaginatedData<Punishment>>>('/admin/punishments', { params }),
  revokePunishment: (id: number) => http.put(`/admin/punishments/${id}/revoke`),
  // 数据
  getDashboard: () => http.get<ApiResponse<DashboardStats>>('/admin/dashboard'),
}

export default http
