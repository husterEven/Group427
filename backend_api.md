openapi: 3.0.3
info:
  title: 投资论坛系统 API
  description: |
    投资论坛系统后端RESTful API接口文档
    - 认证方式：Bearer Token (JWT)
    - 请求格式：application/json
    - 响应格式：application/json
    
    ## 接口设计规范
    - 使用名词复数形式表示资源
    - HTTP方法：GET(查询)、POST(创建)、PUT(全量更新)、PATCH(部分更新)、DELETE(删除)
    - 状态码：200(成功)、201(创建成功)、400(参数错误)、401(未认证)、403(无权限)、404(资源不存在)、500(服务器错误)
  version: 1.0.0
  contact:
    name: API Support
    email: support@forum.com
  license:
    name: Proprietary

servers:
  - url: https://api.forum.com/v1
    description: 生产环境
  - url: https://staging-api.forum.com/v1
    description: 预发布环境
  - url: http://localhost:8080/v1
    description: 本地开发环境

tags:
  - name: 认证
    description: 用户注册、登录、认证相关接口
  - name: 用户
    description: 用户资料、偏好设置、成就查询
  - name: 认证中心
    description: 实名认证、专业认证、风险测评
  - name: 内容
    description: 帖子、评论、动态的增删改查
  - name: 板块
    description: 板块与专区信息查询
  - name: 社交
    description: 关注、私信、群组功能
  - name: 搜索
    description: 全文搜索与筛选
  - name: 互动
    description: 点赞、收藏、转发、举报
  - name: 通知
    description: 消息通知相关
  - name: 管理
    description: 管理员专用接口（需admin权限）

