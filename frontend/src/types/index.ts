// ============================================================
// 通用类型
// ============================================================
export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
}

export interface PaginatedData<T> {
  records: T[]
  total: number
  page: number
  pageSize: number
  totalPages: number
}

// ============================================================
// 认证
// ============================================================
export interface LoginRequest {
  account: string
  password: string
}

export interface RegisterRequest {
  nickname: string
  account: string
  password: string
}

export interface TokenResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
}

// ============================================================
// 用户
// ============================================================
export interface UserBrief {
  userId: number
  nickname: string
  avatarUrl: string
  bio: string
  gender: 0 | 1 | 2           // 0未知 1男 2女
  verificationLevel: number
  level: number
  points: number
  isFollowed: boolean
  isStarred?: boolean
  createdAt: string
}

export interface UserDetail extends UserBrief {
  mobile: string
  email: string
  followerCount: number
  followingCount: number
  preference: UserPreference | null
  achievement: UserAchievement | null
}

export interface UserPreference {
  focusMarkets: string
  riskType: string
}

export interface PrivacySetting {
  profileVisibility: 0 | 1 | 2  // 0仅自己 1好友可见 2所有人
}

export interface UserAchievement {
  totalPostCount: number
  essencePostCount: number
}

export interface RiskAssessment {
  resultLevel: string
  completeTime: string
}

export interface VerificationRecord {
  recordId: number
  verificationType: 0 | 1 | 2  // 0身份证 1学生证 2驾驶证
  auditStatus: 0 | 1 | 2       // 0待审核 1通过 2驳回
  createdAt: string
}

export interface FollowUser {
  userId: number
  nickname: string
  avatarUrl: string
  bio: string
  isStarred: boolean
  isMutual: boolean
  followedAt: string
}

// ============================================================
// 内容
// ============================================================
export interface Post {
  postId: number
  title: string
  content: string
  contentType: 0 | 1 | 2       // 0普通 1投票 2图文
  sectionId: number
  sectionName: string
  zoneId: number
  zoneName: string
  author: UserBrief
  likeCount: number
  viewCount: number
  commentCount: number
  collectCount: number
  isEssence: boolean
  isPinned: boolean
  isLiked: boolean
  isCollected: boolean
  attachments: Attachment[]
  vote: VoteDetail | null
  publishTime: string
}

export interface PostCreate {
  title: string
  content: string
  contentType: number
  sectionId: number
  zoneId?: number
  attachmentIds?: number[]
}

export interface Comment {
  commentId: number
  postId: number
  parentCommentId: number | null
  author: UserBrief
  content: string
  likeCount: number
  replyCount: number
  isLiked: boolean
  isDeleted: boolean
  publishTime: string
}

export interface Attachment {
  attachmentId: number
  fileName: string
  fileType: 0 | 1 | 2 | 3     // 0图片 1文档 2视频 3音频
  fileUrl: string
  fileSize: number
}

export interface Dynamic {
  dynamicId: number
  author: UserBrief
  content: string
  likeCount: number
  isLiked: boolean
  isMine: boolean
  publishTime: string
}

// ============================================================
// 投票
// ============================================================
export interface VoteOption {
  index: number
  text: string
  count: number
  percentage: number
  isSelected: boolean
}

export interface VoteDetail {
  voteId: number
  voteTitle: string
  options: VoteOption[]
  totalCount: number
  endTime: string
  isExpired: boolean
  mySelection: number | null
}

export interface VoteCreate {
  voteTitle: string
  options: string[]
  endTime: string
}

// ============================================================
// 私信
// ============================================================
export interface PrivateMessage {
  messageId: number
  senderId: number
  sender: UserBrief
  content: string
  isRead: boolean
  isMine: boolean
  sendTime: string
}

export interface Conversation {
  targetUser: UserBrief
  lastMessage: string
  unreadCount: number
  lastTime: string
}

// ============================================================
// 群组
// ============================================================
export interface Group {
  groupId: number
  groupName: string
  owner: UserBrief
  mode: 0 | 1 | 2              // 0自由 1审核 2禁止
  status: 0 | 1 | 2            // 0解散 1正常 2禁言
  memberCount: number
  myRole: 0 | 1 | 2 | null     // 0普通 1管理 2群主 null未加入
  createdAt: string
}

export interface GroupMember {
  userId: number
  nickname: string
  avatarUrl: string
  role: 0 | 1 | 2
  joinedAt: string
}

export interface GroupPost {
  groupPostId: number
  groupId: number
  author: UserBrief
  content: string
  likeCount: number
  isLiked: boolean
  isMine: boolean
  publishTime: string
}

// ============================================================
// 板块
// ============================================================
export interface Section {
  sectionId: number
  sectionName: string
  sectionType: 0 | 1 | 2       // 0讨论 1资讯 2问答
  sortOrder: number
  zones: Zone[]
}

export interface Zone {
  zoneId: number
  zoneName: string
  sectionId: number
  sortOrder: number
}

// ============================================================
// 通知
// ============================================================
export interface Notification {
  notificationId: number
  notifyType: 0 | 1 | 2 | 3 | 4 | 5  // 0系统 1点赞 2评论 3关注 4私信 5举报结果
  title: string
  content: string
  targetType: number | null
  targetId: number | null
  isRead: boolean
  createdAt: string
}

// ============================================================
// 管理
// ============================================================
export interface AuditItem {
  auditItemId: number
  contentType: 0 | 1 | 2       // 0帖子 1评论 2附件
  contentId: number
  preview: string
  submitter: UserBrief
  auditStatus: 0 | 1 | 2       // 0待审 1通过 2驳回
  auditor: UserBrief | null
  auditComment: string | null
  createdAt: string
  auditedAt: string | null
}

export interface ReportItem {
  reportId: number
  reporter: UserBrief
  targetType: 0 | 1 | 2 | 3    // 0帖子 1评论 2用户 3私信
  targetId: number
  reason: string
  status: 0 | 1 | 2            // 0待处理 1已处理 2驳回
  handler: UserBrief | null
  handleResult: number | null
  createdAt: string
  handledAt: string | null
}

export interface Punishment {
  punishmentId: number
  user: UserBrief
  punishmentType: 0 | 1 | 2    // 0警告 1禁言 2封号
  reason: string
  operator: UserBrief
  durationDays: number
  isActive: boolean
  createdAt: string
  expireAt: string | null
}

// ============================================================
// 数据分析
// ============================================================
export interface DashboardStats {
  dau: number
  mau: number
  newUsers: number
  postCount: number
  commentCount: number
  interactionCount: number
  trend: { date: string; value: number }[]
}
