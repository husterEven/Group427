# 投资论坛系统 API 文档（完整版）

## 基本信息

- **标题**：投资论坛系统 API
- **描述**：
  - 投资论坛系统后端RESTful API接口文档
  - 认证方式：Bearer Token (JWT)
  - 请求格式：application/json
  - 响应格式：application/json
  - 接口设计规范：
    - 使用名词复数形式表示资源
    - HTTP方法：GET(查询)、POST(创建)、PUT(全量更新)、PATCH(部分更新)、DELETE(删除)
    - 状态码：200(成功)、201(创建成功)、400(参数错误)、401(未认证)、403(无权限)、404(资源不存在)、500(服务器错误)
- **版本**：1.0.0
- **联系方式**：API Support <support@forum.com>
- **许可证**：Proprietary

## 服务器地址

| 环境 | URL |
|------|-----|
| 生产环境 | `https://api.forum.com/v1` |
| 预发布环境 | `https://staging-api.forum.com/v1` |
| 本地开发环境 | `http://localhost:8080/v1` |

## 标签

| 标签 | 说明 |
|------|------|
| 认证 | 用户注册、登录、认证相关接口 |
| 用户 | 用户资料、偏好设置、成就查询 |
| 认证中心 | 实名认证、专业认证、风险测评 |
| 内容 | 帖子、评论、动态的增删改查 |
| 板块 | 板块与专区信息查询 |
| 社交 | 关注、私信、群组功能 |
| 搜索 | 全文搜索与筛选 |
| 互动 | 点赞、收藏、转发、举报 |
| 通知 | 消息通知相关 |
| 管理 | 管理员专用接口（需admin权限） |

---

## 通用组件定义

### securitySchemes

#### BearerAuth
- **类型**：http
- **方案**：bearer
- **Bearer格式**：JWT
- **描述**：使用JWT Token进行认证，在请求头中添加 `Authorization: Bearer {token}`

---

### schemas（数据模型）

#### ApiResponse
通用响应结构

| 属性 | 类型 | 描述 | 示例 |
|------|------|------|------|
| code | integer | 业务状态码，0表示成功 | 0 |
| message | string | 响应消息 | "success" |
| data | object | 响应数据，可空 | null |
| timestamp | integer (int64) | 时间戳 | 1702800000000 |

#### PageResponse
分页响应结构

| 属性 | 类型 | 描述 |
|------|------|------|
| code | integer | 状态码，示例0 |
| message | string | 消息，示例"success" |
| data | object | 数据对象 |
| data.list | array | 数据列表 |
| data.total | integer | 总记录数 |
| data.pageNum | integer | 当前页码 |
| data.pageSize | integer | 每页大小 |
| data.pages | integer | 总页数 |

---

### 用户相关 Schema

#### User
用户信息

| 属性 | 类型 | 约束 | 描述 | 示例 |
|------|------|------|------|------|
| userId | integer (int64) | - | 用户ID | 10001 |
| nickname | string | maxLength:30 | 昵称 | "价值投资客" |
| avatarUrl | string | - | 头像URL | "https://cdn.forum.com/avatar/10001.jpg" |
| bio | string | maxLength:200 | 个人简介 | "专注价值投资10年" |
| verificationLevel | integer | enum: [0,1,2,3] | 认证等级：0-未认证，1-基础，2-实名，3-专业V | 3 |
| riskLevel | integer | enum: [1,2,3,4] | 风险等级：1-保守，2-稳健，3-平衡，4-进取 | 3 |
| points | integer | - | 积分 | 1250 |
| level | integer | 范围1-100 | 用户等级 | 5 |
| postCount | integer | - | 发帖数 | 42 |
| followerCount | integer | - | 粉丝数 | 128 |
| followeeCount | integer | - | 关注数 | 86 |
| registerAt | string (date-time) | - | 注册时间 | "2023-01-01T00:00:00Z" |

#### UserProfileUpdate
用户资料更新

| 属性 | 类型 | 约束 | 描述 |
|------|------|------|------|
| nickname | string | maxLength:30 | 昵称 |
| avatarUrl | string | maxLength:512 | 头像URL |
| bio | string | maxLength:200 | 个人简介 |
| tags | array of strings | - | 投资经验标签 |

#### UserPreference
用户偏好设置

| 属性 | 类型 | 约束 | 描述 |
|------|------|------|------|
| focusMarkets | array of strings | enum: ["A股","港股","美股","基金","期货"] | 关注市场 |
| riskPreference | integer | enum: [1,2,3,4] | 风险偏好：1-保守，2-稳健，3-平衡，4-进取 |
| investmentExp | integer | enum: [1,2,3,4] | 投资经验：1-新手，2-进阶，3-资深，4-职业 |
| receiveNotification | boolean | - | 是否接收推送通知 |

#### PrivacySettings
隐私设置

