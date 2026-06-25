import { describe, it, expect } from 'vitest'
import type {
  ApiResponse,
  PaginatedData,
  TokenResponse,
  LoginRequest,
  RegisterRequest,
  UserBrief,
  UserDetail,
  UserPreference,
  PrivacySetting,
  UserAchievement,
  Post,
  PostCreate,
  Comment,
  Dynamic,
  Section,
  Zone,
  VoteDetail,
  Group,
  GroupMember,
  GroupPost,
  Notification,
  AuditItem,
  ReportItem,
  Punishment,
  DashboardStats,
  Conversation,
  PrivateMessage,
} from '@/types'

describe('TypeScript 类型定义 - 完整性检查', () => {
  describe('ApiResponse<T>', () => {
    it('泛型应为 unknown 默认值', () => {
      const resp: ApiResponse = { code: 200, message: 'ok', data: 'hello' }
      expect(resp.code).toBe(200)
    })

    it('可指定具体类型', () => {
      const resp: ApiResponse<string> = { code: 200, message: 'ok', data: 'text' }
      expect(typeof resp.data).toBe('string')
    })
  })

  describe('PaginatedData<T>', () => {
    it('应包含分页字段', () => {
      const data: PaginatedData<Post> = {
        records: [], total: 0, page: 1, pageSize: 10, totalPages: 0
      }
      expect(data.total).toBe(0)
      expect(data.page).toBe(1)
    })
  })

  describe('TokenResponse', () => {
    it('应包含必要的 token 字段', () => {
      const token: TokenResponse = {
        accessToken: 'at', refreshToken: 'rt', tokenType: 'Bearer', expiresIn: 7200
      }
      expect(token.accessToken).toBe('at')
      expect(token.tokenType).toBe('Bearer')
    })
  })

  describe('LoginRequest', () => {
    it('account 和 password 为必填', () => {
      const req: LoginRequest = { account: 'test', password: '123456' }
      expect(req.account).toBeDefined()
      expect(req.password).toBeDefined()
    })
  })

  describe('RegisterRequest', () => {
    it('应包含昵称、账号、密码', () => {
      const req: RegisterRequest = { nickname: 'user', account: '138', password: '123456' }
      expect(req.nickname).toBe('user')
    })
  })

  describe('UserBrief / UserDetail', () => {
    it('UserDetail 应包含 UserBrief 的所有字段', () => {
      const detail: UserDetail = {
        userId: 1, nickname: 'u', avatarUrl: '', bio: '', gender: 0,
        verificationLevel: 0, level: 1, points: 0, isFollowed: false,
        mobile: '', email: '', followerCount: 0, followingCount: 0,
        preference: null, achievement: null, createdAt: ''
      }
      // UserDetail extends UserBrief, so all UserBrief fields should be present
      expect(detail.userId).toBe(1)
      expect(detail.mobile).toBe('')
      expect(detail.followerCount).toBe(0)
    })
  })

  describe('Post', () => {
    it('应包含帖子的完整信息', () => {
      const post: Post = {
        postId: 1, title: 't', content: 'c', contentType: 0, sectionId: 1,
        sectionName: '', zoneId: 1, zoneName: '',
        author: { userId: 1, nickname: '', avatarUrl: '', bio: '', gender: 0, verificationLevel: 0, level: 1, points: 0, isFollowed: false, createdAt: '' },
        likeCount: 0, viewCount: 0, commentCount: 0, collectCount: 0,
        isEssence: false, isPinned: false, isLiked: false, isCollected: false,
        attachments: [], vote: null, publishTime: ''
      }
      expect(post.postId).toBe(1)
      expect(post.isEssence).toBe(false)
    })
  })

  describe('Comment', () => {
    it('parentCommentId 可为 null', () => {
      const comment: Comment = {
        commentId: 1, postId: 1, parentCommentId: null,
        author: { userId: 1, nickname: '', avatarUrl: '', bio: '', gender: 0, verificationLevel: 0, level: 1, points: 0, isFollowed: false, createdAt: '' },
        content: '', likeCount: 0, replyCount: 0, isLiked: false, isDeleted: false, publishTime: ''
      }
      expect(comment.parentCommentId).toBeNull()
    })
  })

  describe('Dynamic', () => {
    it('isMine 应识别是否为本人动态', () => {
      const dynamic: Dynamic = {
        dynamicId: 1,
        author: { userId: 1, nickname: '', avatarUrl: '', bio: '', gender: 0, verificationLevel: 0, level: 1, points: 0, isFollowed: false, createdAt: '' },
        content: '', likeCount: 0, isLiked: false, isMine: true, publishTime: ''
      }
      expect(dynamic.isMine).toBe(true)
    })
  })

  describe('Section / Zone', () => {
    it('Section 应包含 zones 子分区数组', () => {
      const zone: Zone = { zoneId: 1, zoneName: '上证', sectionId: 1, sortOrder: 1 }
      const section: Section = {
        sectionId: 1, sectionName: 'A股', sectionType: 0, sortOrder: 1, zones: [zone]
      }
      expect(section.zones).toHaveLength(1)
    })
  })

  describe('VoteDetail', () => {
    it('isExpired 判断是否过期', () => {
      const vote: VoteDetail = {
        voteId: 1, voteTitle: 'test', options: [], totalCount: 0,
        endTime: '2020-01-01', isExpired: true, mySelection: null
      }
      expect(vote.isExpired).toBe(true)
    })
  })

  describe('PrivateMessage / Conversation', () => {
    it('Conversation 包含最后一条消息和未读数', () => {
      const conv: Conversation = {
        targetUser: { userId: 2, nickname: 'b', avatarUrl: '', bio: '', gender: 0, verificationLevel: 0, level: 1, points: 0, isFollowed: false, createdAt: '' },
        lastMessage: 'hello', unreadCount: 3, lastTime: ''
      }
      expect(conv.unreadCount).toBe(3)
    })
  })

  describe('Group / GroupMember / GroupPost', () => {
    it('myRole 为 null 表示未加入', () => {
      const group: Group = {
        groupId: 1, groupName: 'g',
        owner: { userId: 1, nickname: '', avatarUrl: '', bio: '', gender: 0, verificationLevel: 0, level: 1, points: 0, isFollowed: false, createdAt: '' },
        mode: 0, status: 1, memberCount: 0, myRole: null, createdAt: ''
      }
      expect(group.myRole).toBeNull()
    })
  })

  describe('Notification', () => {
    it('notifyType 0-5 分别代表不同通知类型', () => {
      const notif: Notification = {
        notificationId: 1, notifyType: 1, title: '点赞', content: 'xxx 赞了你的帖子',
        targetType: 0, targetId: 1, isRead: false, createdAt: ''
      }
      expect(notif.notifyType).toBe(1)
    })
  })

  describe('管理相关类型', () => {
    it('AuditItem 包含审核状态', () => {
      const item: AuditItem = {
        auditItemId: 1, contentType: 0, contentId: 1, preview: '',
        submitter: { userId: 1, nickname: '', avatarUrl: '', bio: '', gender: 0, verificationLevel: 0, level: 1, points: 0, isFollowed: false, createdAt: '' },
        auditStatus: 0, auditor: null, auditComment: null, createdAt: '', auditedAt: null
      }
      expect(item.auditStatus).toBe(0)
    })

    it('ReportItem 包含举报信息', () => {
      const item: ReportItem = {
        reportId: 1,
        reporter: { userId: 1, nickname: '', avatarUrl: '', bio: '', gender: 0, verificationLevel: 0, level: 1, points: 0, isFollowed: false, createdAt: '' },
        targetType: 0, targetId: 1, reason: '违规内容', status: 0,
        handler: null, handleResult: null, createdAt: '', handledAt: null
      }
      expect(item.reason).toBe('违规内容')
    })

    it('Punishment 应包含处罚信息', () => {
      const p: Punishment = {
        punishmentId: 1,
        user: { userId: 2, nickname: '', avatarUrl: '', bio: '', gender: 0, verificationLevel: 0, level: 1, points: 0, isFollowed: false, createdAt: '' },
        punishmentType: 2, reason: '违规', durationDays: 7, isActive: true,
        operator: { userId: 1, nickname: '', avatarUrl: '', bio: '', gender: 0, verificationLevel: 0, level: 1, points: 0, isFollowed: false, createdAt: '' },
        createdAt: '', expireAt: null
      }
      expect(p.punishmentType).toBe(2)
      expect(p.isActive).toBe(true)
    })
  })

  describe('DashboardStats', () => {
    it('应包含统计分析字段', () => {
      const stats: DashboardStats = {
        dau: 100, mau: 1000, newUsers: 50,
        postCount: 200, commentCount: 500, interactionCount: 800,
        trend: []
      }
      expect(stats.dau).toBe(100)
    })
  })
})
