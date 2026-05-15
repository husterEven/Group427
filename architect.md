股票基金投资论坛 - 架构设计与技术选型
一、技术选型总览
1.1 技术栈概览
层级	技术选型	说明
前端	Vue 3 + TypeScript + Vite	现代化响应式界面
移动端	Uni-app	跨端小程序/H5/App
后端	Spring Boot 3 + MyBatis-Plus	稳定高效的企业级框架
缓存	Redis	会话、热点数据、排行榜
数据库	MySQL 8.0	主数据存储
搜索引擎	Elasticsearch	全文搜索
消息队列	RocketMQ / RabbitMQ	异步处理、通知
对象存储	MinIO / 阿里云OSS	图片、附件存储
容器化	Docker + Docker Compose	开发部署环境
二、后端架构设计
2.1 整体架构图
text
┌─────────────────────────────────────────────────────────────┐
│                        客户端层                              │
│  Web端(Vue3) │ 小程序(Uni-app) │ H5 │ 管理后台               │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      网关层 (Spring Cloud Gateway)           │
│              认证鉴权 │ 限流熔断 │ 日志记录                    │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                      业务服务层                              │
│ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐│
│ │用户服务 │ │内容服务 │ │社交服务 │ │搜索服务 │ │管理服务 ││
│ └─────────┘ └─────────┘ └─────────┘ └─────────┘ └─────────┘│
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                      中间件层                                │
│  Redis │ ES │ RocketMQ │ MinIO │ WebSocket                   │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                      数据层                                  │
│         MySQL(主从) │ 定时任务(XXL-JOB)                       │
└─────────────────────────────────────────────────────────────┘
2.2 模块划分
text
stock-forum-backend/
├── forum-common/           # 公共模块
│   ├── utils/              # 工具类
│   ├── exception/          # 全局异常
│   └── constants/          # 常量定义
├── forum-api/              # API接口定义
├── forum-user/             # 用户系统
│   ├── controller/
│   ├── service/
│   ├── mapper/
│   └── dto/
├── forum-content/          # 内容系统
│   ├── post/               # 帖子模块
│   ├── comment/            # 评论模块
│   ├──板块管理/            # 板块CRUD
│   └── attachment/         # 附件管理
├── forum-social/           # 社交系统
│   ├── follow/             # 关注粉丝
│   ├── group/              # 群组功能
│   └── message/            # 私信系统
├── forum-search/           # 搜索系统(ES)
├── forum-admin/            # 管理运营系统
│   ├── audit/              # 审核
│   └── statistics/         # 数据分析
└── forum-gateway/          # 网关
三、核心数据库设计
3.1 用户相关表
sql
-- 用户主表
CREATE TABLE `user` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `phone` VARCHAR(20) COMMENT '手机号',
    `email` VARCHAR(100) COMMENT '邮箱',
    `password` VARCHAR(255) NOT NULL,
    `real_name` VARCHAR(50) COMMENT '真实姓名',
    `id_card` VARCHAR(18) COMMENT '身份证号',
    `certification_level` TINYINT DEFAULT 0 COMMENT '0-普通 1-基础 2-实名 3-专业',
    `certification_status` TINYINT DEFAULT 0 COMMENT '0-未认证 1-审核中 2-已认证 3-拒绝',
    `professional_certs` JSON COMMENT '专业认证材料',
    `risk_assessment` JSON COMMENT '风险评估问卷结果',
    `create_time` DATETIME,
    `update_time` DATETIME
);

-- 用户扩展信息表
CREATE TABLE `user_profile` (
    `user_id` BIGINT PRIMARY KEY,
    `nickname` VARCHAR(50),
    `avatar` VARCHAR(255),
    `bio` VARCHAR(500),
    `investment_tags` JSON COMMENT '投资经验标签',
    `follow_markets` JSON COMMENT '关注的市场',
    `risk_preference` VARCHAR(20) COMMENT '保守/稳健/进取',
    `influence_score` INT DEFAULT 0 COMMENT '影响力值',
    `privacy_settings` JSON COMMENT '隐私设置'
);

