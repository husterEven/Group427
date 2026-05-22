

## 论坛系统数据库设计

### 一、优化说明

| 优化项 | 优化前问题 | 优化方案 |
|--------|------------|----------|
| 主键类型 | 全部使用BIGINT | 区分业务：高频表用BIGINT，字典表用INT |
| 时间字段 | 缺少更新时间 | 增加update_time字段，支持乐观锁 |
| 逻辑删除 | 未设计 | 增加is_deleted字段，软删除替代硬删除 |
| 冗余字段 | 统计字段实时查询 | 增加冗余统计字段，读写分离 |
| 索引覆盖 | 索引不足 | 增加联合索引，覆盖常用查询 |
| 分表策略 | 未考虑 | 行为日志类表按时间分表设计 |
| 枚举字段 | 无默认值 | 统一使用TINYINT + 默认值 |

---

### 二、实体关系总览

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                           论坛系统数据库设计（优化版）                            │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  ┌─────────────┐      ┌─────────────┐      ┌─────────────┐      ┌─────────────┐ │
│  │   用户域    │◄────►│   内容域    │◄────►│   板块域    │      │   统计域    │ │
│  │  (6张表)    │      │  (7张表)    │      │  (2张表)    │      │  (4张表)    │ │
│  └──────┬──────┘      └──────┬──────┘      └──────┬──────┘      └──────┬──────┘ │
│         │                    │                    │                    │       │
│         ▼                    ▼                    ▼                    ▼       │
│  ┌─────────────┐      ┌─────────────┐      ┌─────────────┐      ┌─────────────┐ │
│  │   社交域    │      │   运营域    │      │   认证域    │      │   日志域    │ │
│  │  (5张表)    │      │  (6张表)    │      │  (3张表)    │      │  (3张表)    │ │
│  └─────────────┘      └─────────────┘      └─────────────┘      └─────────────┘ │
│                                                                                 │
│  总计：36张表（含3张日志分表原型）                                                │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

### 三、用户域表设计

#### 3.1 用户表（user）

| 字段名 | 类型 | 约束 | 默认值 | 说明 |
|--------|------|------|--------|------|
| user_id | BIGINT UNSIGNED | PK AUTO | - | 用户唯一标识 |
| nickname | VARCHAR(30) | NOT NULL | - | 用户昵称（最长30字符） |
| avatar_url | VARCHAR(512) | | - | 头像URL |
| bio | VARCHAR(200) | | '' | 个人简介 |
| mobile | CHAR(11) | UNIQUE | - | 手机号（11位） |
| email | VARCHAR(100) | UNIQUE | - | 邮箱地址 |
| password_salt | CHAR(32) | NOT NULL | - | 密码盐值 |
| password_hash | CHAR(64) | NOT NULL | - | 密码哈希（SHA256） |
| wechat_open_id | VARCHAR(100) | UNIQUE | - | 微信OpenID |
| weibo_uid | VARCHAR(100) | UNIQUE | - | 微博UID |
| verification_level | TINYINT UNSIGNED | NOT NULL | 0 | 0-未认证，1-基础，2-实名，3-专业V |
| risk_level | TINYINT UNSIGNED | | NULL | 1-保守，2-稳健，3-平衡，4-进取 |
| points | INT UNSIGNED | NOT NULL | 0 | 积分总数 |
| level | SMALLINT UNSIGNED | NOT NULL | 1 | 用户等级（1-100） |
| post_count | INT UNSIGNED | NOT NULL | 0 | 发帖数（冗余） |
| follower_count | INT UNSIGNED | NOT NULL | 0 | 粉丝数（冗余） |
| followee_count | INT UNSIGNED | NOT NULL | 0 | 关注数（冗余） |
| status | TINYINT UNSIGNED | NOT NULL | 1 | 1-正常，2-禁言，3-封禁 |
| mute_expire_at | DATETIME | | NULL | 禁言到期时间 |
| register_at | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 注册时间 |
| last_login_at | DATETIME | | NULL | 最后登录时间 |
| last_login_ip | VARCHAR(45) | | NULL | 最后登录IP |
| is_deleted | TINYINT UNSIGNED | NOT NULL | 0 | 逻辑删除标记 |
| created_at | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | NOT NULL | CURRENT_TIMESTAMP ON UPDATE | 更新时间 |



#### 3.2 用户认证记录表（user_verification）

| 字段名 | 类型 | 约束 | 默认值 | 说明 |
|--------|------|------|--------|------|
| record_id | BIGINT UNSIGNED | PK AUTO | - | 认证记录ID |
| user_id | BIGINT UNSIGNED | FK NOT NULL | - | 用户ID |
| verify_type | TINYINT UNSIGNED | NOT NULL | - | 1-实名，2-专业 |
| real_name | VARCHAR(50) | | NULL | 真实姓名 |
| id_card_no | CHAR(18) | | NULL | 身份证号（加密存储） |
| id_card_front_url | VARCHAR(512) | | NULL | 身份证正面照 |
| id_card_back_url | VARCHAR(512) | | NULL | 身份证背面照 |
| face_image_url | VARCHAR(512) | | NULL | 人脸识别照片 |
| certificate_urls | TEXT | | NULL | 专业证书材料（JSON数组） |
| education_urls | TEXT | | NULL | 学历证明材料（JSON数组） |
| audit_status | TINYINT UNSIGNED | NOT NULL | 0 | 0-待审，1-通过，2-拒绝 |
| audit_remark | VARCHAR(200) | | '' | 审核备注 |
| auditor_id | BIGINT UNSIGNED | | NULL | 审核人ID |
| audit_at | DATETIME | | NULL | 审核时间 |
| created_at | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | NOT NULL | CURRENT_TIMESTAMP ON UPDATE | 更新时间 |