| 属性 | 类型 | 约束 | 描述 |
|------|------|------|------|
| profileVisible | integer | enum: [0,1,2] | 0-公开，1-仅粉丝，2-仅自己 |
| postVisible | integer | enum: [0,1,2] | 同上 |
| followVisible | integer | enum: [0,1,2] | 同上 |
| allowSearch | boolean | - | 是否允许被搜索 |
| allowMessage | boolean | - | 是否允许私信 |

#### UserAchievement
用户成就

| 属性 | 类型 | 描述 |
|------|------|------|
| totalPosts | integer | 累计发帖 |
| essencePosts | integer | 精华帖数 |
| totalComments | integer | 累计评论 |
| totalLikesReceived | integer | 获赞总数 |
| influenceScore | integer | 影响力分数 |
| badges | array of objects | 徽章列表 |
| badges[].badgeId | integer | 徽章ID |
| badges[].name | string | 徽章名称 |
| badges[].iconUrl | string | 图标URL |
| badges[].obtainTime | string (date-time) | 获得时间 |

---

### 认证相关 Schema

#### LoginRequest
登录请求

| 属性 | 类型 | 必填 | 描述 | 示例 |
|------|------|------|------|------|
| account | string | 是 | 手机号/邮箱/昵称 | "13800000000" |
| password | string | 是 | 密码 | "password123" |
| captcha | string | 否 | 验证码 | "A3B9" |

#### LoginResponse
登录响应

| 属性 | 类型 | 描述 |
|------|------|------|
| token | string | JWT Token |
| expireAt | integer (int64) | 过期时间戳 |
| user | User | 用户信息 |

#### RegisterRequest
注册请求

| 属性 | 类型 | 必填 | 约束/描述 | 示例 |
|------|------|------|------|------|
| registerType | string | 是 | 枚举：mobile, email | "mobile" |
| mobile | string | 条件 | pattern: `^1[3-9]\d{9}$` | "13812345678" |
| email | string (email) | 条件 | 邮箱格式 | "user@example.com" |
| verificationCode | string | 否 | 验证码 | "123456" |
| password | string | 条件 | minLength:6, maxLength:20 | "password123" |
| agreement | boolean | 否 | 是否同意用户协议 | true |

#### SendVerificationCodeRequest
发送验证码请求

| 属性 | 类型 | 必填 | 描述 |
|------|------|------|------|
| contact | string | 是 | 手机号或邮箱 |
| type | string | 是 | 验证码类型：register, login, bind, reset |

#### ThirdPartyLoginRequest
第三方登录请求

| 属性 | 类型 | 必填 | 描述 |
|------|------|------|------|
| platform | string | 是 | 平台：wechat, weibo |
| authCode | string | 是 | 第三方授权码 |

---

### 认证中心 Schema

#### RealNameVerifyRequest
实名认证请求

| 属性 | 类型 | 必填 | 约束/描述 |
|------|------|------|------|
| realName | string | 是 | 真实姓名 |
| idCardNo | string | 是 | pattern: `^[1-9]\d{5}(18\|19\|20)\d{2}(0[1-9]\|1[0-2])(0[1-9]\|[12]\d\|3[01])\d{3}[\dXx]$` |
| idCardFrontUrl | string | 是 | 身份证正面照URL |
| idCardBackUrl | string | 是 | 身份证背面照URL |
| faceImageUrl | string | 是 | 人脸照片URL |

#### ProfessionVerifyRequest
专业认证请求

| 属性 | 类型 | 描述 |
|------|------|------|
| certificateUrls | array of strings | 证书材料URL列表 |
| educationUrls | array of strings | 学历证明URL列表 |
| professionType | string | 从业类型 |

#### RiskAssessmentSubmitRequest
风险测评提交请求

| 属性 | 类型 | 必填 | 描述 |
|------|------|------|------|
| answers | array of objects | 是 | 答案列表 |
| answers[].questionId | string | - | 问题ID |
| answers[].selectedOption | integer | - | 选中的选项编号 |

#### RiskAssessmentResult
风险测评结果

| 属性 | 类型 | 描述 |
|------|------|------|
| level | integer | 风险等级（1-4） |
| levelName | string | 等级名称 |
| score | integer | 得分 |
| description | string | 描述 |

---

### 帖子相关 Schema

#### Post
帖子摘要

| 属性 | 类型 | 描述 |
|------|------|------|
| postId | integer (int64) | 帖子ID |
| author | UserSimple | 作者信息 |
| title | string | 标题 |
| summary | string | 摘要 |
| contentType | integer | 1-普通，2-长文，3-投票，4-动态 |
| sectionId | integer | 板块ID |
| zoneId | integer | 专区ID |
| stockCode | string | 股票代码 |
| isPinned | boolean | 是否置顶 |
| isEssence | boolean | 是否精华 |
| stats | object | 统计数据 |
| stats.likes | integer | 点赞数 |
| stats.favorites | integer | 收藏数 |
| stats.shares | integer | 转发数 |
| stats.comments | integer | 评论数 |
| stats.views | integer | 浏览数 |
| publishedAt | string (date-time) | 发布时间 |

