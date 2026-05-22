# Wrote database_init.sql
-- ====================================================================================================
--  社区论坛系统 - 数据库初始化脚本（优化版）
--  基于 ER 图自动生成，并经过字段补充、索引优化与约束完善
--  MySQL 8.0+ 推荐
-- ====================================================================================================
-- ****************************************************************************************************
-- 一、数据库优化说明
-- ****************************************************************************************************
--
-- | 优化项           | 优化前问题                                                                 | 优化方案                                                                                      |
-- |------------------|----------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------|
-- | ① 缺少密码字段    | user 表没有 password_hash，无法实现登录认证                                 | 添加 `password_hash` VARCHAR(255)，存储加密后的密码哈希值                                     |
-- | ② 缺少头像字段    | user 表缺少头像 URL，用户个人信息不完整                                      | 添加 `avatar_url` VARCHAR(500)，存储用户头像图片地址                                          |
-- | ③ 缺少个人简介    | user 表缺少 bio 字段，用户资料展示不完整                                     | 添加 `bio` VARCHAR(200)，存储用户个人签名/简介                                                |
-- | ④ 帖子缺正文      | post 表仅有 title 而无 content，帖子无法存储正文内容                          | 添加 `content` MEDIUMTEXT，存储帖子富文本正文                                                 |
-- | ⑤ 帖子缺统计字段  | post 表缺少浏览量、评论数、收藏数，前端展示需实时联表查询(性能差)              | 添加 `view_count`、`comment_count`、`collect_count` 冗余计数，避免高频 COUNT 查询             |
-- | ⑥ 帖子缺业务标记  | post 表缺少软删除、精华、置顶等业务常见标记                                   | 添加 `is_deleted`、`is_essence`、`is_pinned` 标记字段                                        |
-- | ⑦ 评论缺点赞/删除 | comment 表缺少 like_count 和软删除标记，功能不完整                            | 添加 `like_count` INT、`is_deleted` TINYINT，支持评论点赞与软删除                             |
-- | ⑧ 举报功能不完整  | report 表缺少举报原因、处理人、处理结果，无法形成完整处理闭环                   | 添加 `reason`、`handler_id`、`handle_result`、`handled_at`                                   |
-- | ⑨ 处罚功能不完整  | user_punishment 表缺少处罚原因与操作人，无法审计追责                            | 添加 `reason` VARCHAR、`operator_id` BIGINT                                                   |
-- | ⑩ 审核缺少审核人  | audit_queue 表无 auditor_id，无法追溯审核责任人                               | 添加 `auditor_id` BIGINT，关联 user 表                                                        |
-- | ⑪ 缺少收藏功能    | ER 图中未设计帖子收藏表，社交互动不完整                                        | 新增 `post_collect` 表，记录用户收藏帖子的关系                                                |
-- | ⑫ 缺少通知功能    | ER 图中未设计消息通知表，用户无法接收点赞/评论/系统通知                         | 新增 `notification` 表，支持多类型消息推送                                                    |
-- | ⑬ 索引不足       | 部分高频查询字段未建索引（如 nickname、publish_time 排序等）                   | 补充 KEY idx_nickname、KEY idx_comment_publish_time、KEY idx_post_deleted 等                  |
-- | ⑭ 缺少软删除支持  | 实时动态、私信等表无软删除标记，数据误删难恢复                                  | 为 realtime_dynamic、private_message 等表添加 `is_deleted` 字段                               |
-- | ⑮ 关注表漏洞      | follow 表未阻止用户关注自己的异常数据                                           | 添加 CHECK 约束 `follower_id <> followee_id`（需 MySQL 8.0.16+）                             |
-- | ⑯ 私信表漏洞      | private_message 表未阻止用户给自己发私信                                       | 添加 CHECK 约束 `sender_id <> receiver_id`（需 MySQL 8.0.16+）                               |
-- ****************************************************************************************************
-- 二、实体关系总览 (ER 关系)
-- ****************************************************************************************************
--
--   ┌──────────────┐       1:N        ┌──────────────────┐
--   │    user      │──────────────────│ user_verification │  一个用户可提交多次认证
--   │   (用户)     │      1:1        ┌──────────────────┐
--   │              │─────────────────│risk_assessment_   │  一个用户仅一份风险评估
--   │  user_id PK  │                 │     answer        │
--   │  nickname    │      1:1        ┌──────────────────┐
--   │  password    │─────────────────│ user_preference   │  一个用户仅一套偏好设置
--   │  avatar_url  │      1:1        ┌──────────────────┐
--   │  ...         │─────────────────│ privacy_setting   │  一个用户仅一套隐私设置
--   └──────┬───────┘      1:1        ┌──────────────────┐
--          │                         │ user_achievement  │  一个用户仅一条成就记录
--          │ 1:N                     └──────────────────┘
--          ├─────────────────────────┐ post (帖子)        │  一个用户可发布多篇帖子
--          │ 1:N                     └──────────────────┘
--          ├─────────────────────────┐ comment (评论)      │  一个用户可发表多条评论
--          │ 1:N                     └──────────────────┘
--          ├─────────────────────────┐ realtime_dynamic   │  一个用户可发布多条动态
--          │ 1:N                     └──────────────────┘
--          ├─────────────────────────┐ follow (关注者)     │  一个用户可关注多人 (follower_id)
--          │ 1:N                     └──────────────────┘
--          ├─────────────────────────┐ follow (被关注者)   │  一个用户可被多人关注 (followee_id)
--          │ 1:N                     └──────────────────┘
--          ├─────────────────────────┐ private_message    │  一个用户可发送多条私信 (sender)
--          │ 1:N                     └──────────────────┘
--          ├─────────────────────────┐ private_message    │  一个用户可接收多条私信 (receiver)
--          │ 1:N                     └──────────────────┘
--          ├─────────────────────────┐ group_info (群组)    │  一个用户可创建多个群组
--          │ 1:N                     └──────────────────┘
--          ├─────────────────────────┐ group_member (群成员)│  一个用户可加入多个群
--          │ 1:N                     └──────────────────┘
--          ├─────────────────────────┐ group_post (群帖子)  │  一个用户可在群内发布多条帖子
--          │ 1:N                     └──────────────────┘
--          ├─────────────────────────┐ report (举报)        │  一个用户可提交多条举报
--          │ 1:N                     └──────────────────┘
--          ├─────────────────────────┐ user_punishment (处罚)│ 一个用户可被处罚多次
--          │ 1:N                     └──────────────────┘
--          ├─────────────────────────┐ user_behavior (行为)  │ 一个用户可产生多条行为日志
--          │ 1:N                     └──────────────────┘
--          ├─────────────────────────┐ post_collect (收藏)   │ 一个用户可收藏多篇帖子 (新增)
--          │ 1:N                     └──────────────────┘
--          └─────────────────────────┐ notification (通知)   │ 一个用户可收到多条通知 (新增)
--
--   ┌──────────────┐       1:N        ┌──────────────────┐
--   │   section    │──────────────────│      zone        │  一个板块包含多个分区
--   │   (板块)     │      1:N        ┌──────────────────┐
--   │              │─────────────────│      post        │  一个板块下有多篇帖子
--   └──────────────┘                  └──────────────────┘
--
--   ┌──────────────┐       1:N        ┌──────────────────┐
--   │     zone     │──────────────────│      post        │  一个分区下有多篇帖子
--   │   (分区)     │                  └──────────────────┘
--   └──────────────┘
--
--   ┌──────────────┐       1:N        ┌──────────────────┐
--   │     post     │──────────────────│    comment       │  一篇帖子可有多条评论
--   │   (帖子)     │      1:N        ┌──────────────────┐
--   │              │─────────────────│   attachment     │  一篇帖子可有多个附件
--   │              │      1:1        ┌──────────────────┐
--   │              │─────────────────│   vote_post      │  一篇帖子可关联一个投票
--   └──────────────┘                  └──────────────────┘
--
--   ┌──────────────┐       1:N        ┌──────────────────┐
--   │   comment    │──────────────────│    comment       │  一条评论可有多条回复（楼中楼自关联）
--   │   (评论)     │    parent_comment_id (自引用)        │
--   └──────────────┘                  └──────────────────┘
--
--   ┌──────────────┐       1:N        ┌──────────────────┐
--   │  vote_post   │──────────────────│   vote_record    │  一个投票可有多条投票记录
--   │   (投票帖)   │                  │   (投票记录)      │
--   └──────────────┘                  └──────────────────┘
--
--   ┌──────────────┐       1:N        ┌──────────────────┐
--   │  group_info  │──────────────────│  group_member    │  一个群组有多个成员
--   │   (群组)     │      1:N        ┌──────────────────┐
--   │              │─────────────────│   group_post     │  一个群组内有多篇帖子
--   └──────────────┘                  └──────────────────┘
--
--   ┌──────────────┐       1:1        ┌──────────────────┐
--   │     user     │──────────────────│   notification   │  一条通知对应一个接收用户
--   └──────────────┘                  └──────────────────┘
-- ****************************************************************************************************
-- 三、数据库创建与表结构定义
-- ****************************************************************************************************
CREATE DATABASE IF NOT EXISTS community_forum
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;
USE community_forum;
-- ====================================================================================================
-- 表 1：user（用户表）—— 存储所有注册用户的基本信息与安全凭证
-- ====================================================================================================
-- | 字段名             | 类型          | 约束                             | 默认值              | 说明 |
-- |--------------------|---------------|----------------------------------|---------------------|------|
-- | user_id            | BIGINT        | NOT NULL AUTO_INCREMENT, PRIMARY KEY | —                | 用户ID（主键，自增） |
-- | nickname           | VARCHAR(50)   | NOT NULL                        | —                   | 用户昵称 |
-- | password_hash      | VARCHAR(255)  | NOT NULL                        | —                   | 密码哈希值（bcrypt/argon2加密） |
-- | mobile             | VARCHAR(20)   | DEFAULT NULL, UNIQUE            | NULL                | 手机号码（唯一） |
-- | email              | VARCHAR(100)  | DEFAULT NULL, UNIQUE            | NULL                | 电子邮箱（唯一） |
-- | avatar_url         | VARCHAR(500)  | DEFAULT NULL                    | NULL                | 头像图片URL地址 |
-- | bio                | VARCHAR(200)  | DEFAULT NULL                    | NULL                | 个人简介/签名 |
-- | gender             | TINYINT       | NOT NULL                        | 0                   | 性别（0未知 1男 2女） |
-- | verification_level | INT           | NOT NULL                        | 0                   | 认证等级（0未认证 1初级 2高级） |
-- | points             | INT           | NOT NULL                        | 0                   | 用户积分 |
-- | level              | INT           | NOT NULL                        | 1                   | 用户等级 |
-- | is_banned          | TINYINT(1)    | NOT NULL                        | 0                   | 是否封禁（0否 1是） |
-- | is_deleted         | TINYINT(1)    | NOT NULL                        | 0                   | 软删除标记（0正常 1已注销） |
-- | register_ip        | VARCHAR(45)   | DEFAULT NULL                    | NULL                | 注册IP地址（支持IPv6） |
-- | created_at         | DATETIME      | NOT NULL                        | CURRENT_TIMESTAMP   | 注册时间 |
-- | updated_at         | DATETIME      | NOT NULL                        | CURRENT_TIMESTAMP ON UPDATE | 最后更新时间 |
CREATE TABLE user (
    user_id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID（主键，自增）',
    nickname            VARCHAR(50)  NOT NULL              COMMENT '用户昵称',
    password_hash       VARCHAR(255) NOT NULL              COMMENT '密码哈希值（bcrypt/argon2加密存储）',
    mobile              VARCHAR(20)  DEFAULT NULL          COMMENT '手机号码（唯一）',
    email               VARCHAR(100) DEFAULT NULL          COMMENT '电子邮箱（唯一）',
    avatar_url          VARCHAR(500) DEFAULT NULL          COMMENT '头像图片URL地址',
    bio                 VARCHAR(200) DEFAULT NULL          COMMENT '个人简介/签名',
    gender              TINYINT      NOT NULL DEFAULT 0    COMMENT '性别（0未知 1男 2女）',
    verification_level  INT          NOT NULL DEFAULT 0    COMMENT '认证等级（0未认证 1初级认证 2高级认证）',
    points              INT          NOT NULL DEFAULT 0    COMMENT '用户积分',
    level               INT          NOT NULL DEFAULT 1    COMMENT '用户等级',
    is_banned           TINYINT(1)   NOT NULL DEFAULT 0    COMMENT '是否封禁（0正常 1已封禁）',
    is_deleted          TINYINT(1)   NOT NULL DEFAULT 0    COMMENT '软删除标记（0正常 1已注销）',
    register_ip         VARCHAR(45)  DEFAULT NULL          COMMENT '注册时的IP地址（支持IPv6）',
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP             COMMENT '注册时间',
    updated_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    PRIMARY KEY (user_id),
    UNIQUE KEY uk_mobile   (mobile),
    UNIQUE KEY uk_email    (email),
    KEY        idx_nickname (nickname),
    KEY        idx_is_banned_deleted (is_banned, is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';
-- ====================================================================================================
-- 表 2：user_verification（用户认证记录表）—— 存储用户的实名/资质认证申请记录
-- ====================================================================================================
-- | 字段名            | 类型     | 约束                                      | 默认值              | 说明 |
-- |-------------------|----------|-------------------------------------------|---------------------|------|
-- | record_id         | BIGINT   | NOT NULL AUTO_INCREMENT, PRIMARY KEY       | —                   | 认证记录ID |
-- | user_id           | BIGINT   | NOT NULL, FOREIGN KEY → user(user_id)      | —                   | 用户ID |
-- | verification_type | INT      | NOT NULL                                  | —                   | 认证方式（0身份证 1学生证 2驾驶证） |
-- | audit_status      | INT      | NOT NULL                                  | 0                   | 审核状态（0待审核 1通过 2驳回） |
-- | created_at        | DATETIME | NOT NULL                                  | CURRENT_TIMESTAMP   | 申请时间 |
CREATE TABLE user_verification (
    record_id         BIGINT  NOT NULL AUTO_INCREMENT COMMENT '认证记录ID（主键，自增）',
    user_id           BIGINT  NOT NULL                COMMENT '用户ID（外键，关联user表）',
    verification_type INT     NOT NULL                COMMENT '认证方式（0身份证 1学生证 2驾驶证）',
    audit_status      INT     NOT NULL DEFAULT 0      COMMENT '审核状态（0待审核 1通过 2驳回）',
    created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    PRIMARY KEY (record_id),
    KEY idx_user_id     (user_id),
    KEY idx_audit_status (audit_status),
    CONSTRAINT fk_uv_user FOREIGN KEY (user_id) REFERENCES user (user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户认证记录表（一个用户可提交多次认证）';
-- ====================================================================================================
-- 表 3：risk_assessment_answer（风险评估答案表）—— 用户的风险承受能力测评结果，1:1 关联 user
-- ====================================================================================================
-- | 字段名         | 类型        | 约束                                      | 默认值 | 说明 |
-- |----------------|-------------|-------------------------------------------|--------|------|
-- | answer_id      | BIGINT      | NOT NULL AUTO_INCREMENT, PRIMARY KEY       | —      | 测评答案ID |
-- | user_id        | BIGINT      | NOT NULL, UNIQUE, FOREIGN KEY → user       | —      | 用户ID |
-- | result_level   | VARCHAR(20) | NOT NULL                                  | —      | 风险等级（保守型/稳健型/进取型） |
-- | complete_time  | DATETIME    | NOT NULL                                  | —      | 测评完成时间 |
CREATE TABLE risk_assessment_answer (
    answer_id     BIGINT      NOT NULL AUTO_INCREMENT COMMENT '测评答案ID（主键，自增）',
    user_id       BIGINT      NOT NULL                COMMENT '用户ID（外键，关联user表）',
    result_level  VARCHAR(20) NOT NULL                COMMENT '风险评估结果等级（保守型 / 稳健型 / 进取型）',
    complete_time DATETIME    NOT NULL                COMMENT '测评完成时间',
    PRIMARY KEY (answer_id),
    UNIQUE KEY uk_user_id (user_id),
    CONSTRAINT fk_raa_user FOREIGN KEY (user_id) REFERENCES user (user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='风险评估答案表（一个用户仅一份记录）';
-- ====================================================================================================
-- 表 4：user_preference（用户偏好表）—— 用户关注的市场与风险偏好，1:1 关联 user
-- ====================================================================================================
-- | 字段名         | 类型         | 约束                                      | 默认值              | 说明 |
-- |----------------|--------------|-------------------------------------------|---------------------|------|
-- | preference_id  | BIGINT       | NOT NULL AUTO_INCREMENT, PRIMARY KEY       | —                   | 偏好设置ID |
-- | user_id        | BIGINT       | NOT NULL, UNIQUE, FOREIGN KEY → user       | —                   | 用户ID |
-- | focus_markets  | VARCHAR(255) | DEFAULT NULL                              | NULL                | 关注的市场（如A股、港股、美股） |
-- | risk_type      | VARCHAR(50)  | DEFAULT NULL                              | NULL                | 风险偏好类型（低风险/中风险/高风险） |
-- | created_at     | DATETIME     | NOT NULL                                  | CURRENT_TIMESTAMP   | 创建时间 |
-- | updated_at     | DATETIME     | NOT NULL                                  | CURRENT_TIMESTAMP   | 最后更新时间 |
CREATE TABLE user_preference (
    preference_id BIGINT       NOT NULL AUTO_INCREMENT COMMENT '偏好设置ID（主键，自增）',
    user_id       BIGINT       NOT NULL                COMMENT '用户ID（外键，关联user表）',
    focus_markets VARCHAR(255) DEFAULT NULL            COMMENT '关注的市场（如：A股、港股、美股）',
    risk_type     VARCHAR(50)  DEFAULT NULL            COMMENT '风险偏好类型（低风险 / 中风险 / 高风险）',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP             COMMENT '创建时间',
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    PRIMARY KEY (preference_id),
    UNIQUE KEY uk_user_id (user_id),
    CONSTRAINT fk_up_user FOREIGN KEY (user_id) REFERENCES user (user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户偏好表（一个用户仅一条偏好记录）';
-- ====================================================================================================
-- 表 5：privacy_setting（隐私设置表）—— 用户个人资料的可见性设置，1:1 关联 user
-- ====================================================================================================
-- | 字段名             | 类型     | 约束                                      | 默认值              | 说明 |
-- |--------------------|----------|-------------------------------------------|---------------------|------|
-- | setting_id         | BIGINT   | NOT NULL AUTO_INCREMENT, PRIMARY KEY       | —                   | 隐私设置ID |
-- | user_id            | BIGINT   | NOT NULL, UNIQUE, FOREIGN KEY → user       | —                   | 用户ID |
-- | profile_visibility | INT      | NOT NULL                                  | 0                   | 资料可见范围（0仅自己 1好友可见 2所有人） |
-- | updated_at         | DATETIME | NOT NULL                                  | CURRENT_TIMESTAMP   | 最后更新时间 |
CREATE TABLE privacy_setting (
    setting_id        BIGINT  NOT NULL AUTO_INCREMENT COMMENT '隐私设置ID（主键，自增）',
    user_id           BIGINT  NOT NULL                COMMENT '用户ID（外键，关联user表）',
    profile_visibility INT    NOT NULL DEFAULT 0      COMMENT '个人资料可见范围（0仅自己 1好友可见 2所有人）',
    updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    PRIMARY KEY (setting_id),
    UNIQUE KEY uk_user_id (user_id),
    CONSTRAINT fk_ps_user FOREIGN KEY (user_id) REFERENCES user (user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='隐私设置表（一个用户仅一条隐私设置）';
-- ====================================================================================================
-- 表 6：user_achievement（用户成就表）—— 统计用户累计发帖与精华帖数量，1:1 关联 user
-- ====================================================================================================
-- | 字段名             | 类型     | 约束                                      | 默认值              | 说明 |
-- |--------------------|----------|-------------------------------------------|---------------------|------|
-- | achievement_id     | BIGINT   | NOT NULL AUTO_INCREMENT, PRIMARY KEY       | —                   | 成就记录ID |
-- | user_id            | BIGINT   | NOT NULL, UNIQUE, FOREIGN KEY → user       | —                   | 用户ID |
-- | total_post_count   | INT      | NOT NULL                                  | 0                   | 累计发帖总数 |
-- | essence_post_count | INT      | NOT NULL                                  | 0                   | 累计精华帖数 |
-- | updated_at         | DATETIME | NOT NULL                                  | CURRENT_TIMESTAMP   | 最后更新时间 |
CREATE TABLE user_achievement (
    achievement_id    BIGINT  NOT NULL AUTO_INCREMENT COMMENT '成就记录ID（主键，自增）',
    user_id           BIGINT  NOT NULL                COMMENT '用户ID（外键，关联user表）',
    total_post_count  INT     NOT NULL DEFAULT 0      COMMENT '累计发帖总数',
    essence_post_count INT    NOT NULL DEFAULT 0      COMMENT '累计精华帖数',
    updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    PRIMARY KEY (achievement_id),
    UNIQUE KEY uk_user_id (user_id),
    CONSTRAINT fk_ua_user FOREIGN KEY (user_id) REFERENCES user (user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户成就表（一个用户仅一条成就记录）';
-- ====================================================================================================
-- 表 7：section（板块表）—— 论坛的一级栏目分类
-- ====================================================================================================
-- | 字段名        | 类型        | 约束                             | 默认值              | 说明 |
-- |---------------|-------------|----------------------------------|---------------------|------|
-- | section_id    | INT         | NOT NULL AUTO_INCREMENT, PRIMARY KEY | —                | 板块ID |
-- | section_name  | VARCHAR(50) | NOT NULL, UNIQUE                 | —                   | 板块名称（如：股票、基金、期货） |
-- | section_type  | INT         | NOT NULL                        | —                   | 板块类型（0讨论区 1资讯区 2问答区） |
-- | sort_order    | INT         | NOT NULL                        | 0                   | 排序编号（值越小越靠前） |
-- | created_at    | DATETIME    | NOT NULL                        | CURRENT_TIMESTAMP   | 创建时间 |
CREATE TABLE section (
    section_id   INT         NOT NULL AUTO_INCREMENT COMMENT '板块ID（主键，自增）',
    section_name VARCHAR(50) NOT NULL                COMMENT '板块名称（如：股票、基金、期货）',
    section_type INT         NOT NULL                COMMENT '板块类型（0讨论区 1资讯区 2问答区）',
    sort_order   INT         NOT NULL DEFAULT 0      COMMENT '排序编号（数值越小越靠前）',
    created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (section_id),
    UNIQUE KEY uk_section_name (section_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='板块表';
-- ====================================================================================================
-- 表 8：zone（分区表）—— 板块下的二级分类，作为 section 的子级
-- ====================================================================================================
-- | 字段名     | 类型        | 约束                                       | 默认值              | 说明 |
-- |------------|-------------|--------------------------------------------|---------------------|------|
-- | zone_id    | INT         | NOT NULL AUTO_INCREMENT, PRIMARY KEY        | —                   | 分区ID |
-- | zone_name  | VARCHAR(50) | NOT NULL                                   | —                   | 分区名称（如：A股、港股、美股） |
-- | section_id | INT         | NOT NULL, FOREIGN KEY → section            | —                   | 所属板块ID |
-- | sort_order | INT         | NOT NULL                                   | 0                   | 排序编号（值越小越靠前） |
-- | created_at | DATETIME    | NOT NULL                                   | CURRENT_TIMESTAMP   | 创建时间 |
CREATE TABLE zone (
    zone_id    INT         NOT NULL AUTO_INCREMENT COMMENT '分区ID（主键，自增）',
    zone_name  VARCHAR(50) NOT NULL                COMMENT '分区名称（如：A股、港股、美股）',
    section_id INT         NOT NULL                COMMENT '所属板块ID（外键，关联section表）',
    sort_order INT         NOT NULL DEFAULT 0      COMMENT '排序编号（数值越小越靠前）',
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (zone_id),
    KEY idx_section_id (section_id),
    CONSTRAINT fk_zone_section FOREIGN KEY (section_id) REFERENCES section (section_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分区表（一个板块包含多个分区）';
-- ====================================================================================================
-- 表 9：post（帖子表）—— 论坛核心内容实体，存储用户发布的帖子正文与元数据
-- ====================================================================================================
-- | 字段名         | 类型          | 约束                                        | 默认值              | 说明 |
-- |----------------|---------------|---------------------------------------------|---------------------|------|
-- | post_id        | BIGINT        | NOT NULL AUTO_INCREMENT, PRIMARY KEY         | —                   | 帖子ID |
-- | author_id      | BIGINT        | NOT NULL, FOREIGN KEY → user                | —                   | 作者用户ID |
-- | title          | VARCHAR(200)  | NOT NULL                                    | —                   | 帖子标题 |
-- | content        | MEDIUMTEXT    | NOT NULL                                    | —                   | 帖子正文（富文本/Markdown，最大约16MB） |
-- | content_type   | INT           | NOT NULL                                    | —                   | 内容类型（0普通帖 1投票帖 2图文帖） |
-- | section_id     | INT           | DEFAULT NULL, FOREIGN KEY → section         | NULL                | 所属板块ID |
-- | zone_id        | INT           | DEFAULT NULL, FOREIGN KEY → zone            | NULL                | 所属分区ID |
-- | audit_status   | INT           | NOT NULL                                    | 0                   | 审核状态（0待审核 1通过 2驳回） |
-- | like_count     | INT           | NOT NULL                                    | 0                   | 点赞数 |
-- | view_count     | INT           | NOT NULL                                    | 0                   | 浏览次数（冗余计数，避免COUNT查询） |
-- | comment_count  | INT           | NOT NULL                                    | 0                   | 评论数（冗余计数） |
-- | collect_count  | INT           | NOT NULL                                    | 0                   | 收藏数（冗余计数） |
-- | is_deleted     | TINYINT(1)    | NOT NULL                                    | 0                   | 软删除标记（0正常 1已删除） |
-- | is_essence     | TINYINT(1)    | NOT NULL                                    | 0                   | 是否精华帖（0否 1是） |
-- | is_pinned      | TINYINT(1)    | NOT NULL                                    | 0                   | 是否置顶帖（0否 1是） |
-- | publish_time   | DATETIME      | NOT NULL                                    | CURRENT_TIMESTAMP   | 发布时间 |
-- | updated_at     | DATETIME      | NOT NULL                                    | CURRENT_TIMESTAMP   | 最后更新时间 |
CREATE TABLE post (
    post_id       BIGINT       NOT NULL AUTO_INCREMENT COMMENT '帖子ID（主键，自增）',
    author_id     BIGINT       NOT NULL                COMMENT '作者用户ID（外键，关联user表）',
    title         VARCHAR(200) NOT NULL                COMMENT '帖子标题',
    content       MEDIUMTEXT   NOT NULL                COMMENT '帖子正文（支持富文本/Markdown，最大约16MB）',
    content_type  INT          NOT NULL                COMMENT '内容类型（0普通帖 1投票帖 2图文帖）',
    section_id    INT          DEFAULT NULL            COMMENT '所属板块ID（外键，关联section表）',
    zone_id       INT          DEFAULT NULL            COMMENT '所属分区ID（外键，关联zone表）',
    audit_status  INT          NOT NULL DEFAULT 0      COMMENT '审核状态（0待审核 1通过 2驳回）',
    like_count    INT          NOT NULL DEFAULT 0      COMMENT '点赞数',
    view_count    INT          NOT NULL DEFAULT 0      COMMENT '浏览次数（冗余计数）',
    comment_count INT          NOT NULL DEFAULT 0      COMMENT '评论数（冗余计数）',
    collect_count INT          NOT NULL DEFAULT 0      COMMENT '收藏数（冗余计数）',
    is_deleted    TINYINT(1)   NOT NULL DEFAULT 0      COMMENT '软删除标记（0正常 1已删除）',
    is_essence    TINYINT(1)   NOT NULL DEFAULT 0      COMMENT '是否精华帖（0否 1是）',
    is_pinned     TINYINT(1)   NOT NULL DEFAULT 0      COMMENT '是否置顶帖（0否 1是）',
    publish_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP             COMMENT '发布时间',
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    PRIMARY KEY (post_id),
    KEY idx_author_id    (author_id),
    KEY idx_section_id   (section_id),
    KEY idx_zone_id      (zone_id),
    KEY idx_audit_status (audit_status),
    KEY idx_publish_time (publish_time),
    KEY idx_deleted_audit (is_deleted, audit_status),
    CONSTRAINT fk_post_author  FOREIGN KEY (author_id)  REFERENCES user (user_id)       ON DELETE CASCADE,
    CONSTRAINT fk_post_section FOREIGN KEY (section_id) REFERENCES section (section_id) ON DELETE SET NULL,
    CONSTRAINT fk_post_zone    FOREIGN KEY (zone_id)    REFERENCES zone (zone_id)       ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='帖子表';
-- ====================================================================================================
-- 表 10：comment（评论表）—— 帖子下的评论与回复，支持楼中楼嵌套（自关联）
-- ====================================================================================================
-- | 字段名            | 类型     | 约束                                         | 默认值              | 说明 |
-- |-------------------|----------|----------------------------------------------|---------------------|------|
-- | comment_id        | BIGINT   | NOT NULL AUTO_INCREMENT, PRIMARY KEY          | —                   | 评论ID |
-- | post_id           | BIGINT   | NOT NULL, FOREIGN KEY → post                 | —                   | 所属帖子ID |
-- | parent_comment_id | BIGINT   | DEFAULT NULL, FOREIGN KEY → comment (自引用)  | NULL                | 父评论ID（楼中楼，NULL表示直接评论帖子） |
-- | author_id         | BIGINT   | NOT NULL, FOREIGN KEY → user                 | —                   | 评论作者用户ID |
-- | content           | TEXT     | NOT NULL                                     | —                   | 评论内容 |
-- | like_count        | INT      | NOT NULL                                     | 0                   | 点赞数 |
-- | is_deleted        | TINYINT(1)| NOT NULL                                    | 0                   | 软删除标记（0正常 1已删除） |
-- | publish_time      | DATETIME | NOT NULL                                     | CURRENT_TIMESTAMP   | 评论发布时间 |
CREATE TABLE comment (
    comment_id        BIGINT   NOT NULL AUTO_INCREMENT COMMENT '评论ID（主键，自增）',
    post_id           BIGINT   NOT NULL                COMMENT '所属帖子ID（外键，关联post表）',
    parent_comment_id BIGINT   DEFAULT NULL            COMMENT '父评论ID（外键自关联，用于楼中楼回复，NULL为直接评论帖子）',
    author_id         BIGINT   NOT NULL                COMMENT '评论作者用户ID（外键，关联user表）',
    content           TEXT     NOT NULL                COMMENT '评论内容',
    like_count        INT      NOT NULL DEFAULT 0      COMMENT '点赞数',
    is_deleted        TINYINT(1) NOT NULL DEFAULT 0    COMMENT '软删除标记（0正常 1已删除）',
    publish_time      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评论发布时间',
    PRIMARY KEY (comment_id),
    KEY idx_post_id          (post_id),
    KEY idx_parent_comment_id (parent_comment_id),
    KEY idx_author_id        (author_id),
    KEY idx_publish_time     (publish_time),
    CONSTRAINT fk_comment_post   FOREIGN KEY (post_id)           REFERENCES post (post_id)       ON DELETE CASCADE,
    CONSTRAINT fk_comment_parent FOREIGN KEY (parent_comment_id) REFERENCES comment (comment_id) ON DELETE CASCADE,
    CONSTRAINT fk_comment_author FOREIGN KEY (author_id)         REFERENCES user (user_id)       ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论表（支持楼中楼嵌套回复）';
-- ====================================================================================================
-- 表 11：vote_post（投票帖子表）—— 帖子的投票扩展信息，与 post 为 1:1 关系
-- ====================================================================================================
-- | 字段名     | 类型         | 约束                                       | 默认值 | 说明 |
-- |------------|--------------|--------------------------------------------|--------|------|
-- | vote_id    | BIGINT       | NOT NULL AUTO_INCREMENT, PRIMARY KEY        | —      | 投票ID |
-- | post_id    | BIGINT       | NOT NULL, UNIQUE, FOREIGN KEY → post        | —      | 关联的帖子ID |
-- | vote_title | VARCHAR(200) | NOT NULL                                   | —      | 投票标题（投票问题） |
-- | end_time   | DATETIME     | NOT NULL                                   | —      | 投票截止时间 |
CREATE TABLE vote_post (
    vote_id    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '投票ID（主键，自增）',
    post_id    BIGINT       NOT NULL                COMMENT '关联的帖子ID（外键，关联post表）',
    vote_title VARCHAR(200) NOT NULL                COMMENT '投票标题（投票问题）',
    end_time   DATETIME     NOT NULL                COMMENT '投票截止时间',
    PRIMARY KEY (vote_id),
    UNIQUE KEY uk_post_id (post_id),
    CONSTRAINT fk_vote_post FOREIGN KEY (post_id) REFERENCES post (post_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='投票帖子表（一个帖子仅可关联一个投票）';
-- ====================================================================================================
-- 表 12：vote_record（投票记录表）—— 用户在某投票中的具体投票选择
-- ====================================================================================================
-- | 字段名       | 类型     | 约束                                            | 默认值              | 说明 |
-- |--------------|----------|-------------------------------------------------|---------------------|------|
-- | record_id    | BIGINT   | NOT NULL AUTO_INCREMENT, PRIMARY KEY             | —                   | 投票记录ID |
-- | vote_id      | BIGINT   | NOT NULL, FOREIGN KEY → vote_post               | —                   | 投票ID |
-- | user_id      | BIGINT   | NOT NULL, FOREIGN KEY → user                    | —                   | 投票用户ID |
-- | option_index | INT      | NOT NULL                                        | —                   | 用户选择的选项序号（0-A 1-B 2-C ...） |
-- | vote_time    | DATETIME | NOT NULL                                        | CURRENT_TIMESTAMP   | 投票时间 |
CREATE TABLE vote_record (
    record_id    BIGINT   NOT NULL AUTO_INCREMENT COMMENT '投票记录ID（主键，自增）',
    vote_id      BIGINT   NOT NULL                COMMENT '投票ID（外键，关联vote_post表）',
    user_id      BIGINT   NOT NULL                COMMENT '投票用户ID（外键，关联user表）',
    option_index INT      NOT NULL                COMMENT '用户选择的选项序号（0-A选项 1-B选项 2-C选项等）',
    vote_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '投票时间',
    PRIMARY KEY (record_id),
    UNIQUE KEY uk_vote_user (vote_id, user_id),
    KEY idx_user_id (user_id),
    CONSTRAINT fk_vr_vote FOREIGN KEY (vote_id) REFERENCES vote_post (vote_id) ON DELETE CASCADE,
    CONSTRAINT fk_vr_user FOREIGN KEY (user_id) REFERENCES user (user_id)      ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='投票记录表（同一用户对同一投票仅可投一次）';
-- ====================================================================================================
-- 表 13：attachment（附件表）—— 帖子中包含的附件文件（图片、文档、视频等）
-- ====================================================================================================
-- | 字段名         | 类型         | 约束                                       | 默认值              | 说明 |
-- |----------------|--------------|--------------------------------------------|---------------------|------|
-- | attachment_id  | BIGINT       | NOT NULL AUTO_INCREMENT, PRIMARY KEY        | —                   | 附件ID |
-- | post_id        | BIGINT       | NOT NULL, FOREIGN KEY → post               | —                   | 所属帖子ID |
-- | file_name      | VARCHAR(255) | NOT NULL                                   | —                   | 文件名（存储路径） |
-- | file_type      | INT          | NOT NULL                                   | —                   | 文件类型（0图片 1文档 2视频 3音频） |
-- | audit_status   | INT          | NOT NULL                                   | 0                   | 审核状态（0待审核 1通过 2驳回） |
-- | created_at     | DATETIME     | NOT NULL                                   | CURRENT_TIMESTAMP   | 上传时间 |
CREATE TABLE attachment (
    attachment_id BIGINT       NOT NULL AUTO_INCREMENT COMMENT '附件ID（主键，自增）',
    post_id       BIGINT       NOT NULL                COMMENT '所属帖子ID（外键，关联post表）',
    file_name     VARCHAR(255) NOT NULL                COMMENT '附件文件名（存储路径）',
    file_type     INT          NOT NULL                COMMENT '文件类型（0图片 1文档 2视频 3音频）',
    audit_status  INT          NOT NULL DEFAULT 0      COMMENT '审核状态（0待审核 1通过 2驳回）',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    PRIMARY KEY (attachment_id),
    KEY idx_post_id      (post_id),
    KEY idx_audit_status (audit_status),
    CONSTRAINT fk_att_post FOREIGN KEY (post_id) REFERENCES post (post_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='附件表（一个帖子可包含多个附件）';
-- ====================================================================================================
-- 表 14：realtime_dynamic（实时动态表）—— 用户的短动态，类似微博/朋友圈
-- ====================================================================================================
-- | 字段名       | 类型     | 约束                                       | 默认值              | 说明 |
-- |--------------|----------|--------------------------------------------|---------------------|------|
-- | dynamic_id   | BIGINT   | NOT NULL AUTO_INCREMENT, PRIMARY KEY        | —                   | 动态ID |
-- | author_id    | BIGINT   | NOT NULL, FOREIGN KEY → user               | —                   | 作者用户ID |
-- | content      | TEXT     | NOT NULL                                   | —                   | 动态内容 |
-- | like_count   | INT      | NOT NULL                                   | 0                   | 点赞数 |
-- | is_deleted   | TINYINT(1)| NOT NULL                                  | 0                   | 软删除标记（0正常 1已删除） |
-- | publish_time | DATETIME | NOT NULL                                   | CURRENT_TIMESTAMP   | 发布时间 |
CREATE TABLE realtime_dynamic (
    dynamic_id   BIGINT   NOT NULL AUTO_INCREMENT COMMENT '动态ID（主键，自增）',
    author_id    BIGINT   NOT NULL                COMMENT '作者用户ID（外键，关联user表）',
    content      TEXT     NOT NULL                COMMENT '动态内容',
    like_count   INT      NOT NULL DEFAULT 0      COMMENT '点赞数',
    is_deleted   TINYINT(1) NOT NULL DEFAULT 0    COMMENT '软删除标记（0正常 1已删除）',
    publish_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
    PRIMARY KEY (dynamic_id),
    KEY idx_author_id    (author_id),
    KEY idx_publish_time (publish_time),
    CONSTRAINT fk_rd_author FOREIGN KEY (author_id) REFERENCES user (user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='实时动态表';
-- ====================================================================================================
-- 表 15：follow（关注表）—— 用户之间的关注/粉丝关系
-- ====================================================================================================
-- | 字段名      | 类型      | 约束                                            | 默认值              | 说明 |
-- |-------------|-----------|-------------------------------------------------|---------------------|------|
-- | relation_id | BIGINT    | NOT NULL AUTO_INCREMENT, PRIMARY KEY             | —                   | 关注关系ID |
-- | follower_id | BIGINT    | NOT NULL, FOREIGN KEY → user                    | —                   | 关注者用户ID（主动关注的人） |
-- | followee_id | BIGINT    | NOT NULL, FOREIGN KEY → user                    | —                   | 被关注者用户ID（被关注的人） |
-- | is_starred  | TINYINT(1)| NOT NULL                                       | 0                   | 是否特别关注（0否 1是） |
-- | created_at  | DATETIME  | NOT NULL                                       | CURRENT_TIMESTAMP   | 关注时间 |
CREATE TABLE follow (
    relation_id BIGINT    NOT NULL AUTO_INCREMENT COMMENT '关注关系ID（主键，自增）',
    follower_id BIGINT    NOT NULL                COMMENT '关注者用户ID（外键，关联user表，主动关注的人）',
    followee_id BIGINT    NOT NULL                COMMENT '被关注者用户ID（外键，关联user表，被关注的人）',
    is_starred  TINYINT(1) NOT NULL DEFAULT 0     COMMENT '是否设为特别关注（0否 1是）',
    created_at  DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '关注时间',
    PRIMARY KEY (relation_id),
    UNIQUE KEY uk_follower_followee (follower_id, followee_id),
    KEY idx_followee_id (followee_id),
    CONSTRAINT chk_no_self_follow CHECK (follower_id <> followee_id),
    CONSTRAINT fk_follow_follower FOREIGN KEY (follower_id) REFERENCES user (user_id) ON DELETE CASCADE,
    CONSTRAINT fk_follow_followee FOREIGN KEY (followee_id) REFERENCES user (user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='关注表（防止用户自关注）';
-- ====================================================================================================
-- 表 16：private_message（私信表）—— 用户之间的一对一私信消息
-- ====================================================================================================
-- | 字段名      | 类型      | 约束                                            | 默认值              | 说明 |
-- |-------------|-----------|-------------------------------------------------|---------------------|------|
-- | message_id  | BIGINT    | NOT NULL AUTO_INCREMENT, PRIMARY KEY             | —                   | 私信ID |
-- | sender_id   | BIGINT    | NOT NULL, FOREIGN KEY → user                    | —                   | 发送者用户ID |
-- | receiver_id | BIGINT    | NOT NULL, FOREIGN KEY → user                    | —                   | 接收者用户ID |
-- | content     | TEXT      | NOT NULL                                       | —                   | 私信内容 |
-- | is_read     | TINYINT(1)| NOT NULL                                       | 0                   | 是否已读（0未读 1已读） |
-- | is_deleted  | TINYINT(1)| NOT NULL                                       | 0                   | 软删除标记（0正常 1已删除） |
-- | send_time   | DATETIME  | NOT NULL                                       | CURRENT_TIMESTAMP   | 发送时间 |
CREATE TABLE private_message (
    message_id  BIGINT    NOT NULL AUTO_INCREMENT COMMENT '私信ID（主键，自增）',
    sender_id   BIGINT    NOT NULL                COMMENT '发送者用户ID（外键，关联user表）',
    receiver_id BIGINT    NOT NULL                COMMENT '接收者用户ID（外键，关联user表）',
    content     TEXT      NOT NULL                COMMENT '私信内容',
    is_read     TINYINT(1) NOT NULL DEFAULT 0     COMMENT '是否已读（0未读 1已读）',
    is_deleted  TINYINT(1) NOT NULL DEFAULT 0     COMMENT '软删除标记（0正常 1已删除）',
    send_time   DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    PRIMARY KEY (message_id),
    KEY idx_sender_id      (sender_id),
    KEY idx_receiver_id    (receiver_id),
    KEY idx_receiver_read  (receiver_id, is_read),
    CONSTRAINT chk_no_self_message CHECK (sender_id <> receiver_id),
    CONSTRAINT fk_pm_sender   FOREIGN KEY (sender_id)   REFERENCES user (user_id) ON DELETE CASCADE,
    CONSTRAINT fk_pm_receiver FOREIGN KEY (receiver_id) REFERENCES user (user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='私信表（防止用户自发送私信）';
-- ====================================================================================================
-- 表 17：group_info（群组表）—— 用户创建的群组信息（GROUP 是 SQL 保留字，故命名为 group_info）
-- ====================================================================================================
-- | 字段名     | 类型         | 约束                                       | 默认值              | 说明 |
-- |------------|--------------|--------------------------------------------|---------------------|------|
-- | group_id   | BIGINT       | NOT NULL AUTO_INCREMENT, PRIMARY KEY        | —                   | 群组ID |
-- | owner_id   | BIGINT       | NOT NULL, FOREIGN KEY → user               | —                   | 群主用户ID |
-- | group_name | VARCHAR(100) | NOT NULL                                   | —                   | 群组名称 |
-- | mode       | INT          | NOT NULL                                   | 0                   | 加群模式（0自由加入 1审核加入 2禁止加入） |
-- | status     | INT          | NOT NULL                                   | 1                   | 群组状态（0已解散 1正常 2禁言中） |
-- | created_at | DATETIME     | NOT NULL                                   | CURRENT_TIMESTAMP   | 创建时间 |
-- | updated_at | DATETIME     | NOT NULL                                   | CURRENT_TIMESTAMP   | 最后更新时间 |
CREATE TABLE group_info (
    group_id   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '群组ID（主键，自增）',
    owner_id   BIGINT       NOT NULL                COMMENT '群主用户ID（外键，关联user表）',
    group_name VARCHAR(100) NOT NULL                COMMENT '群组名称',
    mode       INT          NOT NULL DEFAULT 0      COMMENT '加群模式（0自由加入 1审核加入 2禁止加入）',
    status     INT          NOT NULL DEFAULT 1      COMMENT '群组状态（0已解散 1正常 2禁言中）',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP             COMMENT '创建时间',
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    PRIMARY KEY (group_id),
    KEY idx_owner_id (owner_id),
    CONSTRAINT fk_group_owner FOREIGN KEY (owner_id) REFERENCES user (user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群组表';
-- ====================================================================================================
-- 表 18：group_member（群组成员表）—— 群组与用户的多对多成员关系
-- ====================================================================================================
-- | 字段名    | 类型     | 约束                                            | 默认值              | 说明 |
-- |-----------|----------|-------------------------------------------------|---------------------|------|
-- | member_id | BIGINT   | NOT NULL AUTO_INCREMENT, PRIMARY KEY             | —                   | 成员记录ID |
-- | group_id  | BIGINT   | NOT NULL, FOREIGN KEY → group_info              | —                   | 群组ID |
-- | user_id   | BIGINT   | NOT NULL, FOREIGN KEY → user                    | —                   | 用户ID |
-- | role      | INT      | NOT NULL                                       | 0                   | 群内角色（0普通成员 1管理员 2群主） |
-- | joined_at | DATETIME | NOT NULL                                       | CURRENT_TIMESTAMP   | 加入时间 |
CREATE TABLE group_member (
    member_id BIGINT   NOT NULL AUTO_INCREMENT COMMENT '成员记录ID（主键，自增）',
    group_id  BIGINT   NOT NULL                COMMENT '群组ID（外键，关联group_info表）',
    user_id   BIGINT   NOT NULL                COMMENT '用户ID（外键，关联user表）',
    role      INT      NOT NULL DEFAULT 0      COMMENT '群内角色（0普通成员 1管理员 2群主）',
    joined_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    PRIMARY KEY (member_id),
    UNIQUE KEY uk_group_user (group_id, user_id),
    KEY idx_user_id (user_id),
    CONSTRAINT fk_gm_group FOREIGN KEY (group_id) REFERENCES group_info (group_id) ON DELETE CASCADE,
    CONSTRAINT fk_gm_user  FOREIGN KEY (user_id)  REFERENCES user (user_id)      ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群组成员表（同一用户在同一群中仅一条记录）';
-- ====================================================================================================
-- 表 19：group_post（群帖子表）—— 群组内部的帖子，仅群成员可见
-- ====================================================================================================
-- | 字段名        | 类型     | 约束                                       | 默认值              | 说明 |
-- |---------------|----------|--------------------------------------------|---------------------|------|
-- | group_post_id | BIGINT   | NOT NULL AUTO_INCREMENT, PRIMARY KEY        | —                   | 群帖子ID |
-- | group_id      | BIGINT   | NOT NULL, FOREIGN KEY → group_info         | —                   | 所属群组ID |
-- | author_id     | BIGINT   | NOT NULL, FOREIGN KEY → user               | —                   | 作者用户ID |
-- | content       | TEXT     | NOT NULL                                   | —                   | 群帖子内容 |
-- | like_count    | INT      | NOT NULL                                   | 0                   | 点赞数 |
-- | is_deleted    | TINYINT(1)| NOT NULL                                  | 0                   | 软删除标记（0正常 1已删除） |
-- | publish_time  | DATETIME | NOT NULL                                   | CURRENT_TIMESTAMP   | 发布时间 |
CREATE TABLE group_post (
    group_post_id BIGINT   NOT NULL AUTO_INCREMENT COMMENT '群帖子ID（主键，自增）',
    group_id      BIGINT   NOT NULL                COMMENT '所属群组ID（外键，关联group_info表）',
    author_id     BIGINT   NOT NULL                COMMENT '作者用户ID（外键，关联user表）',
    content       TEXT     NOT NULL                COMMENT '群帖子内容',
    like_count    INT      NOT NULL DEFAULT 0      COMMENT '点赞数',
    is_deleted    TINYINT(1) NOT NULL DEFAULT 0    COMMENT '软删除标记（0正常 1已删除）',
    publish_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
    PRIMARY KEY (group_post_id),
    KEY idx_group_id  (group_id),
    KEY idx_author_id (author_id),
    CONSTRAINT fk_gp_group  FOREIGN KEY (group_id)  REFERENCES group_info (group_id) ON DELETE CASCADE,
    CONSTRAINT fk_gp_author FOREIGN KEY (author_id) REFERENCES user (user_id)       ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群帖子表';
-- ====================================================================================================
-- 表 20：audit_queue（审核队列表）—— 待审核内容的统一队列，供审核人员处理
-- ====================================================================================================
-- | 字段名         | 类型         | 约束                             | 默认值              | 说明 |
-- |----------------|--------------|----------------------------------|---------------------|------|
-- | audit_item_id  | BIGINT       | NOT NULL AUTO_INCREMENT, PRIMARY KEY | —                | 审核项ID |
-- | content_type   | INT          | NOT NULL                        | —                   | 内容类型（0帖子 1评论 2附件） |
-- | content_id     | BIGINT       | NOT NULL                        | —                   | 待审核内容的ID（对应各内容表主键） |
-- | audit_status   | INT          | NOT NULL                        | 0                   | 审核状态（0待审核 1通过 2驳回） |
-- | auditor_id     | BIGINT       | DEFAULT NULL, FOREIGN KEY → user | NULL                | 审核人用户ID |
-- | audit_comment  | VARCHAR(500) | DEFAULT NULL                    | NULL                | 审核意见/备注 |
-- | created_at     | DATETIME     | NOT NULL                        | CURRENT_TIMESTAMP   | 进入队列时间 |
-- | audited_at     | DATETIME     | DEFAULT NULL                    | NULL                | 审核完成时间 |
CREATE TABLE audit_queue (
    audit_item_id BIGINT       NOT NULL AUTO_INCREMENT COMMENT '审核项ID（主键，自增）',
    content_type  INT          NOT NULL                COMMENT '内容类型（0帖子 1评论 2附件）',
    content_id    BIGINT       NOT NULL                COMMENT '待审核内容的ID（对应各内容表主键）',
    audit_status  INT          NOT NULL DEFAULT 0      COMMENT '审核状态（0待审核 1通过 2驳回）',
    auditor_id    BIGINT       DEFAULT NULL            COMMENT '审核人用户ID（外键，关联user表）',
    audit_comment VARCHAR(500) DEFAULT NULL            COMMENT '审核意见/备注',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '进入队列时间',
    audited_at    DATETIME     DEFAULT NULL            COMMENT '审核完成时间',
    PRIMARY KEY (audit_item_id),
    KEY idx_content_type_status (content_type, audit_status),
    KEY idx_content_id          (content_id),
    KEY idx_auditor_id          (auditor_id),
    CONSTRAINT fk_aq_auditor FOREIGN KEY (auditor_id) REFERENCES user (user_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审核队列表';
-- ====================================================================================================
-- 表 21：report（举报表）—— 用户对违规内容的举报记录，支持处理闭环
-- ====================================================================================================
-- | 字段名        | 类型         | 约束                                       | 默认值              | 说明 |
-- |---------------|--------------|--------------------------------------------|---------------------|------|
-- | report_id     | BIGINT       | NOT NULL AUTO_INCREMENT, PRIMARY KEY        | —                   | 举报ID |
-- | reporter_id   | BIGINT       | NOT NULL, FOREIGN KEY → user               | —                   | 举报人用户ID |
-- | target_type   | INT          | NOT NULL                                   | —                   | 被举报目标类型（0帖子 1评论 2用户 3私信） |
-- | target_id     | BIGINT       | NOT NULL                                   | —                   | 被举报目标ID（对应各表主键） |
-- | reason        | VARCHAR(500) | DEFAULT NULL                               | NULL                | 举报原因 |
-- | status        | INT          | NOT NULL                                   | 0                   | 处理状态（0待处理 1已处理 2驳回） |
-- | handler_id    | BIGINT       | DEFAULT NULL, FOREIGN KEY → user            | NULL                | 处理人用户ID |
-- | handle_result | INT          | DEFAULT NULL                               | NULL                | 处理结果（0忽略 1警告 2删帖 3封禁） |
-- | created_at    | DATETIME     | NOT NULL                                   | CURRENT_TIMESTAMP   | 举报时间 |
-- | handled_at    | DATETIME     | DEFAULT NULL                               | NULL                | 处理完成时间 |
CREATE TABLE report (
    report_id     BIGINT       NOT NULL AUTO_INCREMENT COMMENT '举报ID（主键，自增）',
    reporter_id   BIGINT       NOT NULL                COMMENT '举报人用户ID（外键，关联user表）',
    target_type   INT          NOT NULL                COMMENT '被举报目标类型（0帖子 1评论 2用户 3私信）',
    target_id     BIGINT       NOT NULL                COMMENT '被举报目标ID（对应各表主键）',
    reason        VARCHAR(500) DEFAULT NULL            COMMENT '举报原因描述',
    status        INT          NOT NULL DEFAULT 0      COMMENT '处理状态（0待处理 1已处理 2驳回）',
    handler_id    BIGINT       DEFAULT NULL            COMMENT '处理人用户ID（外键，关联user表）',
    handle_result INT          DEFAULT NULL            COMMENT '处理结果（0忽略 1警告 2删帖 3封禁）',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '举报时间',
    handled_at    DATETIME     DEFAULT NULL            COMMENT '处理完成时间',
    PRIMARY KEY (report_id),
    KEY idx_reporter_id (reporter_id),
    KEY idx_target      (target_type, target_id),
    KEY idx_status      (status),
    KEY idx_handler_id  (handler_id),
    CONSTRAINT fk_report_reporter FOREIGN KEY (reporter_id) REFERENCES user (user_id) ON DELETE CASCADE,
    CONSTRAINT fk_report_handler  FOREIGN KEY (handler_id)  REFERENCES user (user_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='举报表（支持完整处理闭环）';
-- ====================================================================================================
-- 表 22：user_punishment（用户处罚表）—— 管理员对违规用户实施的处罚记录
-- ====================================================================================================
-- | 字段名          | 类型         | 约束                                       | 默认值              | 说明 |
-- |-----------------|--------------|--------------------------------------------|---------------------|------|
-- | punishment_id   | BIGINT       | NOT NULL AUTO_INCREMENT, PRIMARY KEY        | —                   | 处罚记录ID |
-- | user_id         | BIGINT       | NOT NULL, FOREIGN KEY → user               | —                   | 被处罚用户ID |
-- | punishment_type | INT          | NOT NULL                                   | —                   | 处罚类型（0警告 1禁言 2封号） |
-- | reason          | VARCHAR(500) | DEFAULT NULL                               | NULL                | 处罚原因 |
-- | operator_id     | BIGINT       | NOT NULL, FOREIGN KEY → user               | —                   | 操作人用户ID |
-- | duration_days   | INT          | NOT NULL                                   | 0                   | 处罚持续天数（0表示永久） |
-- | created_at      | DATETIME     | NOT NULL                                   | CURRENT_TIMESTAMP   | 处罚开始时间 |
-- | expire_at       | DATETIME     | DEFAULT NULL                               | NULL                | 处罚到期时间 |
CREATE TABLE user_punishment (
    punishment_id   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '处罚记录ID（主键，自增）',
    user_id         BIGINT       NOT NULL                COMMENT '被处罚用户ID（外键，关联user表）',
    punishment_type INT          NOT NULL                COMMENT '处罚类型（0警告 1禁言 2封号）',
    reason          VARCHAR(500) DEFAULT NULL            COMMENT '处罚原因描述',
    operator_id     BIGINT       NOT NULL                COMMENT '操作人用户ID（外键，关联user表，即执行处罚的管理员）',
    duration_days   INT          NOT NULL DEFAULT 0      COMMENT '处罚持续天数（0表示永久）',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '处罚开始时间',
    expire_at       DATETIME     DEFAULT NULL            COMMENT '处罚到期时间（NULL表示永久）',
    PRIMARY KEY (punishment_id),
    KEY idx_user_id   (user_id),
    KEY idx_expire_at (expire_at),
    KEY idx_operator_id (operator_id),
    CONSTRAINT fk_up_user     FOREIGN KEY (user_id)     REFERENCES user (user_id) ON DELETE CASCADE,
    CONSTRAINT fk_up_operator FOREIGN KEY (operator_id) REFERENCES user (user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户处罚表';
-- ====================================================================================================
-- 表 23：user_behavior（用户行为表）—— 记录用户的关键操作日志，用于行为分析与风控
-- ====================================================================================================
-- | 字段名        | 类型     | 约束                                       | 默认值              | 说明 |
-- |---------------|----------|--------------------------------------------|---------------------|------|
-- | behavior_id   | BIGINT   | NOT NULL AUTO_INCREMENT, PRIMARY KEY        | —                   | 行为记录ID |
-- | user_id       | BIGINT   | NOT NULL, FOREIGN KEY → user               | —                   | 用户ID |
-- | behavior_type | INT      | NOT NULL                                   | —                   | 行为类型（0浏览 1点赞 2收藏 3分享 4举报） |
-- | target_id     | BIGINT   | DEFAULT NULL                               | NULL                | 行为关联的目标ID（如帖子ID、评论ID） |
-- | created_at    | DATETIME | NOT NULL                                   | CURRENT_TIMESTAMP   | 行为发生时间 |
CREATE TABLE user_behavior (
    behavior_id   BIGINT   NOT NULL AUTO_INCREMENT COMMENT '行为记录ID（主键，自增）',
    user_id       BIGINT   NOT NULL                COMMENT '用户ID（外键，关联user表）',
    behavior_type INT      NOT NULL                COMMENT '行为类型（0浏览 1点赞 2收藏 3分享 4举报）',
    target_id     BIGINT   DEFAULT NULL            COMMENT '行为关联的目标ID（如帖子ID、评论ID）',
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '行为发生时间',
    PRIMARY KEY (behavior_id),
    KEY idx_user_id            (user_id),
    KEY idx_user_behavior_time (user_id, behavior_type, created_at),
    CONSTRAINT fk_ub_user FOREIGN KEY (user_id) REFERENCES user (user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户行为表（操作日志）';
-- ====================================================================================================
-- 表 24：post_collect（帖子收藏表）—— 用户收藏帖子的关系（*新增*）
-- ====================================================================================================
-- | 字段名      | 类型     | 约束                                            | 默认值              | 说明 |
-- |-------------|----------|-------------------------------------------------|---------------------|------|
-- | collect_id  | BIGINT   | NOT NULL AUTO_INCREMENT, PRIMARY KEY             | —                   | 收藏记录ID |
-- | user_id     | BIGINT   | NOT NULL, FOREIGN KEY → user                    | —                   | 收藏用户ID |
-- | post_id     | BIGINT   | NOT NULL, FOREIGN KEY → post                    | —                   | 被收藏帖子ID |
-- | created_at  | DATETIME | NOT NULL                                       | CURRENT_TIMESTAMP   | 收藏时间 |
CREATE TABLE post_collect (
    collect_id BIGINT   NOT NULL AUTO_INCREMENT COMMENT '收藏记录ID（主键，自增）',
    user_id    BIGINT   NOT NULL                COMMENT '收藏用户ID（外键，关联user表）',
    post_id    BIGINT   NOT NULL                COMMENT '被收藏帖子ID（外键，关联post表）',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    PRIMARY KEY (collect_id),
    UNIQUE KEY uk_user_post (user_id, post_id),
    KEY idx_post_id (post_id),
    CONSTRAINT fk_pc_user FOREIGN KEY (user_id) REFERENCES user (user_id) ON DELETE CASCADE,
    CONSTRAINT fk_pc_post FOREIGN KEY (post_id) REFERENCES post (post_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='帖子收藏表（同一用户对同一帖子仅可收藏一次）';
-- ====================================================================================================
-- 表 25：notification（消息通知表）—— 系统消息推送，支持多种通知类型（*新增*）
-- ====================================================================================================
-- | 字段名          | 类型         | 约束                                       | 默认值              | 说明 |
-- |-----------------|--------------|--------------------------------------------|---------------------|------|
-- | notification_id | BIGINT       | NOT NULL AUTO_INCREMENT, PRIMARY KEY        | —                   | 通知ID |
-- | user_id         | BIGINT       | NOT NULL, FOREIGN KEY → user               | —                   | 通知接收用户ID |
-- | notify_type     | INT          | NOT NULL                                   | —                   | 通知类型（0系统通知 1点赞 2评论 3关注 4私信 5举报结果） |
-- | title           | VARCHAR(200) | NOT NULL                                   | —                   | 通知标题 |
-- | content         | TEXT         | NOT NULL                                   | —                   | 通知正文 |
-- | target_type     | INT          | DEFAULT NULL                               | NULL                | 关联目标类型（0帖子 1评论 2用户） |
-- | target_id       | BIGINT       | DEFAULT NULL                               | NULL                | 关联目标ID（用于点击通知跳转） |
-- | is_read         | TINYINT(1)   | NOT NULL                                   | 0                   | 是否已读（0未读 1已读） |
-- | created_at      | DATETIME     | NOT NULL                                   | CURRENT_TIMESTAMP   | 通知时间 |
CREATE TABLE notification (
    notification_id BIGINT       NOT NULL AUTO_INCREMENT COMMENT '通知ID（主键，自增）',
    user_id         BIGINT       NOT NULL                COMMENT '通知接收用户ID（外键，关联user表）',
    notify_type     INT          NOT NULL                COMMENT '通知类型（0系统通知 1点赞 2评论 3关注 4私信 5举报结果）',
    title           VARCHAR(200) NOT NULL                COMMENT '通知标题',
    content         TEXT         NOT NULL                COMMENT '通知正文内容',
    target_type     INT          DEFAULT NULL            COMMENT '关联目标类型（0帖子 1评论 2用户）',
    target_id       BIGINT       DEFAULT NULL            COMMENT '关联目标ID（点击通知跳转到目标页面）',
    is_read         TINYINT(1)   NOT NULL DEFAULT 0      COMMENT '是否已读（0未读 1已读）',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '通知时间',
    PRIMARY KEY (notification_id),
    KEY idx_user_id        (user_id),
    KEY idx_user_read      (user_id, is_read),
    KEY idx_created_at     (created_at),
    CONSTRAINT fk_notif_user FOREIGN KEY (user_id) REFERENCES user (user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息通知表（新增，支持多类型消息推送）';