#### 3.3 用户风险测评记录表（user_risk_assessment）

| 字段名 | 类型 | 约束 | 默认值 | 说明 |
|--------|------|------|--------|------|
| assess_id | BIGINT UNSIGNED | PK AUTO | - | 测评记录ID |
| user_id | BIGINT UNSIGNED | FK NOT NULL | - | 用户ID |
| version | VARCHAR(20) | NOT NULL | '1.0' | 问卷版本 |
| answers_json | JSON | NOT NULL | - | 答案详情（MySQL JSON类型） |
| total_score | SMALLINT UNSIGNED | NOT NULL | - | 总分 |
| risk_level | TINYINT UNSIGNED | NOT NULL | - | 1-保守，2-稳健，3-平衡，4-进取 |
| is_current | TINYINT UNSIGNED | NOT NULL | 0 | 是否为当前有效测评 |
| expired_at | DATETIME | | NULL | 有效期截止时间 |
| created_at | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 完成时间 |



#### 3.4 用户偏好设置表（user_preference）

| 字段名 | 类型 | 约束 | 默认值 | 说明 |
|--------|------|------|--------|------|
| user_id | BIGINT UNSIGNED | PK FK | - | 用户ID |
| focus_markets | SET('A股','港股','美股','基金','期货') | | '' | 关注市场（SET类型） |
| risk_preference | TINYINT UNSIGNED | | NULL | 1-保守，2-稳健，3-平衡，4-进取 |
| investment_exp | TINYINT UNSIGNED | | NULL | 1-新手，2-进阶，3-资深，4-职业 |
| receive_notification | TINYINT UNSIGNED | NOT NULL | 1 | 是否接收推送通知 |
| created_at | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | NOT NULL | CURRENT_TIMESTAMP ON UPDATE | 更新时间 |



#### 3.5 用户隐私设置表（user_privacy）

| 字段名 | 类型 | 约束 | 默认值 | 说明 |
|--------|------|------|--------|------|
| user_id | BIGINT UNSIGNED | PK FK | - | 用户ID |
| profile_visible | TINYINT UNSIGNED | NOT NULL | 0 | 0-公开，1-仅粉丝，2-仅自己 |
| post_visible | TINYINT UNSIGNED | NOT NULL | 0 | 0-公开，1-仅粉丝，2-仅自己 |
| follow_visible | TINYINT UNSIGNED | NOT NULL | 0 | 0-公开，1-仅粉丝，2-仅自己 |
| allow_search | TINYINT UNSIGNED | NOT NULL | 1 | 是否允许被搜索到 |
| allow_message | TINYINT UNSIGNED | NOT NULL | 1 | 是否允许私信 |
| created_at | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | NOT NULL | CURRENT_TIMESTAMP ON UPDATE | 更新时间 |



#### 3.6 用户成就表（user_achievement）

| 字段名 | 类型 | 约束 | 默认值 | 说明 |
|--------|------|------|--------|------|
| user_id | BIGINT UNSIGNED | PK FK | - | 用户ID |
| total_posts | INT UNSIGNED | NOT NULL | 0 | 累计发帖 |
| essence_posts | INT UNSIGNED | NOT NULL | 0 | 精华帖数 |
| total_comments | INT UNSIGNED | NOT NULL | 0 | 累计评论 |
| total_likes_received | INT UNSIGNED | NOT NULL | 0 | 获赞总数 |
| total_fans | INT UNSIGNED | NOT NULL | 0 | 粉丝总数 |
| influence_score | INT UNSIGNED | NOT NULL | 0 | 影响力分数 |
| badges_json | JSON | | NULL | 勋章列表 |
| created_at | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | NOT NULL | CURRENT_TIMESTAMP ON UPDATE | 更新时间 |



### 四、内容域表设计
#### 4.1 帖子表（post）

| 字段名 | 类型 | 约束 | 默认值 | 说明 |
|--------|------|------|--------|------|
| post_id | BIGINT UNSIGNED | PK AUTO | - | 帖子ID |
| author_id | BIGINT UNSIGNED | FK NOT NULL | - | 作者ID |
| title | VARCHAR(200) | NOT NULL | - | 标题 |
| summary | VARCHAR(300) | | '' | 摘要（用于列表展示） |
| content | LONGTEXT | | NULL | 纯文本内容 |
| rich_content | LONGTEXT | | NULL | 富文本内容（HTML） |
| content_type | TINYINT UNSIGNED | NOT NULL | 1 | 1-普通，2-长文，3-投票，4-动态 |
| section_id | INT UNSIGNED | FK | NULL | 板块ID |
| zone_id | INT UNSIGNED | FK | NULL | 专区ID |
| stock_code | VARCHAR(20) | | NULL | 关联股票代码 |
| is_pinned | TINYINT UNSIGNED | NOT NULL | 0 | 0-否，1-是 |
| is_essence | TINYINT UNSIGNED | NOT NULL | 0 | 0-否，1-是 |
| is_top | TINYINT UNSIGNED | NOT NULL | 0 | 0-否，1-置顶 |
| audit_status | TINYINT UNSIGNED | NOT NULL | 0 | 0-待审，1-通过，2-驳回 |
| audit_reason | VARCHAR(200) | | '' | 驳回原因 |
| stats_likes | INT UNSIGNED | NOT NULL | 0 | 点赞数（冗余） |
| stats_favorites | INT UNSIGNED | NOT NULL | 0 | 收藏数（冗余） |
| stats_shares | INT UNSIGNED | NOT NULL | 0 | 转发数（冗余） |
| stats_comments | INT UNSIGNED | NOT NULL | 0 | 评论数（冗余） |
| stats_views | INT UNSIGNED | NOT NULL | 0 | 浏览量（冗余） |
| last_comment_at | DATETIME | | NULL | 最后评论时间 |
| published_at | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 发布时间 |
| audited_at | DATETIME | | NULL | 审核时间 |
| is_deleted | TINYINT UNSIGNED | NOT NULL | 0 | 逻辑删除 |
| created_at | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | NOT NULL | CURRENT_TIMESTAMP ON UPDATE | 更新时间 |