#### PostDetail
帖子详情（继承Post）

| 属性 | 类型 | 描述 |
|------|------|------|
| content | string | 纯文本内容 |
| richContent | string | HTML富文本内容 |
| attachments | array of Attachment | 附件列表 |
| voteInfo | VoteInfo | 投票信息（如果是投票帖） |

#### PostCreateRequest
创建普通帖子请求

| 属性 | 类型 | 必填 | 约束 | 描述 |
|------|------|------|------|------|
| title | string | 是 | maxLength:200 | 标题 |
| content | string | 是 | - | 内容 |
| contentType | integer | 否 | 默认1 | 内容类型 |
| sectionId | integer | 是 | - | 板块ID |
| zoneId | integer | 否 | - | 专区ID |
| stockCode | string | 否 | - | 股票代码 |
| attachments | array of strings | 否 | - | 附件ID列表 |

#### LongArticleRequest
长文请求（继承PostCreateRequest）

| 新增属性 | 类型 | 描述 |
|------|------|------|
| richContent | string | HTML格式富文本 |
| coverImage | string | 封面图URL |

#### VoteCreateRequest
投票创建请求

| 属性 | 类型 | 必填 | 约束 | 描述 |
|------|------|------|------|------|
| title | string | 是 | - | 投票标题 |
| options | array of strings | 是 | minItems:2, maxItems:10 | 选项列表 |
| maxChoices | integer | 否 | min:1, max:5, default:1 | 最多可选数量 |
| durationHours | integer | 否 | default:24 | 持续小时数 |
| isAnonymous | boolean | 否 | default:false | 是否匿名投票 |

#### VoteInfo
投票信息

| 属性 | 类型 | 描述 |
|------|------|------|
| voteId | integer | 投票ID |
| title | string | 标题 |
| options | array of objects | 选项列表 |
| options[].index | integer | 选项索引 |
| options[].text | string | 选项文本 |
| options[].count | integer | 得票数 |
| options[].percentage | number | 百分比 |
| maxChoices | integer | 最大可选数 |
| totalVotes | integer | 总投票数 |
| isEnded | boolean | 是否已结束 |
| userSelected | array of integers | 当前用户选择的选项（已登录时） |

#### VoteCastRequest
投票请求

| 属性 | 类型 | 必填 | 约束 |
|------|------|------|------|
| selectedOptions | array of integers | 是 | minItems:1 选中的选项索引 |

---

### 评论相关 Schema

#### Comment
评论信息

| 属性 | 类型 | 描述 |
|------|------|------|
| commentId | integer (int64) | 评论ID |
| postId | integer | 所属帖子ID |
| author | UserSimple | 作者 |
| content | string | 内容 |
| parentId | integer | 父评论ID |
| rootId | integer | 根评论ID |
| stats | object | 统计 |
| stats.likes | integer | 点赞数 |
| stats.replies | integer | 回复数 |
| publishedAt | string (date-time) | 发布时间 |
| atUsers | array of UserSimple | @的用户列表 |

#### CommentCreateRequest
创建评论请求

| 属性 | 类型 | 必填 | 约束 | 描述 |
|------|------|------|------|------|
| content | string | 是 | maxLength:2000 | 评论内容 |
| parentId | integer (int64) | 否 | - | 回复的评论ID |
| atUserIds | array of integers | 否 | - | @的用户ID列表 |

---

### 板块相关 Schema

#### Section
板块

| 属性 | 类型 | 描述 |
|------|------|------|
| sectionId | integer | 板块ID |
| name | string | 名称 |
| slug | string | 标识符 |
| icon | string | 图标URL |
| description | string | 描述 |
| postCount | integer | 帖子数 |
| children | array of Zone | 子专区列表 |

#### Zone
专区

| 属性 | 类型 | 描述 |
|------|------|------|
| zoneId | integer | 专区ID |
| sectionId | integer | 所属板块ID |
| name | string | 名称 |
| slug | string | 标识符 |
| icon | string | 图标URL |
| description | string | 描述 |
| postCount | integer | 帖子数 |

---

### 社交相关 Schema

#### UserSimple
用户简要信息

| 属性 | 类型 | 描述 |
|------|------|------|
| userId | integer | 用户ID |
| nickname | string | 昵称 |
| avatarUrl | string | 头像URL |
| verificationLevel | integer | 认证等级 |

#### FollowInfo
关注状态

| 属性 | 类型 | 描述 |
|------|------|------|
| followerCount | integer | 粉丝数 |
| followeeCount | integer | 关注数 |
| isFollowing | boolean | 当前用户是否关注对方 |
| isFollowed | boolean | 对方是否关注当前用户 |
| isStarred | boolean | 是否为星标关注 |

#### PrivateMessage
私信