-- 用户成就表
CREATE TABLE `user_achievement` (
    `id` BIGINT PRIMARY KEY,
    `user_id` BIGINT,
    `achievement_type` VARCHAR(50),
    `level` INT,
    `earned_time` DATETIME
);
3.2 内容相关表
sql
-- 板块表（支持动态增删改）
CREATE TABLE `forum_section` (
    `id` BIGINT PRIMARY KEY,
    `name` VARCHAR(50) NOT NULL,
    `parent_id` BIGINT DEFAULT 0,
    `section_type` VARCHAR(30) COMMENT 'market/theme/company/qa',
    `description` VARCHAR(200),
    `sort_order` INT,
    `status` TINYINT DEFAULT 1,
    `create_by` BIGINT,
    `create_time` DATETIME
);

-- 帖子表
CREATE TABLE `post` (
    `id` BIGINT PRIMARY KEY,
    `section_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `post_type` VARCHAR(20) COMMENT 'normal/long/vote/real_time',
    `title` VARCHAR(200),
    `content` LONGTEXT,
    `content_rich` LONGTEXT COMMENT '富文本内容',
    `vote_options` JSON COMMENT '投票选项',
    `view_count` INT DEFAULT 0,
    `like_count` INT DEFAULT 0,
    `comment_count` INT DEFAULT 0,
    `collect_count` INT DEFAULT 0,
    `is_essence` TINYINT DEFAULT 0,
    `is_top` TINYINT DEFAULT 0,
    `status` TINYINT DEFAULT 1 COMMENT '1-正常 2-审核中 3-已删除',
    `create_time` DATETIME,
    `update_time` DATETIME,
    INDEX idx_section_id (section_id),
    INDEX idx_user_id (user_id),
    FULLTEXT idx_title_content (title, content)
);

-- 评论表（支持楼中楼）
CREATE TABLE `comment` (
    `id` BIGINT PRIMARY KEY,
    `post_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `parent_id` BIGINT DEFAULT 0 COMMENT '父评论ID，0表示一级评论',
    `reply_to_user_id` BIGINT COMMENT '@回复的用户',
    `content` TEXT,
    `like_count` INT DEFAULT 0,
    `status` TINYINT DEFAULT 1,
    `create_time` DATETIME,
    INDEX idx_post_id (post_id),
    INDEX idx_parent_id (parent_id)
);

-- 附件审核表
CREATE TABLE `attachment` (
    `id` BIGINT PRIMARY KEY,
    `user_id` BIGINT,
    `post_id` BIGINT,
    `file_name` VARCHAR(200),
    `file_url` VARCHAR(500),
    `file_size` INT,
    `file_type` VARCHAR(50),
    `audit_status` TINYINT DEFAULT 0,
    `audit_result` VARCHAR(200),
    `create_time` DATETIME
);
3.3 社交关系表
sql
-- 关注关系表
CREATE TABLE `user_follow` (
    `id` BIGINT PRIMARY KEY,
    `user_id` BIGINT NOT NULL COMMENT '关注者',
    `follow_user_id` BIGINT NOT NULL COMMENT '被关注者',
    `is_star` TINYINT DEFAULT 0 COMMENT '是否特别关注',
    `create_time` DATETIME,
    UNIQUE KEY uk_user_follow (user_id, follow_user_id)
);

-- 群组表
CREATE TABLE `group_info` (
    `id` BIGINT PRIMARY KEY,
    `name` VARCHAR(50),
    `avatar` VARCHAR(255),
    `description` VARCHAR(500),
    `visibility` TINYINT DEFAULT 1 COMMENT '1-公开 2-私密 3-审核加入',
    `owner_id` BIGINT,
    `member_count` INT DEFAULT 1,
    `create_time` DATETIME
);