#### 4.2 评论表（comment）

| 字段名 | 类型 | 约束 | 默认值 | 说明 |
|--------|------|------|--------|------|
| comment_id | BIGINT UNSIGNED | PK AUTO | - | 评论ID |
| post_id | BIGINT UNSIGNED | FK NOT NULL | - | 所属帖子ID |
| parent_id | BIGINT UNSIGNED | FK | NULL | 父级评论ID（楼中楼） |
| root_id | BIGINT UNSIGNED | | NULL | 根评论ID（冗余，优化查询） |
| author_id | BIGINT UNSIGNED | FK NOT NULL | - | 作者ID |
| content | VARCHAR(2000) | NOT NULL | - | 评论内容 |
| at_user_ids | VARCHAR(500) | | '' | @的用户列表（逗号分隔） |
| stats_likes | INT UNSIGNED | NOT NULL | 0 | 点赞数 |
| stats_replies | INT UNSIGNED | NOT NULL | 0 | 回复数 |
| audit_status | TINYINT UNSIGNED | NOT NULL | 0 | 0-待审，1-通过，2-驳回 |
| published_at | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 发布时间 |
| is_deleted | TINYINT UNSIGNED | NOT NULL | 0 | 逻辑删除 |
| created_at | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | NOT NULL | CURRENT_TIMESTAMP ON UPDATE | 更新时间 |



#### 4.3 投票帖表（vote）

| 字段名 | 类型 | 约束 | 默认值 | 说明 |
|--------|------|------|--------|------|
| vote_id | BIGINT UNSIGNED | PK AUTO | - | 投票ID |
| post_id | BIGINT UNSIGNED | FK UNIQUE NOT NULL | - | 关联帖子ID |
| title | VARCHAR(200) | NOT NULL | - | 投票标题 |
| options_json | JSON | NOT NULL | - | 选项列表（含选项文本） |
| max_choices | TINYINT UNSIGNED | NOT NULL | 1 | 最大可选数 |
| is_anonymous | TINYINT UNSIGNED | NOT NULL | 0 | 是否匿名 |
| started_at | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 开始时间 |
| ended_at | DATETIME | | NULL | 结束时间 |
| total_votes | INT UNSIGNED | NOT NULL | 0 | 总投票人数 |
| status | TINYINT UNSIGNED | NOT NULL | 1 | 1-进行中，2-已结束 |
| created_at | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | NOT NULL | CURRENT_TIMESTAMP ON UPDATE | 更新时间 |



#### 4.4 投票记录表（vote_record）

| 字段名 | 类型 | 约束 | 默认值 | 说明 |
|--------|------|------|--------|------|
| record_id | BIGINT UNSIGNED | PK AUTO | - | 记录ID |
| vote_id | BIGINT UNSIGNED | FK NOT NULL | - | 投票ID |
| user_id | BIGINT UNSIGNED | FK NOT NULL | - | 用户ID |
| selected_options | VARCHAR(100) | NOT NULL | - | 选择的选项索引（逗号分隔） |
| voted_at | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 投票时间 |



#### 4.5 附件表（attachment）

| 字段名 | 类型 | 约束 | 默认值 | 说明 |
|--------|------|------|--------|------|
| attachment_id | BIGINT UNSIGNED | PK AUTO | - | 附件ID |
| post_id | BIGINT UNSIGNED | FK NOT NULL | - | 所属帖子ID |
| file_name | VARCHAR(200) | NOT NULL | - | 原始文件名 |
| file_path | VARCHAR(512) | NOT NULL | - | 存储路径 |
| file_size | INT UNSIGNED | NOT NULL | - | 文件大小（字节） |
| file_ext | VARCHAR(10) | NOT NULL | - | 文件扩展名 |
| mime_type | VARCHAR(50) | | - | MIME类型 |
| download_count | INT UNSIGNED | NOT NULL | 0 | 下载次数 |
| audit_status | TINYINT UNSIGNED | NOT NULL | 0 | 0-待审，1-通过，2-驳回 |
| uploaded_at | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 上传时间 |
| is_deleted | TINYINT UNSIGNED | NOT NULL | 0 | 逻辑删除 |



#### 4.6 盘中动态表（realtime_dynamic）