| 属性 | 类型 | 描述 |
|------|------|------|
| messageId | integer | 消息ID |
| sessionId | string | 会话ID |
| sender | UserSimple | 发送者 |
| receiver | UserSimple | 接收者 |
| messageType | integer | 消息类型 |
| content | string | 内容 |
| isRead | boolean | 是否已读 |
| isRecalled | boolean | 是否已撤回 |
| sentAt | string (date-time) | 发送时间 |

#### PrivateMessageSendRequest
发送私信请求

| 属性 | 类型 | 必填 | 约束 | 描述 |
|------|------|------|------|------|
| receiverId | integer | 是 | - | 接收者ID |
| messageType | integer | 否 | default:1 | 消息类型 |
| content | string | 是 | maxLength:2000 | 内容 |

#### Group
群组

| 属性 | 类型 | 描述 |
|------|------|------|
| groupId | integer | 群组ID |
| name | string | 名称 |
| avatar | string | 头像URL |
| introduction | string | 简介 |
| tags | array of strings | 标签 |
| mode | integer | 模式（公开/私密） |
| memberCount | integer | 成员数 |
| postCount | integer | 帖子数 |
| owner | UserSimple | 群主 |
| createdAt | string (date-time) | 创建时间 |

#### GroupCreateRequest
创建群组请求

| 属性 | 类型 | 必填 | 约束 | 描述 |
|------|------|------|------|------|
| name | string | 是 | maxLength:50 | 名称 |
| introduction | string | 否 | maxLength:200 | 简介 |
| tags | array of strings | 否 | - | 标签 |
| mode | integer | 否 | default:1 | 模式 |
| avatar | string | 否 | - | 头像URL |

---

### 互动相关 Schema

#### InteractionStats
互动统计

| 属性 | 类型 | 描述 |
|------|------|------|
| liked | boolean | 当前用户是否已点赞 |
| favorited | boolean | 是否已收藏 |
| shared | boolean | 是否已转发 |
| likeCount | integer | 点赞总数 |
| favoriteCount | integer | 收藏总数 |
| shareCount | integer | 转发总数 |

#### ReportRequest
举报请求

| 属性 | 类型 | 必填 | 约束 | 描述 |
|------|------|------|------|------|
| targetType | integer | 是 | enum:[1,2,3] | 目标类型：1-帖子，2-评论，3-用户 |
| targetId | integer | 是 | - | 目标ID |
| reasonType | integer | 是 | enum:[1,2,3,4,5] | 举报原因类型 |
| reasonDesc | string | 否 | maxLength:200 | 补充描述 |

---

### 搜索相关 Schema

#### SearchRequest
搜索请求

| 属性 | 类型 | 约束/默认 | 描述 |
|------|------|------|------|
| keyword | string | - | 搜索关键词 |
| type | string | enum:[all,post,user,stock], default:all | 搜索类型 |
| sortBy | string | enum:[relevance,time,likes,comments], default:relevance | 排序方式 |
| timeRange | string | enum:[all,today,week,month,year], default:all | 时间范围 |
| sectionId | integer | - | 板块筛选 |
| isEssence | boolean | - | 是否精华帖 |
| pageNum | integer | min:1, default:1 | 页码 |
| pageSize | integer | min:1, max:50, default:20 | 每页大小 |

#### SearchResult
搜索结果

| 属性 | 类型 | 描述 |
|------|------|------|
| keyword | string | 搜索关键词 |
| total | integer | 总数 |
| posts | array of Post | 帖子结果 |
| users | array of UserSimple | 用户结果 |
| stocks | array of objects | 股票结果 |
| stocks[].code | string | 股票代码 |
| stocks[].name | string | 股票名称 |
| stocks[].postCount | integer | 相关帖子数 |

---

### 通知相关 Schema

#### Notification
通知消息

| 属性 | 类型 | 描述 |
|------|------|------|
| notificationId | integer | 通知ID |
| type | string | 类型：like, comment, follow, system, message |
| title | string | 标题 |
| content | string | 内容 |
| isRead | boolean | 是否已读 |
| targetId | integer | 关联的目标ID（如帖子ID） |
| createdAt | string (date-time) | 创建时间 |

---

### 通用 Schema

#### Attachment
附件

| 属性 | 类型 | 描述 |
|------|------|------|
| attachmentId | integer | 附件ID |
| fileName | string | 文件名 |
| fileSize | integer | 文件大小（字节） |
| fileUrl | string | 文件URL |

#### HotTopic
热门话题

| 属性 | 类型 | 描述 |
|------|------|------|
| rank | integer | 排名 |
| name | string | 名称 |
| heatValue | integer | 热度值 |
| postCount | integer | 帖子数 |
| type | string | 类型：topic, stock |

---

## API 路径定义

### 1. 认证模块

#### POST /auth/login
- **标签**：认证
- **摘要**：用户登录
- **请求体**：application/json，schema: LoginRequest
- **响应 200**：成功，返回 LoginResponse（包装在ApiResponse中）
- **响应 401**：用户名或密码错误