-- 群组成员表
CREATE TABLE `group_member` (
    `id` BIGINT PRIMARY KEY,
    `group_id` BIGINT,
    `user_id` BIGINT,
    `role` TINYINT DEFAULT 0 COMMENT '0-成员 1-管理员 2-群主',
    `join_time` DATETIME
);
3.4 管理运营表
sql
-- 违规记录表
CREATE TABLE `violation_record` (
    `id` BIGINT PRIMARY KEY,
    `user_id` BIGINT,
    `content_type` VARCHAR(20) COMMENT 'post/comment',
    `content_id` BIGINT,
    `violation_type` VARCHAR(50),
    `action` VARCHAR(20) COMMENT 'warn/mute/ban',
    `action_duration` INT COMMENT '禁言时长(小时)',
    `handler_id` BIGINT,
    `create_time` DATETIME
);

-- 操作日志表
CREATE TABLE `operation_log` (
    `id` BIGINT PRIMARY KEY,
    `user_id` BIGINT,
    `operation` VARCHAR(50),
    `target_type` VARCHAR(30),
    `target_id` BIGINT,
    `ip_address` VARCHAR(45),
    `create_time` DATETIME,
    INDEX idx_user_id (user_id)
);
四、Redis缓存设计
4.1 缓存Key设计
java
// 用户会话
"session:token:{token}"           // 用户登录信息，过期7天

// 帖子相关
"post:detail:{postId}"              // 帖子详情，过期30分钟
"post:like:{userId}:{postId}"       // 用户点赞记录，Set结构
"post:hot:section:{sectionId}"      // 板块热门帖子，ZSet，按分数排序

// 排行榜
"rank:daily:like"                   // 日点赞榜
"rank:weekly:hot"                   // 周热帖榜
"rank:influence"                    // 影响力榜

// 用户相关
"user:info:{userId}"                // 用户基本信息
"user:profile:{userId}"             // 用户扩展信息
"user:followers:{userId}"           // 粉丝列表（分页缓存）
"user:following:{userId}"           // 关注列表

// 计数统计
"counter:post:view:{postId}"        // 帖子浏览量
"counter:user:influence:{userId}"   // 用户影响力值

// 限流
"rate:limit:{userId}:{action}"      // 用户操作频率限制
4.2 缓存更新策略
数据类型	过期策略	更新方式
帖子详情	30分钟 + 访问触发延长	写时删除，读时重建
排行榜	每天/每周定时重建	定时任务刷新
用户信息	1小时	写时更新
计数器	不自动过期	每次操作递增，定时持久化到DB
五、搜索设计(Elasticsearch)
5.1 索引映射
json
{
  "post_index": {
    "mappings": {
      "properties": {
        "id": {"type": "long"},
        "title": {"type": "text", "analyzer": "ik_max_word"},
        "content": {"type": "text", "analyzer": "ik_max_word"},
        "section_id": {"type": "integer"},
        "user_id": {"type": "long"},
        "user_nickname": {"type": "keyword"},
        "create_time": {"type": "date"},
        "like_count": {"type": "integer"},
        "comment_count": {"type": "integer"},
        "is_essence": {"type": "boolean"},
        "status": {"type": "byte"}
      }
    }
  },
  "user_index": {
    "mappings": {
      "properties": {
        "id": {"type": "long"},
        "nickname": {"type": "text", "analyzer": "ik_smart"},
        "bio": {"type": "text", "analyzer": "ik_smart"},
        "investment_tags": {"type": "keyword"}
      }
    }
  }
}
5.2 搜索流程
text
用户搜索 → 调用搜索服务 → ES检索 → 聚合高亮 → 
从Redis/MySQL补充详情 → 返回结果
六、关键接口设计
6.1 RESTful API示例
java
// 帖子相关接口
@RestController
@RequestMapping("/api/v1/posts")
public class PostController {
    
    // 发布帖子
    @PostMapping
    public Result<Long> createPost(@RequestBody @Valid PostCreateDTO dto) {
        // 权限校验、敏感词过滤、异步审核
    }
    
    // 获取帖子详情
    @GetMapping("/{id}")
    public Result<PostDetailVO> getPost(@PathVariable Long id) {
        // 增加浏览量、缓存读取、关联数据聚合
    }
    
    // 获取板块帖子列表（支持分页、排序）
    @GetMapping("/section/{sectionId}")
    public Result<PageResult<PostListVO>> getPostsBySection(
        @PathVariable Long sectionId,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "hot") String orderBy) {
        // 热点帖走Redis ZSet，普通帖走MySQL分页
    }
    
    // 点赞
    @PostMapping("/{id}/like")
    public Result<Void> likePost(@PathVariable Long id) {
        // Redis Set记录+计数，异步同步到MySQL
    }
}

