# 社区投资论坛系统

> **技术栈**：React 18 + Spring Boot 3.2 + MyBatis-Plus + MySQL 8.0 + JWT  
> **端口**：前端 3000 / 后端 8080 / MySQL 3306

---

## 一、项目架构

```
 Program/
├── frontend/                 # 前端 (React + TypeScript + Ant Design 5)
│   ├── src/
│   │   ├── components/       # 布局组件 (MainLayout, AdminLayout, AuthGuard)
│   │   ├── pages/            # 27 个页面 + admin/ 子目录 5 个管理页
│   │   ├── router/           # React Router v6 路由配置
│   │   ├── services/         # Axios API 封装 (request/response 拦截器)
│   │   ├── store/            # Zustand 状态管理 (auth + app)
│   │   └── types/            # TypeScript 类型定义
│   └── vite.config.ts        # Vite 构建 + 代理 (/api → localhost:8080)
│
├── backend/                  # 后端 (Spring Boot + MyBatis-Plus)
│   └── src/main/java/com/forum/
│       ├── config/           # 安全/JWT/CORS/MyBatisPlus 配置
│       ├── common/           # Result/PageResult/异常处理/SecurityUtil
│       ├── entity/           # 24 个实体类 (对应数据库表)
│       ├── dto/              # 26 个请求/响应 DTO
│       ├── mapper/           # 25 个 MyBatis-Plus Mapper 接口
│       ├── service/          # 10 个业务接口 + 11 个实现
│       └── controller/       # 12 个 REST 控制器
│
├── database_init.sql         # 数据库建库建表脚本 (25 张表)
├── api_spec.yaml             # OpenAPI 3.0 接口文档
└── UI设计文档.md             # 前端 UI 设计说明
```

---

## 二、环境要求

| 软件 | 版本 | 本机路径 |
|------|------|---------|
| JDK | 17+ | `C:\Program Files\BellSoft\LibericaJDK-17` |
| Maven | 3.9+ | `C:\Users\Even\apache-maven-3.9.6` |
| MySQL | 8.0+ | 已安装，密码 `root` |
| Node.js | 18+ | 需自行安装 |

---

## 三、启动步骤

### 3.1 一键启动（推荐）

```powershell
# 自动检测环境 → 初始化数据库 → 编译 → 启动前后端 → 打开浏览器
.\start-all.ps1
```

可选参数：

| 参数 | 说明 |
|------|------|
| `-SkipBuild` | 跳过 Maven 编译（使用已有 jar） |
| `-SkipDB` | 跳过数据库初始化 |
| `-BackendOnly` | 仅启动后端 |
| `-FrontendOnly` | 仅启动前端 |

### 3.2 分别启动

```powershell
# 后端（编译 + 启动，8080 端口）
.\start-backend.ps1

# 前端（安装依赖 + 启动，3000 端口）
.\start-frontend.ps1
```

### 3.3 停止服务

```powershell
.\stop-all.ps1
```

### 3.4 手动验证

```powershell
curl http://localhost:8080/api/v1/health
# → {"code":200,"message":"success","data":{"status":"UP","version":"1.0.0"}}

# 浏览器访问
start http://localhost:3000
```

### 3.5 数据库初始化（仅首次/重建）

```powershell
& "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -proot community_forum < database_init.sql
```

---

## 四、使用指南

### 4.1 注册与登录

1. 打开 `http://localhost:3000/register` 注册账号
2. 手机号/邮箱均可注册（昵称 2-20 字，密码 6-32 位）
3. 注册成功后自动登录跳转首页，也可访问 `/login` 登录

### 4.2 首页浏览

- 顶部搜索框支持搜索帖子标题和正文
- 左侧板块导航：A股/港股/美股/基金/期货 等分区
- 排序方式：最新 / 最热 / 最受欢迎
- 点击帖子标题进入详情页

### 4.3 发帖

点击右上角 **[+发帖]** 按钮，支持三种模式：

| 模式 | 说明 |
|------|------|
| 普通发帖 | 10 行文本框，短内容分享 |
| 长文分析 | 20 行文本框，深度分析 |
| 投票调研 | 设置投票标题 + 2~10 个选项 + 截止时间 |

### 4.4 帖子互动

- **点赞**：点击 👍 按钮（Toggle 模式）
- **收藏**：点击 ⭐ 按钮，可在个人中心查看收藏列表
- **评论**：支持楼中楼回复（点击评论的"回复"按钮）
- **投票**：在投票帖中单选提交，未截止显示选项，已截止显示柱状图