#### POST /auth/logout
- **标签**：认证
- **摘要**：用户登出
- **安全**：BearerAuth
- **响应 200**：登出成功

#### POST /auth/register
- **标签**：认证
- **摘要**：用户注册
- **请求体**：RegisterRequest
- **响应 201**：注册成功，返回 LoginResponse

#### POST /auth/captcha/send
- **标签**：认证
- **摘要**：发送验证码
- **请求体**：SendVerificationCodeRequest
- **响应 200**：发送成功

#### POST /auth/third-party/login
- **标签**：认证
- **摘要**：第三方登录
- **请求体**：ThirdPartyLoginRequest
- **响应 200**：登录成功

#### POST /auth/refresh
- **标签**：认证
- **摘要**：刷新Token
- **安全**：BearerAuth
- **响应 200**：刷新成功

---

### 2. 用户模块

#### GET /users/me
- **标签**：用户
- **摘要**：获取当前用户信息
- **安全**：BearerAuth
- **响应 200**：成功，返回 User

#### PUT /users/me
- **标签**：用户
- **摘要**：更新当前用户资料
- **安全**：BearerAuth
- **请求体**：UserProfileUpdate
- **响应 200**：更新成功

#### GET /users/{userId}
- **标签**：用户
- **摘要**：获取指定用户信息
- **参数**：`userId` (path, integer, int64, 必填)
- **响应 200**：成功，返回 User

#### GET /users/me/preference
- **标签**：用户
- **摘要**：获取用户偏好设置
- **安全**：BearerAuth
- **响应 200**：成功

#### PUT /users/me/preference
- **标签**：用户
- **摘要**：更新用户偏好设置
- **安全**：BearerAuth
- **请求体**：UserPreference
- **响应 200**：更新成功

#### GET /users/me/privacy
- **标签**：用户
- **摘要**：获取隐私设置
- **安全**：BearerAuth
- **响应 200**：成功

#### PUT /users/me/privacy
- **标签**：用户
- **摘要**：更新隐私设置
- **安全**：BearerAuth
- **请求体**：PrivacySettings
- **响应 200**：更新成功

#### GET /users/me/achievement
- **标签**：用户
- **摘要**：获取用户成就
- **安全**：BearerAuth
- **响应 200**：成功，返回 UserAchievement

---

### 3. 认证中心模块

#### POST /verify/real-name
- **标签**：认证中心
- **摘要**：提交实名认证
- **安全**：BearerAuth
- **请求体**：RealNameVerifyRequest
- **响应 200**：提交成功

#### POST /verify/profession
- **标签**：认证中心
- **摘要**：提交专业认证
- **安全**：BearerAuth
- **请求体**：ProfessionVerifyRequest
- **响应 200**：提交成功

#### GET /verify/status
- **标签**：认证中心
- **摘要**：获取认证状态
- **安全**：BearerAuth
- **响应 200**：成功

#### POST /risk-assessment
- **标签**：认证中心
- **摘要**：提交风险测评
- **安全**：BearerAuth
- **请求体**：RiskAssessmentSubmitRequest
- **响应 200**：提交成功，返回 RiskAssessmentResult

#### GET /risk-assessment
- **标签**：认证中心
- **摘要**：获取当前风险测评结果
- **安全**：BearerAuth
- **响应 200**：成功

---

### 4. 内容模块 - 帖子

#### GET /posts
- **标签**：内容
- **摘要**：获取帖子列表
- **参数**：
  - `sectionId` (query, integer)
  - `zoneId` (query, integer)
  - `sortBy` (query, string, enum: latest/hottest/essence, default: latest)
  - `pageNum` (query, integer, default:1)
  - `pageSize` (query, integer, default:20)
- **响应 200**：成功，返回 PageResponse

#### POST /posts
- **标签**：内容
- **摘要**：发布普通帖子
- **安全**：BearerAuth
- **请求体**：PostCreateRequest
- **响应 201**：发布成功

#### POST /posts/long-article
- **标签**：内容
- **摘要**：发布长文分析
- **安全**：BearerAuth
- **请求体**：LongArticleRequest
- **响应 201**：发布成功

#### POST /posts/vote
- **标签**：内容
- **摘要**：发布投票帖
- **安全**：BearerAuth
- **请求体**：包含 PostCreateRequest 和 vote (VoteCreateRequest)
- **响应 201**：发布成功

#### GET /posts/{postId}
- **标签**：内容
- **摘要**：获取帖子详情
- **参数**：`postId` (path, integer, int64, 必填)
- **响应 200**：成功，返回 PostDetail

#### PUT /posts/{postId}
- **标签**：内容
- **摘要**：编辑帖子
- **安全**：BearerAuth
- **参数**：`postId` (path, 必填)
- **请求体**：PostCreateRequest
- **响应 200**：更新成功

#### DELETE /posts/{postId}
- **标签**：内容
- **摘要**：删除帖子
- **安全**：BearerAuth
- **参数**：`postId` (path, 必填)
- **响应 204**：删除成功