| 字段名 | 类型 | 约束 | 默认值 | 说明 |
|--------|------|------|--------|------|
| dynamic_id | BIGINT UNSIGNED | PK AUTO | - | 动态ID |
| author_id | BIGINT UNSIGNED | FK NOT NULL | - | 作者ID |
| content | VARCHAR(500) | NOT NULL | - | 动态内容 |
| image_urls | VARCHAR(2000) | | '' | 配图URL（JSON数组） |
| stock_code | VARCHAR(20) | | NULL | 关联股票代码 |
| stats_likes | INT UNSIGNED | NOT NULL | 0 | 点赞数 |
| stats_comments | INT UNSIGNED | NOT NULL | 0 | 评论数 |
| published_at | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 发布时间 |
| is_deleted | TINYINT UNSIGNED | NOT NULL | 0 | 逻辑删除 |



#### 4.7 用户互动记录表（user_interaction）

| 字段名 | 类型 | 约束 | 默认值 | 说明 |
|--------|------|------|--------|------|
| interaction_id | BIGINT UNSIGNED | PK AUTO | - | 互动ID |
| user_id | BIGINT UNSIGNED | FK NOT NULL | - | 用户ID |
| target_type | TINYINT UNSIGNED | NOT NULL | - | 1-帖子，2-评论 |
| target_id | BIGINT UNSIGNED | NOT NULL | - | 目标ID |
| action_type | TINYINT UNSIGNED | NOT NULL | - | 1-点赞，2-收藏，3-转发 |
| created_at | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 互动时间 |



### 五、板块域表设计

#### 5.1 板块表（section）

| 字段名 | 类型 | 约束 | 默认值 | 说明 |
|--------|------|------|--------|------|
| section_id | INT UNSIGNED | PK AUTO | - | 板块ID |
| parent_id | INT UNSIGNED | FK | 0 | 父板块ID（支持多级） |
| name | VARCHAR(50) | NOT NULL | - | 板块名称 |
| slug | VARCHAR(50) | UNIQUE NOT NULL | - | URL标识符 |
| icon | VARCHAR(255) | | '' | 图标URL |
| description | VARCHAR(200) | | '' | 描述 |
| sort_order | INT UNSIGNED | NOT NULL | 0 | 排序权重 |
| post_count | INT UNSIGNED | NOT NULL | 0 | 帖子总数（冗余） |
| today_post_count | INT UNSIGNED | NOT NULL | 0 | 今日帖子数（冗余） |
| status | TINYINT UNSIGNED | NOT NULL | 1 | 0-禁用，1-启用 |
| created_at | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | NOT NULL | CURRENT_TIMESTAMP ON UPDATE | 更新时间 |



#### 5.2 专区表（zone）

| 字段名 | 类型 | 约束 | 默认值 | 说明 |
|--------|------|------|--------|------|
| zone_id | INT UNSIGNED | PK AUTO | - | 专区ID |
| section_id | INT UNSIGNED | FK NOT NULL | - | 所属板块ID |
| name | VARCHAR(50) | NOT NULL | - | 专区名称 |
| slug | VARCHAR(50) | UNIQUE NOT NULL | - | URL标识符 |
| icon | VARCHAR(255) | | '' | 图标URL |
| description | VARCHAR(200) | | '' | 描述 |
| post_count | INT UNSIGNED | NOT NULL | 0 | 帖子总数（冗余） |
| status | TINYINT UNSIGNED | NOT NULL | 1 | 0-禁用，1-启用 |
| created_at | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | NOT NULL | CURRENT_TIMESTAMP ON UPDATE | 更新时间 |



### 六、社交域表设计

#### 6.1 关注关系表（follow）

| 字段名 | 类型 | 约束 | 默认值 | 说明 |
|--------|------|------|--------|------|
| follow_id | BIGINT UNSIGNED | PK AUTO | - | 关注ID |
| follower_id | BIGINT UNSIGNED | FK NOT NULL | - | 关注者ID |
| followee_id | BIGINT UNSIGNED | FK NOT NULL | - | 被关注者ID |
| is_starred | TINYINT UNSIGNED | NOT NULL | 0 | 是否星标关注 |
| followed_at | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 关注时间 |



#### 6.2 私信表（private_message）

| 字段名 | 类型 | 约束 | 默认值 | 说明 |
|--------|------|------|--------|------|
| message_id | BIGINT UNSIGNED | PK AUTO | - | 消息ID |
| session_id | VARCHAR(64) | NOT NULL | - | 会话ID（两用户组合） |
| sender_id | BIGINT UNSIGNED | FK NOT NULL | - | 发送者ID |
| receiver_id | BIGINT UNSIGNED | FK NOT NULL | - | 接收者ID |
| message_type | TINYINT UNSIGNED | NOT NULL | 1 | 1-文字，2-图片 |
| content | VARCHAR(2000) | NOT NULL | - | 消息内容 |
| is_read | TINYINT UNSIGNED | NOT NULL | 0 | 0-未读，1-已读 |
| read_at | DATETIME | | NULL | 阅读时间 |
| is_recalled | TINYINT UNSIGNED | NOT NULL | 0 | 是否撤回 |
| sent_at | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 发送时间 |
| is_deleted_sender | TINYINT UNSIGNED | NOT NULL | 0 | 发送方删除 |
| is_deleted_receiver | TINYINT UNSIGNED | NOT NULL | 0 | 接收方删除 |



#### 6.3 群组表（group）