// 板块管理接口
@RestController
@RequestMapping("/api/v1/admin/sections")
public class SectionAdminController {
    
    // 动态添加板块
    @PostMapping
    public Result<Void> addSection(@RequestBody @Valid SectionCreateDTO dto) {
        // 管理员权限校验、板块创建、清除板块缓存
    }
    
    // 修改板块
    @PutMapping("/{id}")
    public Result<Void> updateSection(@PathVariable Long id, @RequestBody SectionUpdateDTO dto) {
        // 权限校验、更新数据库、清除相关缓存
    }
    
    // 删除板块（软删除）
    @DeleteMapping("/{id}")
    public Result<Void> deleteSection(@PathVariable Long id) {
        // 检查板块下是否有帖子、软删除或转移帖子
    }
}
6.2 WebSocket实时消息
java
@ServerEndpoint("/ws/{userId}")
public class WebSocketServer {
    
    // 实时讨论消息
    @OnMessage
    public void onMessage(String message, Session session) {
        // 解析消息 -> 发送到指定房间（板块）
    }
    
    // 通知推送（点赞、评论、关注）
    public void sendNotification(Long userId, NotificationDTO notification) {
        // 向指定用户推送实时通知
    }
}
七、关键功能实现思路
7.1 审核流程
text
用户发布内容 → 敏感词自动过滤(AC自动机) → 
通过 → 发布成功，更新ES
包含敏感词 → 进入人工审核队列 → 
审核通过 → 发布
审核不通过 → 退回+通知用户
7.2 热榜算法
java
// 综合热度 = (点赞数 * 2 + 评论数 * 3 + 收藏数 * 1.5) / 
//          (发布时间距现在的小时数 + 2)^1.2
public double calculateHotScore(int likeCount, int commentCount, 
                                 int collectCount, LocalDateTime createTime) {
    long hours = ChronoUnit.HOURS.between(createTime, LocalDateTime.now());
    double interactionScore = likeCount * 2.0 + commentCount * 3.0 + collectCount * 1.5;
    double timeDecay = Math.pow(hours + 2, 1.2);
    return interactionScore / timeDecay;
}
7.3 投资者适当性评估（问卷积分制）
java
public int evaluateRiskLevel(Map<String, Object> answers) {
    // 题目权重配置
    int totalScore = 0;
    // 问题1: 投资经验（0-20分）
    // 问题2: 可承受亏损（0-30分）
    // 问题3: 投资期限（0-20分）
    // 问题4: 收入稳定性（0-30分）
    
    // 总分<40: 保守型 C1
    // 40-60: 稳健型 C2
    // 60-80: 平衡型 C3
    // 80-100: 进取型 C4
    return totalScore;
}
八、部署架构
8.1 Docker Compose配置
yaml
version: '3.8'
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: root123
    volumes:
      - mysql_data:/var/lib/mysql
      
  redis:
    image: redis:7-alpine
    command: redis-server --appendonly yes
    
  elasticsearch:
    image: elasticsearch:8.10.0
    environment:
      - discovery.type=single-node
      - "ES_JAVA_OPTS=-Xms512m -Xmx512m"
      
  minio:
    image: minio/minio
    command: server /data --console-address ":9001"
    
  rocketmq:
    image: apache/rocketmq:5.0.0
    
  app:
    build: .
    ports:
      - "8080:8080"
    depends_on:
      - mysql
      - redis
      - elasticsearch