#### POST /posts/{postId}/vote/cast
- **标签**：内容
- **摘要**：参与投票
- **安全**：BearerAuth
- **参数**：`postId` (path, 必填)
- **请求体**：VoteCastRequest
- **响应 200**：投票成功

#### GET /posts/{postId}/vote/result
- **标签**：内容
- **摘要**：获取投票结果
- **参数**：`postId` (path, 必填)
- **响应 200**：成功

#### GET /posts/{postId}/interaction
- **标签**：互动
- **摘要**：获取帖子互动统计
- **安全**：BearerAuth
- **参数**：`postId` (path, 必填)
- **响应 200**：成功，返回 InteractionStats

#### POST /posts/{postId}/like
- **标签**：互动
- **摘要**：点赞帖子
- **安全**：BearerAuth
- **参数**：`postId` (path, 必填)
- **响应 200**：成功

#### DELETE /posts/{postId}/like
- **标签**：互动
- **摘要**：取消点赞
- **安全**：BearerAuth
- **参数**：`postId` (path, 必填)
- **响应 204**：取消成功

#### POST /posts/{postId}/favorite
- **标签**：互动
- **摘要**：收藏帖子
- **安全**：BearerAuth
- **参数**：`postId` (path, 必填)
- **响应 200**：成功

#### DELETE /posts/{postId}/favorite
- **标签**：互动
- **摘要**：取消收藏
- **安全**：BearerAuth
- **参数**：`postId` (path, 必填)
- **响应 204**：取消成功

#### POST /posts/{postId}/share
- **标签**：互动
- **摘要**：转发帖子
- **安全**：BearerAuth
- **参数**：`postId` (path, 必填)
- **响应 200**：成功

---

### 5. 内容模块 - 评论

#### GET /posts/{postId}/comments
- **标签**：内容
- **摘要**：获取帖子评论列表
- **参数**：
  - `postId` (path, 必填)
  - `pageNum` (query, integer, default:1)
  - `pageSize` (query, integer, default:20)
  - `sortBy` (query, string, enum: latest/hottest, default: latest)
- **响应 200**：成功

#### POST /posts/{postId}/comments
- **标签**：内容
- **摘要**：发表评论
- **安全**：BearerAuth
- **参数**：`postId` (path, 必填)
- **请求体**：CommentCreateRequest
- **响应 201**：发表成功

#### DELETE /comments/{commentId}
- **标签**：内容
- **摘要**：删除评论
- **安全**：BearerAuth
- **参数**：`commentId` (path, 必填)
- **响应 204**：删除成功

#### POST /comments/{commentId}/like
- **标签**：互动
- **摘要**：点赞评论
- **安全**：BearerAuth
- **参数**：`commentId` (path, 必填)
- **响应 200**：成功

#### DELETE /comments/{commentId}/like
- **标签**：互动
- **摘要**：取消点赞评论
- **安全**：BearerAuth
- **参数**：`commentId` (path, 必填)
- **响应 204**：取消成功

---

### 6. 板块模块

#### GET /sections
- **标签**：板块
- **摘要**：获取所有板块
- **响应 200**：成功，返回 Section 数组

#### GET /sections/{sectionId}/zones
- **标签**：板块
- **摘要**：获取板块下的专区
- **参数**：`sectionId` (path, 必填)
- **响应 200**：成功

---

### 7. 盘中动态

#### GET /dynamics
- **标签**：内容
- **摘要**：获取实时动态列表（时间线）
- **参数**：`pageNum` (query, integer, default:1), `pageSize` (query, integer, default:20)
- **响应 200**：成功

#### POST /dynamics
- **标签**：内容
- **摘要**：发布盘中动态
- **安全**：BearerAuth
- **请求体**：
  - `content` (string, maxLength:500)
  - `imageUrls` (array of strings)
  - `stockCode` (string)
- **响应 201**：发布成功

---

### 8. 社交模块 - 关注

#### POST /users/{userId}/follow
- **标签**：社交
- **摘要**：关注用户
- **安全**：BearerAuth
- **参数**：`userId` (path, 必填)
- **响应 200**：关注成功

#### DELETE /users/{userId}/follow
- **标签**：社交
- **摘要**：取消关注
- **安全**：BearerAuth
- **参数**：`userId` (path, 必填)
- **响应 204**：取消成功

#### POST /users/{userId}/follow/star
- **标签**：社交
- **摘要**：设为星标关注
- **安全**：BearerAuth
- **参数**：`userId` (path, 必填)
- **响应 200**：成功

#### DELETE /users/{userId}/follow/star
- **标签**：社交
- **摘要**：取消星标
- **安全**：BearerAuth
- **参数**：`userId` (path, 必填)
- **响应 204**：成功

#### GET /users/me/followers
- **标签**：社交
- **摘要**：获取我的粉丝列表
- **安全**：BearerAuth
- **参数**：`pageNum`, `pageSize`
- **响应 200**：成功