# ==================== 通用组件定义 ====================
components:
  securitySchemes:
    BearerAuth:
      type: http
      scheme: bearer
      bearerFormat: JWT
      description: 使用JWT Token进行认证，在请求头中添加 `Authorization: Bearer {token}`

  schemas:
    # ==================== 通用响应结构 ====================
    ApiResponse:
      type: object
      properties:
        code:
          type: integer
          description: 业务状态码，0表示成功
          example: 0
        message:
          type: string
          description: 响应消息
          example: success
        data:
          type: object
          description: 响应数据
          nullable: true
        timestamp:
          type: integer
          format: int64
          description: 时间戳
          example: 1702800000000

    PageResponse:
      type: object
      properties:
        code:
          type: integer
          example: 0
        message:
          type: string
          example: success
        data:
          type: object
          properties:
            list:
              type: array
              items:
                type: object
            total:
              type: integer
              description: 总记录数
            pageNum:
              type: integer
              description: 当前页码
            pageSize:
              type: integer
              description: 每页大小
            pages:
              type: integer
              description: 总页数

    # ==================== 用户相关 Schema ====================
    User:
      type: object
      properties:
        userId:
          type: integer
          format: int64
          description: 用户ID
          example: 10001
        nickname:
          type: string
          description: 昵称
          maxLength: 30
          example: "价值投资客"
        avatarUrl:
          type: string
          description: 头像URL
          example: "https://cdn.forum.com/avatar/10001.jpg"
        bio:
          type: string
          description: 个人简介
          maxLength: 200
          example: "专注价值投资10年"
        verificationLevel:
          type: integer
          description: 认证等级 0-未认证 1-基础 2-实名 3-专业V
          enum: [0, 1, 2, 3]
        riskLevel:
          type: integer
          description: 风险等级 1-保守 2-稳健 3-平衡 4-进取
          enum: [1, 2, 3, 4]
        points:
          type: integer
          description: 积分
        level:
          type: integer
          description: 用户等级 1-100
        postCount:
          type: integer
          description: 发帖数
        followerCount:
          type: integer
          description: 粉丝数
        followeeCount:
          type: integer
          description: 关注数
        registerAt:
          type: string
          format: date-time
          description: 注册时间

    UserProfileUpdate:
      type: object
      properties:
        nickname:
          type: string
          maxLength: 30
        avatarUrl:
          type: string
          maxLength: 512
        bio:
          type: string
          maxLength: 200
        tags:
          type: array
          items:
            type: string
          description: 投资经验标签

    UserPreference:
      type: object
      properties:
        focusMarkets:
          type: array
          items:
            type: string
            enum: [A股, 港股, 美股, 基金, 期货]
          description: 关注市场
        riskPreference:
          type: integer
          enum: [1, 2, 3, 4]
          description: 风险偏好
        investmentExp:
          type: integer
          enum: [1, 2, 3, 4]
          description: 投资经验 1-新手 2-进阶 3-资深 4-职业
        receiveNotification:
          type: boolean
          description: 是否接收推送通知

    PrivacySettings:
      type: object
      properties:
        profileVisible:
          type: integer
          enum: [0, 1, 2]
          description: 0-公开 1-仅粉丝 2-仅自己
        postVisible:
          type: integer
          enum: [0, 1, 2]
        followVisible:
          type: integer
          enum: [0, 1, 2]
        allowSearch:
          type: boolean
        allowMessage:
          type: boolean

    UserAchievement:
      type: object
      properties:
        totalPosts:
          type: integer
          description: 累计发帖
        essencePosts:
          type: integer
          description: 精华帖数
        totalComments:
          type: integer
          description: 累计评论
        totalLikesReceived:
          type: integer
          description: 获赞总数
        influenceScore:
          type: integer
          description: 影响力分数
        badges:
          type: array
          items:
            type: object
            properties:
              badgeId:
                type: integer
              name:
                type: string
              iconUrl:
                type: string
              obtainTime:
                type: string
                format: date-time

    # ==================== 认证相关 Schema ====================
    LoginRequest:
      type: object
      required:
        - account
        - password
      properties:
        account:
          type: string
          description: 手机号/邮箱/昵称
          example: "13800000000"
        password:
          type: string
          description: 密码
          format: password
          example: "password123"
        captcha:
          type: string
          description: 验证码（可选）
          example: "A3B9"

    LoginResponse:
      type: object
      properties:
        token:
          type: string
          description: JWT Token
        expireAt:
          type: integer
          format: int64
          description: 过期时间戳
        user:
          $ref: '#/components/schemas/User'

    RegisterRequest:
      type: object
      required:
        - registerType
      properties:
        registerType:
          type: string
          enum: [mobile, email]
        mobile:
          type: string
          pattern: '^1[3-9]\d{9}$'
        email:
          type: string
          format: email
        verificationCode:
          type: string
        password:
          type: string
          minLength: 6
          maxLength: 20
        agreement:
          type: boolean
          description: 是否同意用户协议

    SendVerificationCodeRequest:
      type: object
      required:
        - contact
        - type
      properties:
        contact:
          type: string
          description: 手机号或邮箱
        type:
          type: string
          enum: [register, login, bind, reset]
          description: 验证码类型

    ThirdPartyLoginRequest:
      type: object
      required:
        - platform
        - authCode
      properties:
        platform:
          type: string
          enum: [wechat, weibo]
        authCode:
          type: string
          description: 第三方授权码

    # ==================== 认证中心 Schema ====================
    RealNameVerifyRequest:
      type: object
      required:
        - realName
        - idCardNo
        - idCardFrontUrl
        - idCardBackUrl
        - faceImageUrl
      properties:
        realName:
          type: string
        idCardNo:
          type: string
          pattern: '^[1-9]\d{5}(18|19|20)\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\d|3[01])\d{3}[\dXx]$'
        idCardFrontUrl:
          type: string
        idCardBackUrl:
          type: string
        faceImageUrl:
          type: string

    ProfessionVerifyRequest:
      type: object
      properties:
        certificateUrls:
          type: array
          items:
            type: string
        educationUrls:
          type: array
          items:
            type: string
        professionType:
          type: string
          description: 从业类型

    RiskAssessmentSubmitRequest:
      type: object
      required:
        - answers
      properties:
        answers:
          type: array
          items:
            type: object
            properties:
              questionId:
                type: string
              selectedOption:
                type: integer

    RiskAssessmentResult:
      type: object
      properties:
        level:
          type: integer
          description: 风险等级
        levelName:
          type: string
        score:
          type: integer
        description:
          type: string

    # ==================== 帖子相关 Schema ====================
    Post:
      type: object
      properties:
        postId:
          type: integer
          format: int64
        author:
          $ref: '#/components/schemas/UserSimple'
        title:
          type: string
        summary:
          type: string
        contentType:
          type: integer
          description: 1-普通 2-长文 3-投票 4-动态
        sectionId:
          type: integer
        zoneId:
          type: integer
        stockCode:
          type: string
        isPinned:
          type: boolean
        isEssence:
          type: boolean
        stats:
          type: object
          properties:
            likes:
              type: integer
            favorites:
              type: integer
            shares:
              type: integer
            comments:
              type: integer
            views:
              type: integer
        publishedAt:
          type: string
          format: date-time

    PostDetail:
      allOf:
        - $ref: '#/components/schemas/Post'
        - type: object
          properties:
            content:
              type: string
            richContent:
              type: string
            attachments:
              type: array
              items:
                $ref: '#/components/schemas/Attachment'
            voteInfo:
              $ref: '#/components/schemas/VoteInfo'

    PostCreateRequest:
      type: object
      required:
        - title
        - content
        - sectionId
      properties:
        title:
          type: string
          maxLength: 200
        content:
          type: string
        contentType:
          type: integer
          default: 1
        sectionId:
          type: integer
        zoneId:
          type: integer
        stockCode:
          type: string
        attachments:
          type: array
          items:
            type: string
          description: 附件ID列表

    LongArticleRequest:
      allOf:
        - $ref: '#/components/schemas/PostCreateRequest'
        - type: object
          properties:
            richContent:
              type: string
              description: HTML格式富文本
            coverImage:
              type: string

    VoteCreateRequest:
      type: object
      required:
        - title
        - options
      properties:
        title:
          type: string
        options:
          type: array
          minItems: 2
          maxItems: 10
          items:
            type: string
        maxChoices:
          type: integer
          minimum: 1
          maximum: 5
          default: 1
        durationHours:
          type: integer
          default: 24
        isAnonymous:
          type: boolean
          default: false

    VoteInfo:
      type: object
      properties:
        voteId:
          type: integer
        title:
          type: string
        options:
          type: array
          items:
            type: object
            properties:
              index:
                type: integer
              text:
                type: string
              count:
                type: integer
              percentage:
                type: number
        maxChoices:
          type: integer
        totalVotes:
          type: integer
        isEnded:
          type: boolean
        userSelected:
          type: array
          items:
            type: integer
          description: 当前用户选择的选项（已登录时）

    VoteCastRequest:
      type: object
      required:
        - selectedOptions
      properties:
        selectedOptions:
          type: array
          items:
            type: integer
          minItems: 1

    # ==================== 评论相关 Schema ====================
    Comment:
      type: object
      properties:
        commentId:
          type: integer
          format: int64
        postId:
          type: integer
        author:
          $ref: '#/components/schemas/UserSimple'
        content:
          type: string
        parentId:
          type: integer
        rootId:
          type: integer
        stats:
          type: object
          properties:
            likes:
              type: integer
            replies:
              type: integer
        publishedAt:
          type: string
          format: date-time
        atUsers:
          type: array
          items:
            $ref: '#/components/schemas/UserSimple'

    CommentCreateRequest:
      type: object
      required:
        - content
      properties:
        content:
          type: string
          maxLength: 2000
        parentId:
          type: integer
          format: int64
          description: 回复的评论ID
        atUserIds:
          type: array
          items:
            type: integer

    # ==================== 板块相关 Schema ====================
    Section:
      type: object
      properties:
        sectionId:
          type: integer
        name:
          type: string
        slug:
          type: string
        icon:
          type: string
        description:
          type: string
        postCount:
          type: integer
        children:
          type: array
          items:
            $ref: '#/components/schemas/Zone'

    Zone:
      type: object
      properties:
        zoneId:
          type: integer
        sectionId:
          type: integer
        name:
          type: string
        slug:
          type: string
        icon:
          type: string
        description:
          type: string
        postCount:
          type: integer

    # ==================== 社交相关 Schema ====================
    UserSimple:
      type: object
      properties:
        userId:
          type: integer
        nickname:
          type: string
        avatarUrl:
          type: string
        verificationLevel:
          type: integer

    FollowInfo:
      type: object
      properties:
        followerCount:
          type: integer
        followeeCount:
          type: integer
        isFollowing:
          type: boolean
        isFollowed:
          type: boolean
        isStarred:
          type: boolean

    PrivateMessage:
      type: object
      properties:
        messageId:
          type: integer
        sessionId:
          type: string
        sender:
          $ref: '#/components/schemas/UserSimple'
        receiver:
          $ref: '#/components/schemas/UserSimple'
        messageType:
          type: integer
        content:
          type: string
        isRead:
          type: boolean
        isRecalled:
          type: boolean
        sentAt:
          type: string
          format: date-time

    PrivateMessageSendRequest:
      type: object
      required:
        - receiverId
        - content
      properties:
        receiverId:
          type: integer
        messageType:
          type: integer
          default: 1
        content:
          type: string
          maxLength: 2000

    Group:
      type: object
      properties:
        groupId:
          type: integer
        name:
          type: string
        avatar:
          type: string
        introduction:
          type: string
        tags:
          type: array
          items:
            type: string
        mode:
          type: integer
        memberCount:
          type: integer
        postCount:
          type: integer
        owner:
          $ref: '#/components/schemas/UserSimple'
        createdAt:
          type: string
          format: date-time

    GroupCreateRequest:
      type: object
      required:
        - name
      properties:
        name:
          type: string
          maxLength: 50
        introduction:
          type: string
          maxLength: 200
        tags:
          type: array
          items:
            type: string
        mode:
          type: integer
          default: 1
        avatar:
          type: string

    # ==================== 互动相关 Schema ====================
    InteractionStats:
      type: object
      properties:
        liked:
          type: boolean
        favorited:
          type: boolean
        shared:
          type: boolean
        likeCount:
          type: integer
        favoriteCount:
          type: integer
        shareCount:
          type: integer

    ReportRequest:
      type: object
      required:
        - targetType
        - targetId
        - reasonType
      properties:
        targetType:
          type: integer
          enum: [1, 2, 3]
          description: 1-帖子 2-评论 3-用户
        targetId:
          type: integer
        reasonType:
          type: integer
          enum: [1, 2, 3, 4, 5]
        reasonDesc:
          type: string
          maxLength: 200

    # ==================== 搜索相关 Schema ====================
    SearchRequest:
      type: object
      properties:
        keyword:
          type: string
          description: 搜索关键词
        type:
          type: string
          enum: [all, post, user, stock]
          default: all
        sortBy:
          type: string
          enum: [relevance, time, likes, comments]
          default: relevance
        timeRange:
          type: string
          enum: [all, today, week, month, year]
          default: all
        sectionId:
          type: integer
          description: 板块筛选
        isEssence:
          type: boolean
        pageNum:
          type: integer
          minimum: 1
          default: 1
        pageSize:
          type: integer
          minimum: 1
          maximum: 50
          default: 20

    SearchResult:
      type: object
      properties:
        keyword:
          type: string
        total:
          type: integer
        posts:
          type: array
          items:
            $ref: '#/components/schemas/Post'
        users:
          type: array
          items:
            $ref: '#/components/schemas/UserSimple'
        stocks:
          type: array
          items:
            type: object
            properties:
              code:
                type: string
              name:
                type: string
              postCount:
                type: integer

    # ==================== 通知相关 Schema ====================
    Notification:
      type: object
      properties:
        notificationId:
          type: integer
        type:
          type: string
          enum: [like, comment, follow, system, message]
        title:
          type: string
        content:
          type: string
        isRead:
          type: boolean
        targetId:
          type: integer
          description: 关联的目标ID（帖子ID等）
        createdAt:
          type: string
          format: date-time

    # ==================== 通用 Schema ====================
    Attachment:
      type: object
      properties:
        attachmentId:
          type: integer
        fileName:
          type: string
        fileSize:
          type: integer
        fileUrl:
          type: string

    HotTopic:
      type: object
      properties:
        rank:
          type: integer
        name:
          type: string
        heatValue:
          type: integer
        postCount:
          type: integer
        type:
          type: string
          enum: [topic, stock]