8.2 开发环境要求
工具	版本	用途
JDK	17+	后端运行环境
Maven	3.8+	项目构建
Node.js	18+	前端构建
MySQL	8.0+	数据库
Redis	7.0+	缓存
IDE	IDEA / VS Code	开发工具
九、项目结构（前端）
text
stock-forum-frontend/
├── public/
├── src/
│   ├── api/               # API接口
│   │   ├── user.ts
│   │   ├── post.ts
│   │   └── section.ts
│   ├── assets/            # 静态资源
│   ├── components/        # 公共组件
│   │   ├── PostCard.vue
│   │   ├── CommentTree.vue
│   │   └── RichEditor.vue
│   ├── views/             # 页面视图
│   │   ├── home/          # 首页
│   │   ├── forum/         # 论坛板块
│   │   ├── post/          # 帖子详情
│   │   ├── user/          # 用户中心
│   │   └── admin/         # 管理后台
│   ├── router/            # 路由配置
│   ├── stores/            # Pinia状态管理
│   ├── utils/             # 工具函数
│   └── main.ts
├── package.json
└── vite.config.ts
十、开发建议与注意事项
10.1 优先级划分
优先级	功能模块	说明
P0	用户注册登录、发帖评论、板块浏览	MVP核心功能
P1	点赞收藏、关注粉丝、搜索	社交互动
P2	私信系统、群组功能、排行榜	增强功能
P3	专业认证、风险评估、数据分析	进阶功能
10.2 技术难点及应对
难点	解决方案
高并发点赞	Redis计数 + 定时批量落库
全文搜索	Elasticsearch + IK分词
楼中楼评论	递归查询或使用闭包表模型
实时讨论	WebSocket + 消息队列
敏感词过滤	AC自动机 + 前缀树
10.3 安全考虑
用户密码使用BCrypt加密存储

敏感接口限流（发帖、评论限制频率）

XSS过滤、SQL防注入

身份证/人脸信息脱敏存储

附件上传类型和大小限制
# 股票基金投资论坛 - 架构设计（纯设计，无代码）
一、技术选型总览
1.1 技术栈决策
层级	技术选型	选型理由
前端	Vue 3 + TypeScript	组合式API适合复杂交互，TypeScript保证类型安全
移动端	Uni-app	一套代码覆盖小程序、H5、App，成本低
后端	Spring Boot 3 + MyBatis-Plus	成熟稳定，生态丰富，适合快速开发
缓存	Redis 7.x	高性能，数据结构丰富，支持排行榜、会话、计数器
数据库	MySQL 8.0	关系型数据强一致，支持事务，论坛核心数据存储
搜索引擎	Elasticsearch 8.x	全文检索能力强，支持股票代码、用户、帖子混合搜索
消息队列	RocketMQ	高吞吐，支持事务消息，适合异步审核、通知推送
对象存储	MinIO / 阿里云OSS	存储头像、附件、图片，MinIO适合私有化部署
网关	Spring Cloud Gateway	统一认证、限流、路由，微服务入口
容器化	Docker Compose	课程项目规模，Compose足够管理多容器
二、系统架构设计
2.1 整体架构分层
text
┌─────────────────────────────────────────────────────────────────┐
│                        客户端层                                  │
│    Web端    │   微信小程序   │   H5移动端   │   管理后台         │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                        接入层                                    │
│   Nginx（反向代理+负载均衡+静态资源）                              │
│   Spring Cloud Gateway（路由+鉴权+限流+日志）                     │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                        业务服务层                                │
│ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌────────┐ │
│ │ 用户服务  │ │ 内容服务  │ │ 社交服务  │ │ 搜索服务  │ │管理服务│ │
│ │ -注册认证 │ │ -帖子管理 │ │ -关注粉丝 │ │ -全文检索 │ │ -审核  │ │
│ │ -资料管理 │ │ -评论系统 │ │ -群组功能 │ │ -高级筛选 │ │ -统计  │ │
│ │ -成就系统 │ │ -板块管理 │ │ -私信系统 │ │ -自动补全 │ │ -处罚  │ │
│ └──────────┘ └──────────┘ └──────────┘ └──────────┘ └────────┘ │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                        中间件层                                  │
│  ┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐              │
│  │Redis│ │  ES │ │Rocket│ │MinIO│ │ZK  │ │WebS │              │
│  │缓存 │ │搜索 │ │ MQ   │ │存储 │ │协调 │ │ocket│              │
│  └─────┘ └─────┘ └─────┘ └─────┘ └─────┘ └─────┘              │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                        数据层                                    │
│  ┌─────────────────────────┐  ┌─────────────────────────────┐   │
│  │      MySQL主库           │  │      MySQL从库（读）         │   │
│  │   - 用户表/帖子表/评论表  │  │   - 报表查询/分析查询         │   │
│  │   - 社交关系/群组/附件    │  │                             │   │
│  └─────────────────────────┘  └─────────────────────────────┘   │
│                                                                  │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │              定时任务（XXL-JOB）                          │    │
│  │   - 热榜计算  - 过期数据处理  - 统计报表生成              │    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
2.2 微服务拆分原则
服务名	核心职责	独立存储	对外接口数
user-service	用户注册、认证、资料、成就	user_db	~15个
content-service	帖子、评论、板块、附件	content_db	~20个
social-service	关注、粉丝、私信、群组	social_db	~12个
search-service	ES索引管理、全文搜索	ES集群	~8个
admin-service	审核、用户管理、统计分析	admin_db	~15个
gateway-service	路由、鉴权、限流	无	内部
服务间通信：同步调用走Feign（HTTP），异步通知走RocketMQ