#### GET /users/me/followees
- **标签**：社交
- **摘要**：获取我关注的人列表
- **安全**：BearerAuth
- **参数**：`pageNum`, `pageSize`, `starredOnly` (boolean, default:false)
- **响应 200**：成功

#### GET /users/{userId}/follow/status
- **标签**：社交
- **摘要**：获取关注状态
- **安全**：BearerAuth
- **参数**：`userId` (path, 必填)
- **响应 200**：返回 FollowInfo

---

### 9. 社交模块 - 私信

#### GET /messages
- **标签**：社交
- **摘要**：获取会话列表
- **安全**：BearerAuth
- **参数**：`pageNum`, `pageSize`
- **响应 200**：成功

#### GET /messages/session/{sessionId}
- **标签**：社交
- **摘要**：获取与某用户的聊天记录
- **安全**：BearerAuth
- **参数**：`sessionId` (path, 必填), `pageNum`, `pageSize` (default:50)
- **响应 200**：成功

#### POST /messages/session/{sessionId}
- **标签**：社交
- **摘要**：发送私信
- **安全**：BearerAuth
- **参数**：`sessionId` (path, 必填)
- **请求体**：PrivateMessageSendRequest
- **响应 201**：发送成功

#### POST /messages/{messageId}/recall
- **标签**：社交
- **摘要**：撤回私信
- **安全**：BearerAuth
- **参数**：`messageId` (path, 必填)
- **响应 200**：撤回成功

#### GET /messages/unread/count
- **标签**：社交
- **摘要**：获取未读私信数量
- **安全**：BearerAuth
- **响应 200**：成功

---

### 10. 社交模块 - 群组

#### GET /groups
- **标签**：社交
- **摘要**：获取群组列表
- **参数**：`keyword` (query, string), `pageNum` (default:1), `pageSize` (default:20)
- **响应 200**：成功

#### POST /groups
- **标签**：社交
- **摘要**：创建群组
- **安全**：BearerAuth
- **请求体**：GroupCreateRequest
- **响应 201**：创建成功

#### GET /groups/{groupId}
- **标签**：社交
- **摘要**：获取群组详情
- **参数**：`groupId` (path, 必填)
- **响应 200**：成功

#### PUT /groups/{groupId}
- **标签**：社交
- **摘要**：编辑群组信息
- **安全**：BearerAuth
- **参数**：`groupId` (path, 必填)
- **请求体**：GroupCreateRequest
- **响应 200**：更新成功

#### POST /groups/{groupId}/join
- **标签**：社交
- **摘要**：申请加入群组
- **安全**：BearerAuth
- **参数**：`groupId` (path, 必填)
- **响应 200**：申请成功

#### POST /groups/{groupId}/leave
- **标签**：社交
- **摘要**：退出群组
- **安全**：BearerAuth
- **参数**：`groupId` (path, 必填)
- **响应 200**：退出成功

#### GET /groups/{groupId}/members
- **标签**：社交
- **摘要**：获取群组成员列表
- **参数**：`groupId` (path, 必填), `pageNum` (default:1), `pageSize` (default:50)
- **响应 200**：成功

#### GET /groups/{groupId}/posts
- **标签**：社交
- **摘要**：获取群组帖子列表
- **参数**：`groupId` (path, 必填), `pageNum`, `pageSize`
- **响应 200**：成功

#### POST /groups/{groupId}/posts
- **标签**：社交
- **摘要**：在群组中发帖
- **安全**：BearerAuth
- **参数**：`groupId` (path, 必填)
- **请求体**：`content` (string), `attachments` (array of strings)
- **响应 201**：发布成功

---

### 11. 搜索模块

#### GET /search
- **标签**：搜索
- **摘要**：全文搜索
- **参数**：
  - `keyword` (query, 必填)
  - `type` (query, enum: all/post/user/stock, default:all)
  - `sortBy` (query, enum: relevance/time/likes/comments, default:relevance)
  - `pageNum` (default:1)
  - `pageSize` (default:20)
- **响应 200**：返回 SearchResult

#### POST /search
- **标签**：搜索
- **摘要**：高级搜索（POST方式）
- **请求体**：SearchRequest
- **响应 200**：成功

#### GET /search/suggest
- **标签**：搜索
- **摘要**：搜索联想（自动补全）
- **参数**：`keyword` (query, 必填), `limit` (query, integer, default:10)
- **响应 200**：成功

#### GET /search/hot-keywords
- **标签**：搜索
- **摘要**：获取热门搜索关键词
- **参数**：`period` (query, enum: day/week, default:day), `limit` (integer, default:10)
- **响应 200**：成功

---

### 12. 首页推荐模块

#### GET /feed/recommend
- **标签**：内容
- **摘要**：个性化推荐流
- **安全**：BearerAuth
- **参数**：`pageNum`, `pageSize`
- **响应 200**：成功