| 字段名 | 类型 | 约束 | 默认值 | 说明 |
|--------|------|------|--------|------|
| group_id | BIGINT UNSIGNED | PK AUTO | - | 群组ID |
| owner_id | BIGINT UNSIGNED | FK NOT NULL | - | 群主ID |
| name | VARCHAR(50) | NOT NULL | - | 群组名称 |
| avatar | VARCHAR(512) | | '' | 群头像 |
| introduction | VARCHAR(200) | | '' | 群简介 |
| tags | VARCHAR(200) | | '' | 标签（逗号分隔） |
| mode | TINYINT UNSIGNED | NOT NULL | 1 | 1-公开，2-私密，3-审核 |
| member_count | INT UNSIGNED | NOT NULL | 0 | 成员数（冗余） |
| post_count | INT UNSIGNED | NOT NULL | 0 | 帖子数（冗余） |
| audit_status | TINYINT UNSIGNED | NOT NULL | 0 | 0-待审，1-通过，2-驳回 |
| status | TINYINT UNSIGNED | NOT NULL | 1 | 1-正常，2-封禁 |
| created_at | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | NOT NULL | CURRENT_TIMESTAMP ON UPDATE | 更新时间 |



#### 6.4 群组成员表（group_member）

| 字段名 | 类型 | 约束 | 默认值 | 说明 |
|--------|------|------|--------|------|
| group_id | BIGINT UNSIGNED | PK FK | - | 群组ID |
| user_id | BIGINT UNSIGNED | PK FK | - | 用户ID |
| role | TINYINT UNSIGNED | NOT NULL | 3 | 1-群主，2-管理员，3-成员 |
| join_type | TINYINT UNSIGNED | NOT NULL | - | 1-邀请，2-申请，3-公开 |
| is_muted | TINYINT UNSIGNED | NOT NULL | 0 | 是否禁言 |
| muted_until | DATETIME | | NULL | 禁言到期时间 |
| joined_at | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 加入时间 |



#### 6.5 群组帖子表（group_post）

| 字段名 | 类型 | 约束 | 默认值 | 说明 |
|--------|------|------|--------|------|
| post_id | BIGINT UNSIGNED | PK AUTO | - | 帖子ID |
| group_id | BIGINT UNSIGNED | FK NOT NULL | - | 所属群组ID |
| author_id | BIGINT UNSIGNED | FK NOT NULL | - | 作者ID |
| content | VARCHAR(2000) | NOT NULL | - | 内容 |
| attachments | JSON | | NULL | 附件列表 |
| stats_likes | INT UNSIGNED | NOT NULL | 0 | 点赞数 |
| stats_comments | INT UNSIGNED | NOT NULL | 0 | 评论数 |
| published_at | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 发布时间 |
| is_deleted | TINYINT UNSIGNED | NOT NULL | 0 | 逻辑删除 |



### 七、运营域表设计

#### 7.1 内容审核队列表（audit_queue）

| 字段名 | 类型 | 约束 | 默认值 | 说明 |
|--------|------|------|--------|------|
| audit_id | BIGINT UNSIGNED | PK AUTO | - | 审核ID |
| target_type | TINYINT UNSIGNED | NOT NULL | - | 1-帖子，2-评论，3-附件，4-群组，5-认证 |
| target_id | BIGINT UNSIGNED | NOT NULL | - | 目标ID |
| submitter_id | BIGINT UNSIGNED | NOT NULL | - | 提交人ID |
| priority | TINYINT UNSIGNED | NOT NULL | 5 | 1-最高，5-普通，9-最低 |
| audit_status | TINYINT UNSIGNED | NOT NULL | 0 | 0-待审，1-通过，2-驳回 |
| audit_result | VARCHAR(200) | | '' | 审核结果备注 |
| auditor_id | BIGINT UNSIGNED | | NULL | 审核人ID |
| submitted_at | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 提交时间 |
| audited_at | DATETIME | | NULL | 审核时间 |



#### 7.2 举报记录表（report）

| 字段名 | 类型 | 约束 | 默认值 | 说明 |
|--------|------|------|--------|------|
| report_id | BIGINT UNSIGNED | PK AUTO | - | 举报ID |
| reporter_id | BIGINT UNSIGNED | FK NOT NULL | - | 举报人ID |
| target_type | TINYINT UNSIGNED | NOT NULL | - | 1-帖子，2-评论，3-用户 |
| target_id | BIGINT UNSIGNED | NOT NULL | - | 目标ID |
| reason_type | TINYINT UNSIGNED | NOT NULL | - | 1-违规内容，2-广告，3-人身攻击，4-色情，5-其他 |
| reason_desc | VARCHAR(200) | | '' | 详细说明 |
| status | TINYINT UNSIGNED | NOT NULL | 0 | 0-待处理，1-已处理，2-驳回 |
| result | VARCHAR(200) | | '' | 处理结果 |
| handler_id | BIGINT UNSIGNED | | NULL | 处理人ID |
| reported_at | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 举报时间 |
| handled_at | DATETIME | | NULL | 处理时间 |



#### 7.3 敏感词库表（sensitive_word）

| 字段名 | 类型 | 约束 | 默认值 | 说明 |
|--------|------|------|--------|------|
| word_id | INT UNSIGNED | PK AUTO | - | 词ID |
| word | VARCHAR(50) | NOT NULL | - | 敏感词 |
| word_type | TINYINT UNSIGNED | NOT NULL | - | 1-政治，2-色情，3-赌博，4-暴力，5-广告 |
| severity | TINYINT UNSIGNED | NOT NULL | 2 | 1-警告，2-拦截，3-封号 |
| is_regex | TINYINT UNSIGNED | NOT NULL | 0 | 是否正则表达式 |
| status | TINYINT UNSIGNED | NOT NULL | 1 | 0-禁用，1-启用 |
| created_by | BIGINT UNSIGNED | | NULL | 创建人ID |
| created_at | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 创建时间 |