2.3 服务间调用关系
text
gateway → user-service
gateway → content-service
gateway → social-service
gateway → search-service
gateway → admin-service

content-service → user-service（获取发帖人信息）
content-service → social-service（查询用户是否点赞）
social-service → user-service（获取用户信息）
search-service → content-service（同步索引数据）
admin-service → 所有服务（审核、统计）
三、数据架构设计
3.1 数据库设计原则
原则	具体做法
分库	按业务域分库：user_db、content_db、social_db、admin_db
读写分离	主库写入，从库查询（报表、后台统计走从库）
冷热分离	3个月内的帖子热数据在主表，历史帖子归档到历史表
软删除	所有表增加status字段，删除时标记而非物理删除
冗余设计	帖子表冗余发帖人昵称、头像，避免连表查询
3.2 核心数据模型关系
text
用户(User) ──1:N── 帖子(Post)
用户(User) ──1:N── 评论(Comment)
用户(User) ──M:N── 用户(User)  通过关注表
用户(User) ──M:N── 帖子(Post)  通过点赞/收藏表

板块(Section) ──1:N── 帖子(Post)
帖子(Post) ──1:N── 评论(Comment)
评论(Comment) ──1:N── 评论(Comment) 楼中楼，自关联

群组(Group) ──M:N── 用户(User) 通过群组成员表
3.3 索引设计策略
场景	索引类型	字段	说明
板块帖子列表	复合索引	(section_id, create_time)	按板块和时间排序
用户帖子列表	复合索引	(user_id, create_time)	查询用户的发帖记录
评论列表	复合索引	(post_id, parent_id, create_time)	查询帖子的评论树
关注列表	唯一索引	(user_id, follow_user_id)	防止重复关注
全文搜索	全文索引	(title, content)	MySQL的FULLTEXT，或迁移到ES
3.4 数据量预估（课程项目规模）
表名	预估数据量	增长速率	存储方案
user	1万~10万	慢	MySQL单表
post	10万~100万	中等	按月分表或归档
comment	50万~500万	快	按post_id哈希分表
follow	10万~50万	中等	MySQL单表
group	1000~5000	慢	MySQL单表
operation_log	100万+	快	独立日志库，定期清理
四、缓存架构设计
4.1 多级缓存策略
text
请求进入 → L1本地缓存(Caffeine) → L2 Redis缓存 → L3 数据库
                ↓                       ↓
           热点数据(板块列表)      用户会话、帖子详情
           过期时间30秒           过期时间30分钟