# ==================== API 路径定义 ====================
paths:
  # ==================== 认证模块 ====================
  /auth/login:
    post:
      tags: [认证]
      summary: 用户登录
      operationId: login
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/LoginRequest'
      responses:
        '200':
          description: 登录成功
          content:
            application/json:
              schema:
                allOf:
                  - $ref: '#/components/schemas/ApiResponse'
                  - type: object
                    properties:
                      data:
                        $ref: '#/components/schemas/LoginResponse'
        '401':
          description: 用户名或密码错误

  /auth/logout:
    post:
      tags: [认证]
      summary: 用户登出
      security:
        - BearerAuth: []
      responses:
        '200':
          description: 登出成功

  /auth/register:
    post:
      tags: [认证]
      summary: 用户注册
      operationId: register
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/RegisterRequest'
      responses:
        '201':
          description: 注册成功
          content:
            application/json:
              schema:
                allOf:
                  - $ref: '#/components/schemas/ApiResponse'
                  - type: object
                    properties:
                      data:
                        $ref: '#/components/schemas/LoginResponse'

  /auth/captcha/send:
    post:
      tags: [认证]
      summary: 发送验证码
      operationId: sendCaptcha
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/SendVerificationCodeRequest'
      responses:
        '200':
          description: 发送成功

  /auth/third-party/login:
    post:
      tags: [认证]
      summary: 第三方登录
      operationId: thirdPartyLogin
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ThirdPartyLoginRequest'
      responses:
        '200':
          description: 登录成功

  /auth/refresh:
    post:
      tags: [认证]
      summary: 刷新Token
      security:
        - BearerAuth: []
      responses:
        '200':
          description: 刷新成功

  # ==================== 用户模块 ====================
  /users/me:
    get:
      tags: [用户]
      summary: 获取当前用户信息
      security:
        - BearerAuth: []
      responses:
        '200':
          description: 成功
          content:
            application/json:
              schema:
                allOf:
                  - $ref: '#/components/schemas/ApiResponse'
                  - type: object
                    properties:
                      data:
                        $ref: '#/components/schemas/User'

    put:
      tags: [用户]
      summary: 更新当前用户资料
      security:
        - BearerAuth: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/UserProfileUpdate'
      responses:
        '200':
          description: 更新成功

  /users/{userId}:
    get:
      tags: [用户]
      summary: 获取指定用户信息
      parameters:
        - name: userId
          in: path
          required: true
          schema:
            type: integer
            format: int64
      responses:
        '200':
          description: 成功
          content:
            application/json:
              schema:
                allOf:
                  - $ref: '#/components/schemas/ApiResponse'
                  - type: object
                    properties:
                      data:
                        $ref: '#/components/schemas/User'

  /users/me/preference:
    get:
      tags: [用户]
      summary: 获取用户偏好设置
      security:
        - BearerAuth: []
      responses:
        '200':
          description: 成功
    put:
      tags: [用户]
      summary: 更新用户偏好设置
      security:
        - BearerAuth: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/UserPreference'
      responses:
        '200':
          description: 更新成功

  /users/me/privacy:
    get:
      tags: [用户]
      summary: 获取隐私设置
      security:
        - BearerAuth: []
      responses:
        '200':
          description: 成功
    put:
      tags: [用户]
      summary: 更新隐私设置
      security:
        - BearerAuth: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/PrivacySettings'
      responses:
        '200':
          description: 更新成功

  /users/me/achievement:
    get:
      tags: [用户]
      summary: 获取用户成就
      security:
        - BearerAuth: []
      responses:
        '200':
          description: 成功
          content:
            application/json:
              schema:
                allOf:
                  - $ref: '#/components/schemas/ApiResponse'
                  - type: object
                    properties:
                      data:
                        $ref: '#/components/schemas/UserAchievement'

  # ==================== 认证中心模块 ====================
  /verify/real-name:
    post:
      tags: [认证中心]
      summary: 提交实名认证
      security:
        - BearerAuth: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/RealNameVerifyRequest'
      responses:
        '200':
          description: 提交成功

  /verify/profession:
    post:
      tags: [认证中心]
      summary: 提交专业认证
      security:
        - BearerAuth: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ProfessionVerifyRequest'
      responses:
        '200':
          description: 提交成功

  /verify/status:
    get:
      tags: [认证中心]
      summary: 获取认证状态
      security:
        - BearerAuth: []
      responses:
        '200':
          description: 成功

  /risk-assessment:
    post:
      tags: [认证中心]
      summary: 提交风险测评
      security:
        - BearerAuth: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/RiskAssessmentSubmitRequest'
      responses:
        '200':
          description: 提交成功
          content:
            application/json:
              schema:
                allOf:
                  - $ref: '#/components/schemas/ApiResponse'
                  - type: object
                    properties:
                      data:
                        $ref: '#/components/schemas/RiskAssessmentResult'

    get:
      tags: [认证中心]
      summary: 获取当前风险测评结果
      security:
        - BearerAuth: []
      responses:
        '200':
          description: 成功

  # ==================== 内容模块 - 帖子 ====================
  /posts:
    get:
      tags: [内容]
      summary: 获取帖子列表
      parameters:
        - name: sectionId
          in: query
          schema:
            type: integer
        - name: zoneId
          in: query
          schema:
            type: integer
        - name: sortBy
          in: query
          schema:
            type: string
            enum: [latest, hottest, essence]
            default: latest
        - name: pageNum
          in: query
          schema:
            type: integer
            default: 1
        - name: pageSize
          in: query
          schema:
            type: integer
            default: 20
      responses:
        '200':
          description: 成功
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/PageResponse'

    post:
      tags: [内容]
      summary: 发布普通帖子
      security:
        - BearerAuth: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/PostCreateRequest'
      responses:
        '201':
          description: 发布成功

  /posts/long-article:
    post:
      tags: [内容]
      summary: 发布长文分析
      security:
        - BearerAuth: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/LongArticleRequest'
      responses:
        '201':
          description: 发布成功

  /posts/vote:
    post:
      tags: [内容]
      summary: 发布投票帖
      security:
        - BearerAuth: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              allOf:
                - $ref: '#/components/schemas/PostCreateRequest'
                - type: object
                  properties:
                    vote:
                      $ref: '#/components/schemas/VoteCreateRequest'
      responses:
        '201':
          description: 发布成功

  /posts/{postId}:
    get:
      tags: [内容]
      summary: 获取帖子详情
      parameters:
        - name: postId
          in: path
          required: true
          schema:
            type: integer
            format: int64
      responses:
        '200':
          description: 成功
          content:
            application/json:
              schema:
                allOf:
                  - $ref: '#/components/schemas/ApiResponse'
                  - type: object
                    properties:
                      data:
                        $ref: '#/components/schemas/PostDetail'

    put:
      tags: [内容]
      summary: 编辑帖子
      security:
        - BearerAuth: []
      parameters:
        - name: postId
          in: path
          required: true
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/PostCreateRequest'
      responses:
        '200':
          description: 更新成功

    delete:
      tags: [内容]
      summary: 删除帖子
      security:
        - BearerAuth: []
      parameters:
        - name: postId
          in: path
          required: true
      responses:
        '204':
          description: 删除成功

  /posts/{postId}/vote/cast:
    post:
      tags: [内容]
      summary: 参与投票
      security:
        - BearerAuth: []
      parameters:
        - name: postId
          in: path
          required: true
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/VoteCastRequest'
      responses:
        '200':
          description: 投票成功

  /posts/{postId}/vote/result:
    get:
      tags: [内容]
      summary: 获取投票结果
      parameters:
        - name: postId
          in: path
          required: true
      responses:
        '200':
          description: 成功

  /posts/{postId}/interaction:
    get:
      tags: [互动]
      summary: 获取帖子互动统计
      security:
        - BearerAuth: []
      parameters:
        - name: postId
          in: path
          required: true
      responses:
        '200':
          description: 成功
          content:
            application/json:
              schema:
                allOf:
                  - $ref: '#/components/schemas/ApiResponse'
                  - type: object
                    properties:
                      data:
                        $ref: '#/components/schemas/InteractionStats'

  /posts/{postId}/like:
    post:
      tags: [互动]
      summary: 点赞帖子
      security:
        - BearerAuth: []
      parameters:
        - name: postId
          in: path
          required: true
      responses:
        '200':
          description: 成功

    delete:
      tags: [互动]
      summary: 取消点赞
      security:
        - BearerAuth: []
      parameters:
        - name: postId
          in: path
          required: true
      responses:
        '204':
          description: 取消成功

  /posts/{postId}/favorite:
    post:
      tags: [互动]
      summary: 收藏帖子
      security:
        - BearerAuth: []
      parameters:
        - name: postId
          in: path
          required: true
      responses:
        '200':
          description: 成功

    delete:
      tags: [互动]
      summary: 取消收藏
      security:
        - BearerAuth: []
      parameters:
        - name: postId
          in: path
          required: true
      responses:
        '204':
          description: 取消成功

  /posts/{postId}/share:
    post:
      tags: [互动]
      summary: 转发帖子
      security:
        - BearerAuth: []
      parameters:
        - name: postId
          in: path
          required: true
      responses:
        '200':
          description: 成功

  # ==================== 内容模块 - 评论 ====================
  /posts/{postId}/comments:
    get:
      tags: [内容]
      summary: 获取帖子评论列表
      parameters:
        - name: postId
          in: path
          required: true
        - name: pageNum
          in: query
          schema:
            type: integer
            default: 1
        - name: pageSize
          in: query
          schema:
            type: integer
            default: 20
        - name: sortBy
          in: query
          schema:
            type: string
            enum: [latest, hottest]
            default: latest
      responses:
        '200':
          description: 成功

    post:
      tags: [内容]
      summary: 发表评论
      security:
        - BearerAuth: []
      parameters:
        - name: postId
          in: path
          required: true
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CommentCreateRequest'
      responses:
        '201':
          description: 发表成功

  /comments/{commentId}:
    delete:
      tags: [内容]
      summary: 删除评论
      security:
        - BearerAuth: []
      parameters:
        - name: commentId
          in: path
          required: true
      responses:
        '204':
          description: 删除成功

  /comments/{commentId}/like:
    post:
      tags: [互动]
      summary: 点赞评论
      security:
        - BearerAuth: []
      parameters:
        - name: commentId
          in: path
          required: true
      responses:
        '200':
          description: 成功

    delete:
      tags: [互动]
      summary: 取消点赞评论
      security:
        - BearerAuth: []
      parameters:
        - name: commentId
          in: path
          required: true
      responses:
        '204':
          description: 取消成功

  # ==================== 板块模块 ====================
  /sections:
    get:
      tags: [板块]
      summary: 获取所有板块
      responses:
        '200':
          description: 成功
          content:
            application/json:
              schema:
                allOf:
                  - $ref: '#/components/schemas/ApiResponse'
                  - type: object
                    properties:
                      data:
                        type: array
                        items:
                          $ref: '#/components/schemas/Section'

  /sections/{sectionId}/zones:
    get:
      tags: [板块]
      summary: 获取板块下的专区
      parameters:
        - name: sectionId
          in: path
          required: true
      responses:
        '200':
          description: 成功

  # ==================== 盘中动态 ====================
  /dynamics:
    get:
      tags: [内容]
      summary: 获取实时动态列表（时间线）
      parameters:
        - name: pageNum
          in: query
          schema:
            type: integer
            default: 1
        - name: pageSize
          in: query
          schema:
            type: integer
            default: 20
      responses:
        '200':
          description: 成功

    post:
      tags: [内容]
      summary: 发布盘中动态
      security:
        - BearerAuth: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              properties:
                content:
                  type: string
                  maxLength: 500
                imageUrls:
                  type: array
                  items:
                    type: string
                stockCode:
                  type: string
      responses:
        '201':
          description: 发布成功

  # ==================== 社交模块 - 关注 ====================
  /users/{userId}/follow:
    post:
      tags: [社交]
      summary: 关注用户
      security:
        - BearerAuth: []
      parameters:
        - name: userId
          in: path
          required: true
      responses:
        '200':
          description: 关注成功

    delete:
      tags: [社交]
      summary: 取消关注
      security:
        - BearerAuth: []
      parameters:
        - name: userId
          in: path
          required: true
      responses:
        '204':
          description: 取消成功

  /users/{userId}/follow/star:
    post:
      tags: [社交]
      summary: 设为星标关注
      security:
        - BearerAuth: []
      parameters:
        - name: userId
          in: path
          required: true
      responses:
        '200':
          description: 成功

    delete:
      tags: [社交]
      summary: 取消星标
      security:
        - BearerAuth: []
      parameters:
        - name: userId
          in: path
          required: true
      responses:
        '204':
          description: 成功

  /users/me/followers:
    get:
      tags: [社交]
      summary: 获取我的粉丝列表
      security:
        - BearerAuth: []
      parameters:
        - name: pageNum
          in: query
          schema:
            type: integer
            default: 1
        - name: pageSize
          in: query
          schema:
            type: integer
            default: 20
      responses:
        '200':
          description: 成功

  /users/me/followees:
    get:
      tags: [社交]
      summary: 获取我关注的人列表
      security:
        - BearerAuth: []
      parameters:
        - name: pageNum
          in: query
          schema:
            type: integer
            default: 1
        - name: pageSize
          in: query
          schema:
            type: integer
            default: 20
        - name: starredOnly
          in: query
          schema:
            type: boolean
            default: false
      responses:
        '200':
          description: 成功

  /users/{userId}/follow/status:
    get:
      tags: [社交]
      summary: 获取关注状态
      parameters:
        - name: userId
          in: path
          required: true
      security:
        - BearerAuth: []
      responses:
        '200':
          description: 成功
          content:
            application/json:
              schema:
                allOf:
                  - $ref: '#/components/schemas/ApiResponse'
                  - type: object
                    properties:
                      data:
                        $ref: '#/components/schemas/FollowInfo'

  # ==================== 社交模块 - 私信 ====================
  /messages:
    get:
      tags: [社交]
      summary: 获取会话列表
      security:
        - BearerAuth: []
      parameters:
        - name: pageNum
          in: query
          schema:
            type: integer
            default: 1
        - name: pageSize
          in: query
          schema:
            type: integer
            default: 20
      responses:
        '200':
          description: 成功

  /messages/session/{sessionId}:
    get:
      tags: [社交]
      summary: 获取与某用户的聊天记录
      security:
        - BearerAuth: []
      parameters:
        - name: sessionId
          in: path
          required: true
        - name: pageNum
          in: query
          schema:
            type: integer
            default: 1
        - name: pageSize
          in: query
          schema:
            type: integer
            default: 50
      responses:
        '200':
          description: 成功

    post:
      tags: [社交]
      summary: 发送私信
      security:
        - BearerAuth: []
      parameters:
        - name: sessionId
          in: path
          required: true
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/PrivateMessageSendRequest'
      responses:
        '201':
          description: 发送成功

  /messages/{messageId}/recall:
    post:
      tags: [社交]
      summary: 撤回私信
      security:
        - BearerAuth: []
      parameters:
        - name: messageId
          in: path
          required: true
      responses:
        '200':
          description: 撤回成功

  /messages/unread/count:
    get:
      tags: [社交]
      summary: 获取未读私信数量
      security:
        - BearerAuth: []
      responses:
        '200':
          description: 成功

  # ==================== 社交模块 - 群组 ====================
  /groups:
    get:
      tags: [社交]
      summary: 获取群组列表
      parameters:
        - name: keyword
          in: query
          schema:
            type: string
        - name: pageNum
          in: query
          default: 1
        - name: pageSize
          in: query
          default: 20
      responses:
        '200':
          description: 成功

    post:
      tags: [社交]
      summary: 创建群组
      security:
        - BearerAuth: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/GroupCreateRequest'
      responses:
        '201':
          description: 创建成功

  /groups/{groupId}:
    get:
      tags: [社交]
      summary: 获取群组详情
      parameters:
        - name: groupId
          in: path
          required: true
      responses:
        '200':
          description: 成功

    put:
      tags: [社交]
      summary: 编辑群组信息
      security:
        - BearerAuth: []
      parameters:
        - name: groupId
          in: path
          required: true
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/GroupCreateRequest'
      responses:
        '200':
          description: 更新成功

  /groups/{groupId}/join:
    post:
      tags: [社交]
      summary: 申请加入群组
      security:
        - BearerAuth: []
      parameters:
        - name: groupId
          in: path
          required: true
      responses:
        '200':
          description: 申请成功

  /groups/{groupId}/leave:
    post:
      tags: [社交]
      summary: 退出群组
      security:
        - BearerAuth: []
      parameters:
        - name: groupId
          in: path
          required: true
      responses:
        '200':
          description: 退出成功

  /groups/{groupId}/members:
    get:
      tags: [社交]
      summary: 获取群组成员列表
      parameters:
        - name: groupId
          in: path
          required: true
        - name: pageNum
          in: query
          default: 1
        - name: pageSize
          in: query
          default: 50
      responses:
        '200':
          description: 成功

  /groups/{groupId}/posts:
    get:
      tags: [社交]
      summary: 获取群组帖子列表
      parameters:
        - name: groupId
          in: path
          required: true
        - name: pageNum
          in: query
          default: 1
        - name: pageSize
          in: query
          default: 20
      responses:
        '200':
          description: 成功

    post:
      tags: [社交]
      summary: 在群组中发帖
      security:
        - BearerAuth: []
      parameters:
        - name: groupId
          in: path
          required: true
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              properties:
                content:
                  type: string
                attachments:
                  type: array
                  items:
                    type: string
      responses:
        '201':
          description: 发布成功

  # ==================== 搜索模块 ====================
  /search:
    get:
      tags: [搜索]
      summary: 全文搜索
      parameters:
        - name: keyword
          in: query
          required: true
          schema:
            type: string
        - name: type
          in: query
          schema:
            $ref: '#/components/schemas/SearchRequest/properties/type'
        - name: sortBy
          in: query
          schema:
            $ref: '#/components/schemas/SearchRequest/properties/sortBy'
        - name: pageNum
          in: query
          schema:
            type: integer
            default: 1
        - name: pageSize
          in: query
          schema:
            type: integer
            default: 20
      responses:
        '200':
          description: 成功
          content:
            application/json:
              schema:
                allOf:
                  - $ref: '#/components/schemas/ApiResponse'
                  - type: object
                    properties:
                      data:
                        $ref: '#/components/schemas/SearchResult'

    post:
      tags: [搜索]
      summary: 高级搜索（POST方式）
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/SearchRequest'
      responses:
        '200':
          description: 成功

  /search/suggest:
    get:
      tags: [搜索]
      summary: 搜索联想（自动补全）
      parameters:
        - name: keyword
          in: query
          required: true
          schema:
            type: string
        - name: limit
          in: query
          schema:
            type: integer
            default: 10
      responses:
        '200':
          description: 成功

  /search/hot-keywords:
    get:
      tags: [搜索]
      summary: 获取热门搜索关键词
      parameters:
        - name: period
          in: query
          schema:
            type: string
            enum: [day, week]
            default: day
        - name: limit
          in: query
          schema:
            type: integer
            default: 10
      responses:
        '200':
          description: 成功

  # ==================== 首页推荐模块 ====================
  /feed/recommend:
    get:
      tags: [内容]
      summary: 个性化推荐流
      security:
        - BearerAuth: []
      parameters:
        - name: pageNum
          in: query
          schema:
            type: integer
            default: 1
        - name: pageSize
          in: query
          schema:
            type: integer
            default: 20
      responses:
        '200':
          description: 成功

  /feed/hot:
    get:
      tags: [内容]
      summary: 热榜列表
      parameters:
        - name: period
          in: query
          schema:
            type: string
            enum: [day, week]
            default: day
        - name: limit
          in: query
          schema:
            type: integer
            default: 20
      responses:
        '200':
          description: 成功
          content:
            application/json:
              schema:
                allOf:
                  - $ref: '#/components/schemas/ApiResponse'
                  - type: object
                    properties:
                      data:
                        type: array
                        items:
                          $ref: '#/components/schemas/HotTopic'

  /feed/essence:
    get:
      tags: [内容]
      summary: 编辑精选列表
      parameters:
        - name: pageNum
          in: query
          default: 1
        - name: pageSize
          in: query
          default: 20
      responses:
        '200':
          description: 成功

  /feed/following:
    get:
      tags: [内容]
      summary: 关注动态流
      security:
        - BearerAuth: []
      parameters:
        - name: pageNum
          in: query
          default: 1
        - name: pageSize
          in: query
          default: 20
      responses:
        '200':
          description: 成功

  # ==================== 通知模块 ====================
  /notifications:
    get:
      tags: [通知]
      summary: 获取通知列表
      security:
        - BearerAuth: []
      parameters:
        - name: type
          in: query
          schema:
            type: string
            enum: [like, comment, follow, system, message]
        - name: isRead
          in: query
          schema:
            type: boolean
        - name: pageNum
          in: query
          default: 1
        - name: pageSize
          in: query
          default: 20
      responses:
        '200':
          description: 成功

  /notifications/unread-count:
    get:
      tags: [通知]
      summary: 获取未读通知数量
      security:
        - BearerAuth: []
      responses:
        '200':
          description: 成功

  /notifications/{notificationId}/read:
    put:
      tags: [通知]
      summary: 标记通知为已读
      security:
        - BearerAuth: []
      parameters:
        - name: notificationId
          in: path
          required: true
      responses:
        '200':
          description: 成功

  /notifications/read-all:
    put:
      tags: [通知]
      summary: 标记所有通知为已读
      security:
        - BearerAuth: []
      responses:
        '200':
          description: 成功

  # ==================== 举报模块 ====================
  /reports:
    post:
      tags: [互动]
      summary: 提交举报
      security:
        - BearerAuth: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ReportRequest'
      responses:
        '201':
          description: 举报成功

  # ==================== 附件模块 ====================
  /attachments/upload:
    post:
      tags: [内容]
      summary: 上传附件
      security:
        - BearerAuth: []
      requestBody:
        required: true
        content:
          multipart/form-data:
            schema:
              type: object
              properties:
                file:
                  type: string
                  format: binary
                fileType:
                  type: integer
                  description: 1-PDF 2-Excel 3-图片
      responses:
        '200':
          description: 上传成功
          content:
            application/json:
              schema:
                allOf:
                  - $ref: '#/components/schemas/ApiResponse'
                  - type: object
                    properties:
                      data:
                        $ref: '#/components/schemas/Attachment'

  # ==================== 管理模块（管理员专用）====================
  /admin/audit/pending:
    get:
      tags: [管理]
      summary: 获取待审核列表
      security:
        - BearerAuth: []
      parameters:
        - name: targetType
          in: query
          schema:
            type: integer
        - name: pageNum
          in: query
          default: 1
        - name: pageSize
          in: query
          default: 20
      responses:
        '200':
          description: 成功

  /admin/audit/{auditId}/approve:
    post:
      tags: [管理]
      summary: 审核通过
      security:
        - BearerAuth: []
      parameters:
        - name: auditId
          in: path
          required: true
      requestBody:
        content:
          application/json:
            schema:
              type: object
              properties:
                remark:
                  type: string
      responses:
        '200':
          description: 成功

  /admin/audit/{auditId}/reject:
    post:
      tags: [管理]
      summary: 审核驳回
      security:
        - BearerAuth: []
      parameters:
        - name: auditId
          in: path
          required: true
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              required:
                - reason
              properties:
                reason:
                  type: string
      responses:
        '200':
          description: 成功

  /admin/users/{userId}/punish:
    post:
      tags: [管理]
      summary: 处罚用户
      security:
        - BearerAuth: []
      parameters:
        - name: userId
          in: path
          required: true
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              required:
                - punishType
                - reason
              properties:
                punishType:
                  type: integer
                  enum: [1, 2, 3]
                  description: 1-警告 2-禁言 3-封号
                reason:
                  type: string
                durationDays:
                  type: integer
                  description: 处罚天数，0表示永久
      responses:
        '200':
          description: 成功

  /admin/users/{userId}/punish/{punishId}/lift:
    post:
      tags: [管理]
      summary: 解除处罚
      security:
        - BearerAuth: []
      parameters:
        - name: userId
          in: path
          required: true
        - name: punishId
          in: path
          required: true
      responses:
        '200':
          description: 成功

  /admin/sections:
    post:
      tags: [管理]
      summary: 新增板块
      security:
        - BearerAuth: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/Section'
      responses:
        '201':
          description: 创建成功

    put:
      tags: [管理]
      summary: 编辑板块
      security:
        - BearerAuth: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/Section'
      responses:
        '200':
          description: 更新成功

    delete:
      tags: [管理]
      summary: 删除板块
      security:
        - BearerAuth: []
      parameters:
        - name: sectionId
          in: query
          required: true
      responses:
        '204':
          description: 删除成功

  /admin/statistics/overview:
    get:
      tags: [管理]
      summary: 获取平台整体数据统计
      security:
        - BearerAuth: []
      parameters:
        - name: date
          in: query
          schema:
            type: string
            format: date
      responses:
        '200':
          description: 成功

  /admin/statistics/trend:
    get:
      tags: [管理]
      summary: 获取数据趋势
      security:
        - BearerAuth: []
      parameters:
        - name: startDate
          in: query
          required: true
          schema:
            type: string
            format: date
        - name: endDate
          in: query
          required: true
          schema:
            type: string
            format: date
        - name: metrics
          in: query
          schema:
            type: string
            description: 指标列表，逗号分隔
      responses:
        '200':
          description: 成功