#### 7.4 用户处罚记录表（user_punishment）

| 字段名 | 类型 | 约束 | 默认值 | 说明 |
|--------|------|------|--------|------|
| punish_id | BIGINT UNSIGNED | PK AUTO | - | 处罚ID |
| user_id | BIGINT UNSIGNED | FK NOT NULL | - | 被处罚用户ID |
| punish_type | TINYINT UNSIGNED | NOT NULL | - | 1-警告，2-禁言，3-封号 |
| reason | VARCHAR(200) | NOT NULL | - | 处罚原因 |
| duration_days | SMALLINT UNSIGNED | NOT NULL | 0 | 处罚天数（0-永久） |
| start_at | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 开始时间 |
| end_at | DATETIME | | NULL | 结束时间 |
| operator_id | BIGINT UNSIGNED | NOT NULL | - | 操作人ID |
| created_at | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 创建时间 |



#### 7.5 用户行为记录表（user_behavior_log）（按月分表）

| 字段名 | 类型 | 约束 | 默认值 | 说明 |
|--------|------|------|--------|------|
| log_id | BIGINT UNSIGNED | PK AUTO | - | 日志ID |
| user_id | BIGINT UNSIGNED | NOT NULL | - | 用户ID |
| action_type | TINYINT UNSIGNED | NOT NULL | - | 1-浏览，2-点赞，3-评论，4-发帖，5-搜索 |
| target_type | TINYINT UNSIGNED | | NULL | 目标类型 |
| target_id | BIGINT UNSIGNED | | NULL | 目标ID |
| extra_data | JSON | | NULL | 扩展数据 |
| ip_address | VARCHAR(45) | | NULL | IP地址 |
| user_agent | VARCHAR(255) | | NULL | User Agent |
| created_at | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 创建时间 |


#### 7.6 运营操作日志表（operation_log）

| 字段名 | 类型 | 约束 | 默认值 | 说明 |
|--------|------|------|--------|------|
| log_id | BIGINT UNSIGNED | PK AUTO | - | 日志ID |
| operator_id | BIGINT UNSIGNED | NOT NULL | - | 操作人ID |
| operation | VARCHAR(50) | NOT NULL | - | 操作名称 |
| target_type | VARCHAR(50) | | NULL | 目标类型 |
| target_id | BIGINT UNSIGNED | | NULL | 目标ID |
| before_data | JSON | | NULL | 操作前数据 |
| after_data | JSON | | NULL | 操作后数据 |
| ip_address | VARCHAR(45) | | NULL | IP地址 |
| created_at | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 操作时间 |



### 八、热数据缓存设计（非关系型补充）

| 缓存Key模式 | 数据类型 | TTL | 说明 |
|-------------|----------|-----|------|
| user:{user_id} | Hash | 1小时 | 用户基本信息 |
| post:{post_id} | Hash | 30分钟 | 帖子详情 |
| post:list:hot | ZSet | 5分钟 | 热帖排行 |
| section:tree | String(JSON) | 1小时 | 板块树结构 |
| user:session:{token} | String(JSON) | 7天 | 登录会话 |

# database.init.sql
-- ============================================
-- 社区论坛系统 - 数据库初始化脚本
-- 基于 ER 图自动生成
-- ============================================

CREATE DATABASE IF NOT EXISTS community_forum
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE community_forum;

-- ============================================
-- 1. 核心用户模块
-- ============================================