4.2 缓存分类与策略
缓存类别	存储位置	过期策略	更新模式	典型数据
会话缓存	Redis	7天滑动	每次访问刷新	用户登录Token、权限
热点对象	Redis	30分钟+LRU	Cache Aside	帖子详情、用户信息
排行榜	Redis ZSet	定时重建	定时任务	热帖榜、影响力榜
计数缓存	Redis	永不过期	Write Behind	浏览量、点赞数
列表缓存	Redis + Caffeine	5分钟	Cache Aside	板块列表、热门帖子
防重复缓存	Redis	1秒~1分钟	写入时检查	防重复提交、限流
4.3 缓存穿透/雪崩/击穿应对
问题	应对措施
缓存穿透	布隆过滤器（过滤不存在的postId）、空值缓存（过期时间短）
缓存雪崩	不同缓存设置随机过期时间（±10%）、高可用Redis集群
缓存击穿	热点数据互斥锁（SETNX）、逻辑过期（主动更新）
缓存一致	延迟双删（写操作删两次缓存）、消息队列异步更新
五、搜索架构设计
5.1 ES索引规划
索引名称	分片数	副本数	存储字段	主要用途
post_index	3	1	id, title, content, user_id, nickname, create_time, like_count	帖子全文搜索
user_index	1	1	id, nickname, bio, investment_tags	用户搜索
stock_index	1	1	code, name, industry	股票代码联想
5.2 数据同步方案
text
MySQL binlog → Canal → RocketMQ → ES消费 → 更新索引

实时场景：发帖、编辑、删除 → 同步更新ES
批量场景：历史数据导入 → 批量写入ES
5.3 搜索功能设计
搜索类型	实现方式	权重设计
标题匹配	match query + boost	权重=3
内容匹配	match query	权重=1
用户昵称	prefix + wildcard	前缀匹配优先
股票代码	term精确匹配	最优先
板块筛选	term filter	精准过滤
搜索排序公式：

text
最终分数 = ES相关度分 * 0.4 + 热度分(like/comment) * 0.3 + 时间衰减分 * 0.3
六、消息队列设计
6.1 Topic规划
Topic名称	生产者	消费者	用途
post_publish	content-service	search-service, admin-service	帖子发布后同步ES、进入审核
comment_create	content-service	social-service	评论后触发通知
like_action	content-service	social-service	点赞后更新计数、推送
audit_result	admin-service	content-service, user-service	审核结果通知
notification	各服务	notification-worker	统一推送通知
6.2 消息可靠性保障
阶段	保障措施
生产端	同步发送 + 失败重试（3次） + 消息落库
Broker	主从同步刷盘 + 消息持久化
消费端	手动确认 + 幂等处理（消息去重） + 死信队列
七、认证与安全架构
7.1 多级认证体系
text
┌─────────────────────────────────────────────────────┐
│                   认证等级                           │
├─────────────┬─────────────┬─────────────┬──────────┤
│  基础认证    │   实名认证    │  专业认证    │ 适当性评估 │
├─────────────┼─────────────┼─────────────┼──────────┤
│ 手机/邮箱验证 │ 身份证+人脸  │ 上传资格证   │ 问卷打分  │
├─────────────┼─────────────┼─────────────┼──────────┤
│    可发帖    │   加V标识    │  专业区权限  │ 风险匹配  │
└─────────────┴─────────────┴─────────────┴──────────┘
7.2 权限模型
采用 RBAC 模型：

text
用户(User) → 角色(Role) → 权限(Permission)
                    ↓
              数据权限（板块访问、帖子操作范围）
预定义角色：

普通用户：基础操作（浏览、发帖、评论）

实名用户：实名专区访问、发帖权重提升

专业用户：专业区发帖、加V标识

版主：板块管理、帖子置顶/加精、删除违规

管理员：用户管理、全局配置、审核

超级管理员：所有权限

7.3 安全防护
威胁类型	防护措施
SQL注入	MyBatis参数化查询、防注入过滤器
XSS	输入过滤（HTML转义）、输出编码、CSP策略
CSRF	Token验证、SameSite Cookie
暴力破解	验证码、登录限流（5次/分钟）
DDoS	Nginx限流、API网关令牌桶
数据泄露	敏感数据加密存储、传输HTTPS
八、高可用与运维架构
8.1 部署架构
text
                    ┌─────────────┐
                    │   DNS/CDN    │
                    └──────┬──────┘
                           │
                    ┌──────▼──────┐
                    │   Nginx     │  (主备+Keepalive)
                    │   负载均衡   │
                    └──────┬──────┘
                           │
            ┌──────────────┼──────────────┐
            │              │              │
       ┌────▼────┐    ┌────▼────┐    ┌────▼────┐
       │Gateway1 │    │Gateway2 │    │Gateway3 │
       │(8080)   │    │(8080)   │    │(8080)   │
       └────┬────┘    └────┬────┘    └────┬────┘
            │              │              │
            └──────────────┼──────────────┘
                           │
       ┌───────────────────┼───────────────────┐
       │                   │                   │
   服务节点1            服务节点2            服务节点3
   (业务服务)          (业务服务)          (业务服务)