### 4.5 用户中心

点击右上角头像 → 个人主页 `/profile/:userId`：

| 功能 | 路径 |
|------|------|
| 编辑资料 | `/settings/profile` |
| 投资偏好 | `/settings/preference` |
| 隐私设置 | `/settings/privacy` |
| 认证中心 | `/settings/verification` |
| 成就系统 | `/settings/achievement` |
| 风险评估 | `/settings/risk` |

### 4.6 社交模块

| 功能 | 路径 | 说明 |
|------|------|------|
| 实时动态 | `/dynamics` | 类似微博/朋友圈的短动态 |
| 关注/粉丝 | `/following` | 关注与被关注管理 |
| 私信 | `/messages` | 一对一聊天 |
| 群组 | `/groups` | 创建群组、群内发帖、成员管理 |
| 通知 | `/notifications` | 点赞/评论/关注/系统通知 |

### 4.7 管理后台（管理员）

访问 `/admin/dashboard`（需要 `verification_level >= 3`）：

| 模块 | 路径 | 功能 |
|------|------|------|
| 数据大盘 | `/admin/dashboard` | 用户/帖子/评论统计 |
| 内容审核 | `/admin/audit` | 审核帖子/评论/附件 |
| 举报处理 | `/admin/reports` | 处理用户举报 |
| 处罚管理 | `/admin/punishments` | 查看/撤销处罚 |
| 用户监控 | `/admin/users` | 搜索用户行为日志 |

---

## 五、API 接口速查

所有接口前缀：`http://localhost:8080/api/v1`

### 认证（公开）
```
POST /auth/register    注册
POST /auth/login       登录
POST /auth/refresh     刷新 Token
POST /auth/logout      登出
```

### 用户（需登录）
```
GET    /users/me                   个人信息
PUT    /users/me                   更新资料
PUT    /users/me/password          修改密码
GET    /users/me/preference        偏好设置
GET    /users/me/privacy           隐私设置
GET    /users/me/achievement       成就统计
POST   /users/me/risk-assessment   提交风险评估
POST   /users/me/verification      提交认证申请
GET    /users/search?keyword=      搜索用户
GET    /users/{userId}             查看用户主页
```

### 帖子
```
GET    /posts                      帖子列表 (?sort=latest|hot|popular)
POST   /posts                      发布帖子
GET    /posts/{postId}             帖子详情
PUT    /posts/{postId}             编辑帖子
DELETE /posts/{postId}             删除帖子
POST   /posts/{postId}/like        点赞/取消
POST   /posts/{postId}/collect     收藏/取消
GET    /posts/collections          我的收藏
PUT    /posts/{postId}/pin         置顶/取消（管理员）
PUT    /posts/{postId}/essence     精华/取消（管理员）
```

### 评论 & 投票
```
GET    /posts/{postId}/comments        评论列表
POST   /posts/{postId}/comments        发表评论
GET    /posts/{postId}/vote            查看投票
POST   /posts/{postId}/vote            创建投票
POST   /votes/{voteId}/submit          提交投票
```

### 动态 & 社交
```
GET    /dynamics                     动态流
GET    /follow/{userId}/following    关注列表
POST   /follow/{userId}/toggle       关注/取消
GET    /messages                     会话列表
GET    /groups                       群组列表
```

### 管理（需 ADMIN 权限）
```
GET    /admin/dashboard    数据大盘
GET    /admin/audit        审核队列
GET    /admin/reports      举报列表
```

完整 API 文档见 `api_spec.yaml`（OpenAPI 3.0 格式，可用 Swagger Editor 打开）。

---

## 六、认证说明

- 使用 JWT 双 Token：`accessToken`（2 小时）+ `refreshToken`（7 天）
- 请求头：`Authorization: Bearer <accessToken>`
- 401 时前端自动跳转登录页
- 管理员：`verificationLevel >= 3` 的用户

---

## 七、数据库配置

配置文件：`backend/src/main/resources/application.yml`

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/community_forum
    username: root
    password: root     # 修改为你的 MySQL 密码
```

---

## 八、常见问题

| 问题 | 解决 |
|------|------|
| 端口 8080 被占用 | 修改 `application.yml` 中 `server.port` 和前端 `vite.config.ts` 的代理目标 |
| MySQL 连接失败 | 检查 MySQL 服务是否运行：`Get-Service MySQL*` |
| 前端 npm 命令无效 | 需安装 Node.js (https://nodejs.org) |
| 注册提示"账号已存在" | 直接登录或用新手机号/邮箱注册 |