CREATE TABLE user (
    user_id             BIGINT       NOT NULL AUTO_INCREMENT,
    nickname            VARCHAR(50)  NOT NULL,
    mobile              VARCHAR(20)  DEFAULT NULL,
    email               VARCHAR(100) DEFAULT NULL,
    verification_level  INT          NOT NULL DEFAULT 0,
    points              INT          NOT NULL DEFAULT 0,
    level               INT          NOT NULL DEFAULT 1,
    is_banned           TINYINT(1)   NOT NULL DEFAULT 0,
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id),
    UNIQUE KEY uk_mobile (mobile),
    UNIQUE KEY uk_email  (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_verification (
    record_id         BIGINT  NOT NULL AUTO_INCREMENT,
    user_id           BIGINT  NOT NULL,
    verification_type INT     NOT NULL,
    audit_status      INT     NOT NULL DEFAULT 0,
    created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (record_id),
    KEY idx_user_id     (user_id),
    KEY idx_audit_status (audit_status),
    CONSTRAINT fk_uv_user FOREIGN KEY (user_id) REFERENCES user (user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE risk_assessment_answer (
    answer_id     BIGINT      NOT NULL AUTO_INCREMENT,
    user_id       BIGINT      NOT NULL,
    result_level  VARCHAR(20) NOT NULL,
    complete_time DATETIME    NOT NULL,
    PRIMARY KEY (answer_id),
    UNIQUE KEY uk_user_id (user_id),
    CONSTRAINT fk_raa_user FOREIGN KEY (user_id) REFERENCES user (user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_preference (
    preference_id BIGINT       NOT NULL AUTO_INCREMENT,
    user_id       BIGINT       NOT NULL,
    focus_markets VARCHAR(255) DEFAULT NULL,
    risk_type     VARCHAR(50)  DEFAULT NULL,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (preference_id),
    UNIQUE KEY uk_user_id (user_id),
    CONSTRAINT fk_up_user FOREIGN KEY (user_id) REFERENCES user (user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE privacy_setting (
    setting_id        BIGINT  NOT NULL AUTO_INCREMENT,
    user_id           BIGINT  NOT NULL,
    profile_visibility INT    NOT NULL DEFAULT 0,
    updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (setting_id),
    UNIQUE KEY uk_user_id (user_id),
    CONSTRAINT fk_ps_user FOREIGN KEY (user_id) REFERENCES user (user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_achievement (
    achievement_id    BIGINT  NOT NULL AUTO_INCREMENT,
    user_id           BIGINT  NOT NULL,
    total_post_count  INT     NOT NULL DEFAULT 0,
    essence_post_count INT    NOT NULL DEFAULT 0,
    updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (achievement_id),
    UNIQUE KEY uk_user_id (user_id),
    CONSTRAINT fk_ua_user FOREIGN KEY (user_id) REFERENCES user (user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- 2. 板块与分区
-- ============================================

CREATE TABLE section (
    section_id   INT         NOT NULL AUTO_INCREMENT,
    section_name VARCHAR(50) NOT NULL,
    section_type INT         NOT NULL,
    sort_order   INT         NOT NULL DEFAULT 0,
    created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (section_id),
    UNIQUE KEY uk_section_name (section_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE zone (
    zone_id    INT         NOT NULL AUTO_INCREMENT,
    zone_name  VARCHAR(50) NOT NULL,
    section_id INT         NOT NULL,
    sort_order INT         NOT NULL DEFAULT 0,
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (zone_id),
    KEY idx_section_id (section_id),
    CONSTRAINT fk_zone_section FOREIGN KEY (section_id) REFERENCES section (section_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- 3. 内容模块
-- ============================================

CREATE TABLE post (
    post_id       BIGINT       NOT NULL AUTO_INCREMENT,
    author_id     BIGINT       NOT NULL,
    title         VARCHAR(200) NOT NULL,
    content_type  INT          NOT NULL,
    section_id    INT          DEFAULT NULL,
    zone_id       INT          DEFAULT NULL,
    audit_status  INT          NOT NULL DEFAULT 0,
    like_count    INT          NOT NULL DEFAULT 0,
    publish_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (post_id),
    KEY idx_author_id    (author_id),
    KEY idx_section_id   (section_id),
    KEY idx_zone_id      (zone_id),
    KEY idx_audit_status (audit_status),
    KEY idx_publish_time (publish_time),
    CONSTRAINT fk_post_author  FOREIGN KEY (author_id)  REFERENCES user (user_id)       ON DELETE CASCADE,
    CONSTRAINT fk_post_section FOREIGN KEY (section_id) REFERENCES section (section_id) ON DELETE SET NULL,
    CONSTRAINT fk_post_zone    FOREIGN KEY (zone_id)    REFERENCES zone (zone_id)       ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE comment (
    comment_id        BIGINT   NOT NULL AUTO_INCREMENT,
    post_id           BIGINT   NOT NULL,
    parent_comment_id BIGINT   DEFAULT NULL,
    author_id         BIGINT   NOT NULL,
    content           TEXT     NOT NULL,
    publish_time      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (comment_id),
    KEY idx_post_id          (post_id),
    KEY idx_parent_comment_id (parent_comment_id),
    KEY idx_author_id        (author_id),
    CONSTRAINT fk_comment_post   FOREIGN KEY (post_id)           REFERENCES post (post_id)       ON DELETE CASCADE,
    CONSTRAINT fk_comment_parent FOREIGN KEY (parent_comment_id) REFERENCES comment (comment_id) ON DELETE CASCADE,
    CONSTRAINT fk_comment_author FOREIGN KEY (author_id)         REFERENCES user (user_id)       ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE vote_post (
    vote_id    BIGINT       NOT NULL AUTO_INCREMENT,
    post_id    BIGINT       NOT NULL,
    vote_title VARCHAR(200) NOT NULL,
    end_time   DATETIME     NOT NULL,
    PRIMARY KEY (vote_id),
    UNIQUE KEY uk_post_id (post_id),
    CONSTRAINT fk_vote_post FOREIGN KEY (post_id) REFERENCES post (post_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE vote_record (
    record_id    BIGINT   NOT NULL AUTO_INCREMENT,
    vote_id      BIGINT   NOT NULL,
    user_id      BIGINT   NOT NULL,
    option_index INT      NOT NULL,
    vote_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (record_id),
    UNIQUE KEY uk_vote_user (vote_id, user_id),
    KEY idx_user_id (user_id),
    CONSTRAINT fk_vr_vote FOREIGN KEY (vote_id) REFERENCES vote_post (vote_id) ON DELETE CASCADE,
    CONSTRAINT fk_vr_user FOREIGN KEY (user_id) REFERENCES user (user_id)      ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE attachment (
    attachment_id BIGINT       NOT NULL AUTO_INCREMENT,
    post_id       BIGINT       NOT NULL,
    file_name     VARCHAR(255) NOT NULL,
    file_type     INT          NOT NULL,
    audit_status  INT          NOT NULL DEFAULT 0,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (attachment_id),
    KEY idx_post_id      (post_id),
    KEY idx_audit_status (audit_status),
    CONSTRAINT fk_att_post FOREIGN KEY (post_id) REFERENCES post (post_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE realtime_dynamic (
    dynamic_id   BIGINT   NOT NULL AUTO_INCREMENT,
    author_id    BIGINT   NOT NULL,
    content      TEXT     NOT NULL,
    publish_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (dynamic_id),
    KEY idx_author_id    (author_id),
    KEY idx_publish_time (publish_time),
    CONSTRAINT fk_rd_author FOREIGN KEY (author_id) REFERENCES user (user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- 4. 社交模块
-- ============================================

CREATE TABLE follow (
    relation_id BIGINT    NOT NULL AUTO_INCREMENT,
    follower_id BIGINT    NOT NULL,
    followee_id BIGINT    NOT NULL,
    is_starred  TINYINT(1) NOT NULL DEFAULT 0,
    created_at  DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (relation_id),
    UNIQUE KEY uk_follower_followee (follower_id, followee_id),
    KEY idx_followee_id (followee_id),
    CONSTRAINT fk_follow_follower FOREIGN KEY (follower_id) REFERENCES user (user_id) ON DELETE CASCADE,
    CONSTRAINT fk_follow_followee FOREIGN KEY (followee_id) REFERENCES user (user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE private_message (
    message_id  BIGINT    NOT NULL AUTO_INCREMENT,
    sender_id   BIGINT    NOT NULL,
    receiver_id BIGINT    NOT NULL,
    content     TEXT      NOT NULL,
    is_read     TINYINT(1) NOT NULL DEFAULT 0,
    send_time   DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (message_id),
    KEY idx_sender_id      (sender_id),
    KEY idx_receiver_id    (receiver_id),
    KEY idx_receiver_read  (receiver_id, is_read),
    CONSTRAINT fk_pm_sender   FOREIGN KEY (sender_id)   REFERENCES user (user_id) ON DELETE CASCADE,
    CONSTRAINT fk_pm_receiver FOREIGN KEY (receiver_id) REFERENCES user (user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE group_info (
    group_id   BIGINT       NOT NULL AUTO_INCREMENT,
    owner_id   BIGINT       NOT NULL,
    group_name VARCHAR(100) NOT NULL,
    mode       INT          NOT NULL DEFAULT 0,
    status     INT          NOT NULL DEFAULT 1,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (group_id),
    KEY idx_owner_id (owner_id),
    CONSTRAINT fk_group_owner FOREIGN KEY (owner_id) REFERENCES user (user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE group_member (
    member_id BIGINT   NOT NULL AUTO_INCREMENT,
    group_id  BIGINT   NOT NULL,
    user_id   BIGINT   NOT NULL,
    role      INT      NOT NULL DEFAULT 0,
    joined_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (member_id),
    UNIQUE KEY uk_group_user (group_id, user_id),
    KEY idx_user_id (user_id),
    CONSTRAINT fk_gm_group FOREIGN KEY (group_id) REFERENCES group_info (group_id) ON DELETE CASCADE,
    CONSTRAINT fk_gm_user  FOREIGN KEY (user_id)  REFERENCES user (user_id)      ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE group_post (
    group_post_id BIGINT   NOT NULL AUTO_INCREMENT,
    group_id      BIGINT   NOT NULL,
    author_id     BIGINT   NOT NULL,
    content       TEXT     NOT NULL,
    publish_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (group_post_id),
    KEY idx_group_id  (group_id),
    KEY idx_author_id (author_id),
    CONSTRAINT fk_gp_group  FOREIGN KEY (group_id)  REFERENCES group_info (group_id) ON DELETE CASCADE,
    CONSTRAINT fk_gp_author FOREIGN KEY (author_id) REFERENCES user (user_id)       ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- 5. 运营管理模块
-- ============================================

CREATE TABLE audit_queue (
    audit_item_id BIGINT   NOT NULL AUTO_INCREMENT,
    content_type  INT      NOT NULL,
    content_id    BIGINT   NOT NULL,
    audit_status  INT      NOT NULL DEFAULT 0,
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    audited_at    DATETIME DEFAULT NULL,
    PRIMARY KEY (audit_item_id),
    KEY idx_content_type_status (content_type, audit_status),
    KEY idx_content_id          (content_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE report (
    report_id   BIGINT   NOT NULL AUTO_INCREMENT,
    reporter_id BIGINT   NOT NULL,
    target_type INT      NOT NULL,
    target_id   BIGINT   NOT NULL,
    status      INT      NOT NULL DEFAULT 0,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (report_id),
    KEY idx_reporter_id (reporter_id),
    KEY idx_target      (target_type, target_id),
    KEY idx_status      (status),
    CONSTRAINT fk_report_reporter FOREIGN KEY (reporter_id) REFERENCES user (user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_punishment (
    punishment_id  BIGINT   NOT NULL AUTO_INCREMENT,
    user_id        BIGINT   NOT NULL,
    punishment_type INT    NOT NULL,
    duration_days  INT      NOT NULL DEFAULT 0,
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expire_at      DATETIME DEFAULT NULL,
    PRIMARY KEY (punishment_id),
    KEY idx_user_id   (user_id),
    KEY idx_expire_at (expire_at),
    CONSTRAINT fk_up_user FOREIGN KEY (user_id) REFERENCES user (user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_behavior (
    behavior_id   BIGINT   NOT NULL AUTO_INCREMENT,
    user_id       BIGINT   NOT NULL,
    behavior_type INT      NOT NULL,
    target_id     BIGINT   DEFAULT NULL,
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (behavior_id),
    KEY idx_user_id             (user_id),
    KEY idx_user_behavior_time  (user_id, behavior_type, created_at),
    CONSTRAINT fk_ub_user FOREIGN KEY (user_id) REFERENCES user (user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