#### GET /feed/hot
- **标签**：内容
- **摘要**：热榜列表
- **参数**：`period` (enum: day/week, default:day), `limit` (integer, default:20)
- **响应 200**：返回 HotTopic 数组

#### GET /feed/essence
- **标签**：内容
- **摘要**：编辑精选列表
- **参数**：`pageNum`, `pageSize`
- **响应 200**：成功

#### GET /feed/following
- **标签**：内容
- **摘要**：关注动态流
- **安全**：BearerAuth
- **参数**：`pageNum`, `pageSize`
- **响应 200**：成功

---

### 13. 通知模块

#### GET /notifications
- **标签**：通知
- **摘要**：获取通知列表
- **安全**：BearerAuth
- **参数**：`type` (enum: like/comment/follow/system/message), `isRead` (boolean), `pageNum`, `pageSize`
- **响应 200**：成功

#### GET /notifications/unread-count
- **标签**：通知
- **摘要**：获取未读通知数量
- **安全**：BearerAuth
- **响应 200**：成功

#### PUT /notifications/{notificationId}/read
- **标签**：通知
- **摘要**：标记通知为已读
- **安全**：BearerAuth
- **参数**：`notificationId` (path, 必填)
- **响应 200**：成功

#### PUT /notifications/read-all
- **标签**：通知
- **摘要**：标记所有通知为已读
- **安全**：BearerAuth
- **响应 200**：成功

---

### 14. 举报模块

#### POST /reports
- **标签**：互动
- **摘要**：提交举报
- **安全**：BearerAuth
- **请求体**：ReportRequest
- **响应 201**：举报成功

---

### 15. 附件模块

#### POST /attachments/upload
- **标签**：内容
- **摘要**：上传附件
- **安全**：BearerAuth
- **请求体**：multipart/form-data
  - `file` (binary, 必填)
  - `fileType` (integer, 描述: 1-PDF, 2-Excel, 3-图片)
- **响应 200**：返回 Attachment

---

### 16. 管理模块（管理员专用）

#### GET /admin/audit/pending
- **标签**：管理
- **摘要**：获取待审核列表
- **安全**：BearerAuth (需admin权限)
- **参数**：`targetType` (integer), `pageNum`, `pageSize`
- **响应 200**：成功

#### POST /admin/audit/{auditId}/approve
- **标签**：管理
- **摘要**：审核通过
- **安全**：BearerAuth
- **参数**：`auditId` (path, 必填)
- **请求体**：`remark` (string)
- **响应 200**：成功

#### POST /admin/audit/{auditId}/reject
- **标签**：管理
- **摘要**：审核驳回
- **安全**：BearerAuth
- **参数**：`auditId` (path, 必填)
- **请求体**：`reason` (string, 必填)
- **响应 200**：成功

#### POST /admin/users/{userId}/punish
- **标签**：管理
- **摘要**：处罚用户
- **安全**：BearerAuth
- **参数**：`userId` (path, 必填)
- **请求体**：
  - `punishType` (integer, enum:[1,2,3], 1-警告,2-禁言,3-封号, 必填)
  - `reason` (string, 必填)
  - `durationDays` (integer, 处罚天数，0表示永久)
- **响应 200**：成功

#### POST /admin/users/{userId}/punish/{punishId}/lift
- **标签**：管理
- **摘要**：解除处罚
- **安全**：BearerAuth
- **参数**：`userId`, `punishId` (path, 必填)
- **响应 200**：成功

#### POST /admin/sections
- **标签**：管理
- **摘要**：新增板块
- **安全**：BearerAuth
- **请求体**：Section
- **响应 201**：创建成功

#### PUT /admin/sections
- **标签**：管理
- **摘要**：编辑板块
- **安全**：BearerAuth
- **请求体**：Section
- **响应 200**：更新成功

#### DELETE /admin/sections
- **标签**：管理
- **摘要**：删除板块
- **安全**：BearerAuth
- **参数**：`sectionId` (query, 必填)
- **响应 204**：删除成功

#### GET /admin/statistics/overview
- **标签**：管理
- **摘要**：获取平台整体数据统计
- **安全**：BearerAuth
- **参数**：`date` (query, string, format: date)
- **响应 200**：成功

#### GET /admin/statistics/trend
- **标签**：管理
- **摘要**：获取数据趋势
- **安全**：BearerAuth
- **参数**：
  - `startDate` (query, date, 必填)
  - `endDate` (query, date, 必填)
  - `metrics` (query, string, 指标列表，逗号分隔)
- **响应 200**：成功

---

## 错误码说明

| HTTP状态码 | 说明 |
|------------|------|
| 200 | 成功 |
| 201 | 创建成功 |
| 400 | 参数错误 |
| 401 | 未认证（未登录或Token失效） |
| 403 | 无权限（权限不足） |
| 404 | 资源不存在 |
| 500 | 服务器错误 |

> **业务状态码**：响应中的 `code` 字段，0 表示成功，非0表示失败，具体错误信息见 `message` 字段。