8.2 服务高可用
组件	高可用方案	故障恢复
网关	多实例 + Nginx负载	自动剔除故障节点
业务服务	多副本部署（至少2个）	容器自动重启
MySQL	主从复制 + MHA/MGR	手动/自动切换
Redis	哨兵模式（1主2从3哨兵）	自动故障转移
ES	多节点集群	自动副本分配
MQ	主从 + 自动切换	Broker自动选主
8.3 监控告警
监控维度	工具	告警阈值
应用性能	SkyWalking	接口RT>1s报警
系统资源	Prometheus+NodeExporter	CPU>80%、内存>85%
数据库	Prometheus+MySQLExporter	慢查询>100条/分钟
日志	ELK	Error日志触发
业务指标	自研埋点	发帖量异常波动
8.4 容量规划（课程项目参考）
资源	初始配置	扩展方式
应用容器	2C4G × 3台	水平扩展
MySQL	4C8G（主） + 2C4G（从）	垂直/水平分片
Redis	2C4G × 1主2从	集群模式
ES	2C4G × 3节点	增加节点
九、模块交互时序图
9.1 发帖流程
text
用户 → 网关 → 内容服务 → 审核服务 → 消息队列 → 搜索服务/通知服务
                ↓            ↓
              缓存 ← MySQL  异步处理

时序：
1. 用户提交帖子 → 网关鉴权 → 内容服务
2. 内容服务执行：敏感词过滤 → 写入MySQL → 删除板块列表缓存
3. 同步返回：帖子发布成功，进入审核状态
4. 异步处理：发送消息到MQ → 审核服务消费 → 
   - 通过：更新状态，发送到ES，推送通知
   - 不通过：退回，通知用户
9.2 热榜更新流程
text
定时任务触发 → 读取MySQL帖子数据 → 计算热度分 → 写入Redis ZSet
     ↓
每日/每小时执行
9.3 用户关注流程
text
用户A关注用户B → 社交服务 → 写MySQL关系表 → 
                ↓
           删除缓存(user:followers:B, user:following:A) →
                ↓
           发送MQ → 通知服务推送消息给B
十、架构决策记录
决策点	选项	最终选择	理由
单体 vs 微服务	单体 / 微服务	模块化单体（先单体后拆分）	课程项目规模，过度设计增加复杂度
前后端分离	是 / 否	是	独立开发部署，移动端复用
数据库分表	是 / 否	先不分表，预留分表字段	数据量可控，按时间后缀分表
搜索方案	MySQL全文索引 / ES	ES	学习价值高，功能更强
实时推送	轮询 / WebSocket	WebSocket	实时性好，减少服务器压力
认证方案	Session / JWT	JWT + Redis黑名单	无状态，易于水平扩展
十一、课程项目交付建议
11.1 MVP功能范围（建议第一期完成）
模块	包含功能	预计工作量
用户系统	注册登录、基础资料、手机验证	2周
内容系统	板块浏览、发帖、评论、点赞	3周
内容系统	简单的敏感词过滤	3天
管理后台	板块管理、用户封禁	1周
搜索	标题搜索（MySQL LIKE）	2天
部署	Docker Compose本地部署	1周
11.2 加分功能（有精力再做）
实时聊天/私信

ES全文搜索

排行榜算法

专业认证+加V

WebSocket实时讨论

后台数据分析图表

11.3 技术文档要求
文档类型	内容
架构设计文档	系统架构图、模块划分、技术选型理由
数据库设计文档	ER图、表结构说明、索引设计
API文档	Swagger/YApi 自动生成
部署文档	Docker命令、环境变量说明
测试报告	核心接口测试用例、性能测试
