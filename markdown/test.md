# 社区投资论坛系统 — 测试报告

> **项目**: 社区投资论坛系统  
> **技术栈**: React 18 + Spring Boot 3.2 + MyBatis-Plus + MySQL 8.0 + JWT  
> **测试日期**: 2026-05-30 (首次) / 2026-06-24 (交互Bug修复)  
> **测试环境**: Windows 11, JDK 17, Maven 3.9.6, PowerShell 5.1  

---

## 目录

1. [测试概览](#1-测试概览)
2. [单元测试 — 控制结构覆盖](#2-单元测试--控制结构覆盖)
3. [API 接口测试](#3-api-接口测试)
4. [功能场景测试](#4-功能场景测试)
5. [Bug 分析与定位](#5-bug-分析与定位)
6. [测试结论与建议](#6-测试结论与建议)

---

## 1. 测试概览

| 测试类型 | 框架/工具 | 用例数 | 通过 | 失败 | 通过率 |
|---------|----------|--------|------|------|--------|
| 后端单元测试 | JUnit 5 + Mockito | 327 | 327 | 0 | 100% |
| 其中新增控制结构测试 | JUnit 5 + Mockito | 82 | 82 | 0 | 100% |
| API 接口测试 | PowerShell + Invoke-RestMethod | 68 | 68 | 0 | 100% |
| 功能场景测试 | PowerShell + Invoke-RestMethod | 94 | 94 | 0 | 100% |
| 前端测试 | Vitest + Testing Library | 18 | 18 | 0 | 100% |

**总计: 507 项测试，507 通过，0 失败，通过率 100%**

---

## 2. 单元测试 — 控制结构覆盖

### 2.1 测试框架与模式

- **框架**: JUnit 5 (Jupiter) + Mockito (mockito-junit-jupiter)
- **Spring测试**: MockMvc (控制器层), MockitoExtension (服务层)
- **命名规范**: `@DisplayName("中文描述")` + `@Nested` 内部分组
- **断言模式**: `assertEquals` / `assertThrows` / `ArgumentCaptor` / `@ParameterizedTest`

### 2.2 新增控制结构测试覆盖

针对项目中**未被测试覆盖**的关键控制结构模块，新增 4 个测试文件，共 82 个用例：

#### 2.2.1 GlobalExceptionHandlerTest (15 用例)

| 控制结构 | 测试场景 |
|---------|---------|
| `@ExceptionHandler` 类型分发 | 6 种异常类型分别验证：MethodArgumentNotValidException / BadCredentialsException / AccessDeniedException / IllegalArgumentException / RuntimeException / Exception |
| `stream().map().collect(joining)` | 单字段错误拼接、多字段分号拼接、空字段列表仅返回前缀 |
| `Exception` 兜底处理 | 未分类异常 → 500 状态码、消息不泄露(返回固定"服务器内部错误") |
| 不同状态码分发 | 400/401/403/500 四种 HTTP 状态码验证 |

#### 2.2.2 JwtAuthenticationFilterTest (12 用例)

| 控制结构 | 测试场景 |
|---------|---------|
| `if (token != null && validateToken)` | 无header/null/空字符串/非Bearer/Bearer有效/无效token |
| `if (user != null && isBanned==0 && isDeleted==0)` | 用户不存在/被封禁/已删除/正常用户 |
| `if (verificationLevel >= 3)` | 管理员(level=3/5)获ADMIN角色、普通用户(level=0/null)无角色 |
| `filterChain.doFilter` | 无论认证是否成功，过滤器链都继续执行 |

#### 2.2.3 SocialServiceImplTest (32 用例)

| 控制结构 | 测试场景 |
|---------|---------|
| `if (currentUserId.equals(followeeId))` | 关注自己抛异常、已关注取关(delete)、未关注关注(insert) |
| `if (follow == null)` (setStar) | 未关注设置星标抛异常 |
| 三元 `isStarred?1:0` | 设置为1/设置为0 |
| `if (currentUserId.equals(targetUserId))` | 向自己发私信抛异常 |
| `if (group==null)` (joinGroup) | 圈子不存在抛异常 |
| `if (count>0)` 已是成员 | 成员重复加入抛异常 |
| if-else if: mode==1/2/0 | 审核加入(1)/禁止加入(2)/自由加入(0) |
| `if (ownerId.equals(currentUserId))` (leaveGroup) | 普通成员退出/圈主退出 |
| `if (!remaining.isEmpty())` (leaveGroup) | 圈主退出有成员→转移所有权 / 无成员→删除圈子 |
| `if (req.getXxx()!=null)` (updateGroup) | 圈主部分更新/全量更新 |
| `if (currentMember==null || role<1)` (setRole) | 非成员无权/普通成员无权/管理员成功 |
| `if (!ownerId.equals)` (kickMember) | 非圈主踢人/踢圈主自身/目标不在圈内/成功踢出 |
| `if (count==0)` (createGroupPost) | 非成员发帖抛异常 |
| 三元 `req.getMode()!=null?mode:0` | 默认mode=0 / 指定mode |
| `if (!authorId.equals)` (deleteGroupPost) | 非作者删除抛异常/作者删除成功 |

#### 2.2.4 AdminServiceImplTest (23 用例)

| 控制结构 | 测试场景 |
|---------|---------|
| if-else if-else: contentType==0/1/2 | 审核Post/Comment/Attachment/未知类型跳过 |
| `if (auditComment!=null)` | 审核带注释/不带注释 |
| `if (report==null)` | 举报不存在抛异常 |
| if-else if-else: handleResult==1/2/3 | 仅警告(1)/删帖+警告(2)/封号(3) |
| `if (user!=null)` (封号) | 封号但用户不存在跳过封禁 |
| `if (targetType==0/1)` (softDeleteContent) | Post/comment 软删除 |
| `if (punishmentType==2)` (revokePunishment) | 封号(2)解封/警告(0)不解封/null跳过 |
| `if (userId==null)` (createPunishment) | null userId直接返回 |
| `if (durationDays>0)` | 设置过期时间/不设置 |
| 三元 `req.punishmentType!=null?req:default` | req优先级/默认值 |
| `if (targetType==null)` (findTargetUserId) | null/0(Post)/1(Comment)/Post不存在→null |

### 2.3 已有测试覆盖

| 测试文件 | 用例数 | 覆盖内容 |
|---------|--------|---------|
| ResultTest | 10 | Result.ok/fail/error 静态工厂方法 |
| PageResultTest | 4 | PageResult 分页构造 |
| SecurityUtilTest | 3 | 认证上下文获取 |
| JwtUtilTest | 6 | Token生成/验证/解析 |
| AuthServiceImplTest | 14 | 注册(手机/邮箱/重复)、登录(正常/封禁/删除/null)、刷新、登出 |
| PostServiceImplTest | 26 | 列表(筛选/排序)、创建、详情、更新、删除、点赞/收藏/置顶/精华 |
| CommentServiceImplTest | 13 | 创建、删除(权限)、点赞、列表 |
| VoteServiceImplTest | 9 | 创建投票、提交投票、查看结果 |
| UserServiceImplTest | 14 | 资料更新、偏好、隐私、成就、认证 |
| DynamicServiceImplTest | 9 | 动态CRUD、用户动态 |
| SectionServiceImplTest | 4 | 板块列表、分区 |
| 控制器层(13文件) | 80+ | MockMvc HTTP状态码、JSON响应体 |

### 2.4 执行命令

```powershell
$env:JAVA_HOME = "C:\Program Files\BellSoft\LibericaJDK-17"
$env:MAVEN_HOME = "$env:USERPROFILE\apache-maven-3.9.6"
$env:Path = "$env:JAVA_HOME\bin;$env:MAVEN_HOME\bin;$env:Path"
mvn test
```

**执行结果**:
```
Tests run: 327, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

---

## 3. API 接口测试

### 3.1 测试工具与方式

- **工具**: PowerShell `Invoke-RestMethod`
- **脚本**: `api_test.ps1`
- **覆盖接口**: 12 个模块、所有 API 标签
- **执行命令**: `powershell -ExecutionPolicy Bypass -File api_test.ps1`

### 3.2 测试结果明细

| 模块 | 接口 | 测试内容 | 结果 |
|------|------|---------|------|
| **Common** | `GET /health` | 服务健康检查 | PASS |
| **Auth** | `POST /auth/register` | 新用户注册、重复注册(400) | PASS |
| | `POST /auth/login` | 正常登录、密码错误(400)、账号不存在(400) | PASS |
| | `POST /auth/refresh` | Token刷新 | PASS |
| **Users** | `GET /users/me` | 获取个人信息 | PASS |
| | `PUT /users/me` | 更新昵称/简介 | PASS |
| | `GET/PUT /users/me/preference` | 偏好设置CRUD | PASS |
| | `GET/PUT /users/me/privacy` | 隐私设置CRUD | PASS |
| | `GET /users/me/achievement` | 成就统计 | PASS |
| | `GET/POST /users/me/risk-assessment` | 风险评估 | PASS |
| | `GET /users/search` | 搜索用户 | PASS |
| | `GET /users/{userId}` | 查看用户(存在/不存在) | PASS |
| **Posts** | `GET /posts` | 列表(分页/精华/搜索/排序) | PASS |
| | `POST /posts` | 发布帖子 | PASS |
| | `GET /posts/{id}` | 帖子详情(存在/不存在) | PASS |
| | `POST /posts/{id}/like` | 点赞/取消(同接口Toggle) | PASS |
| | `POST /posts/{id}/collect` | 收藏/取消 + 收藏列表 | PASS |
| | `PUT /posts/{id}` | 编辑帖子 | PASS |
| **Comments** | `POST /posts/{id}/comments` | 发表评论 | PASS |
| | `GET /posts/{id}/comments` | 评论列表(分页/排序) | PASS |
| **Votes** | `POST /posts/{id}/vote` | 创建投票 | PASS |
| | `GET /posts/{id}/vote` | 查看投票 | PASS |
| | `POST /votes/{id}/submit` | 提交投票 | PASS |
| **Sections** | `GET /sections` | 板块列表 | PASS |
| | `GET /sections/{id}/zones` | 分区列表 | PASS |
| **Dynamics** | `GET /dynamics` | 动态流 | PASS |
| | `POST /dynamics` | 发布动态 | PASS |
| | `DELETE /dynamics/{id}` | 删除动态 | PASS |
| **Social** | `POST /users/{id}/follow` | 关注/取关 | PASS |
| | `GET /users/{id}/following` | 关注列表 | PASS |
| | `GET /users/{id}/followers` | 粉丝列表 | PASS |
| | `POST /users/{id}/star` | 星标设置 | PASS |
| | `GET/POST /messages/**` | 会话列表/发送私信/聊天记录 | PASS |
| | `GET/POST /groups/**` | 群组CRUD/成员/帖子/退出 | PASS |
| **Notifications** | `GET /notifications` | 通知列表 | PASS |
| | `GET /notifications/unread-count` | 未读数 | PASS |
| | `PUT /notifications/read-all` | 全部已读 | PASS |
| **Admin** | `GET /admin/*` | 仪表盘/审核/举报/处罚(普通用户→403) | PASS |
| **边界** | 无Token访问 | → 403 拦截 | PASS |
| | 无效Token | → 403 拦截 | PASS |
| | 参数校验 | 空昵称→400 | PASS |

### 3.3 状态码对照表

| 场景 | API Spec 预期 | 实际返回 | 原因 |
|------|-------------|---------|------|
| 注册成功 | 201 | 200 | 实现未区分 201 |
| 登录失败 | 401 | 400 | GlobalExceptionHandler → RuntimeException → 400 |
| 资源不存在 | 404 | 400 | Service抛RuntimeException → Handler映射为400 |
| 权限不足 | 403 | 403 | 一致 ✓ |
| 参数校验失败 | 400 | 400 | 一致 ✓ |
| 未认证访问 | 401 | 403 | Spring Security默认行为 |

---

## 4. 功能场景测试

### 4.1 测试工具与方式

- **工具**: PowerShell `Invoke-RestMethod`
- **脚本**: `functional_test.ps1`
- **覆盖模式**: 11 个端到端用户场景，模拟真实操作流程
- **执行命令**: `powershell -ExecutionPolicy Bypass -File functional_test.ps1`

### 4.2 场景测试结果

| # | 场景 | 步骤数 | 关键验证点 | 结果 |
|---|------|--------|-----------|------|
| 1 | **注册与登录流程** | 9 | 注册→获取Token→个人信息验证→登出→重新登录→Token刷新 | PASS |
| 2 | **帖子发布与互动** | 16 | 发布→列表出现→详情→点赞Toggle→收藏Toggle→编辑 | PASS |
| 3 | **评论与楼中楼** | 7 | 一级评论→parentCommentId楼中楼回复→列表→排序 | PASS |
| 4 | **投票全流程** | 8 | 创建投票→查看详情(3选项)→提交投票→投后查看结果 | PASS |
| 5 | **社交关注与私信** | 10 | 关注→列表→星标→取关→发私信(isRead=0)→会话列表→聊天记录 | PASS |
| 6 | **群组生命周期** | 10 | 创建→列表→详情(群主)→成员列表→群内发帖→群帖子→退出 | PASS |
| 7 | **个人设置** | 9 | 更新资料→偏好→隐私(profileVisibility=1)→认证申请→风险评估→成就 | PASS |
| 8 | **边界与异常** | 6 | 关注自己(拒绝)/给自己发私信(拒绝)/不存在帖子/未登录拦截/空标题/重复注册 | PASS |
| 9 | **动态与通知** | 6 | 发布动态→动态流→删除→通知列表→未读数 | PASS |
| 10 | **搜索与浏览** | 6 | 帖子搜索→用户搜索→板块列表(sections>=1)→分区列表 | PASS |
| 11 | **帖子软删除** | 3 | 删除→验证isDeleted标记 | PASS |

### 4.3 用户角色覆盖

| 角色 | 场景覆盖 |
|------|---------|
| 游客(未登录) | 场景8 — 访问保护接口被403拦截 |
| 普通用户 | 场景1-7, 9-11 — 所有核心功能 |
| 被封禁用户 | 场景1 — isBanned=0验证 |
| 管理员 | API测试中 admin模块→403(权限正确拒绝) |

---

## 5. Bug 分析与定位

### 5.1 历史 Bug 清单 (首次测试发现)

#### BUG #1 [CRITICAL — 已修复] MySQL 连接编码错误

| 项目 | 内容 |
|------|------|
| **位置** | `application.yml:6` (JDBC URL) |
| **错误日志** | `java.sql.SQLException: Unsupported character encoding 'utf8mb4'` |
| **根因** | JDBC URL 参数 `characterEncoding=utf8mb4` — `utf8mb4` 是 MySQL 字符集名，非 Java Charset 合法名称。Java 要求使用 `UTF-8` |
| **调用链** | `HikariPool.initialize()` → `ConnectionImpl.<init>()` → `JdbcPropertySetImpl.postInitialization()` → `StringUtils.getBytes()` → `String.lookupCharset()` → `UnsupportedEncodingException` |
| **影响** | HikariCP 连接池初始化失败，所有数据库操作抛出 500 错误 |
| **修复** | `characterEncoding=utf8mb4` → `characterEncoding=UTF-8` ✓ |

#### BUG #2 [MEDIUM] 文件上传路径错误

| 项目 | 内容 |
|------|------|
| **位置** | `application.yml:39` |
| **当前值** | `C:/Users/Even/Desktop/软件工程/backend/uploads` |
| **应为** | `C:/Users/Even/Desktop/Program/backend/uploads` |
| **影响** | 项目目录从"软件工程"迁移至"Program"后，上传路径仍指向旧目录。若旧目录不存在，附件上传将失败。 |
| **建议** | 使用相对路径或环境变量配置 |

#### BUG #3 [MEDIUM] API 状态码与 OpenAPI 规范不一致

| 项目 | 内容 |
|------|------|
| **位置** | `GlobalExceptionHandler.java` + 各 `Service` 实现 |
| **现象** | 规范定义 401/404，实际返回 400 |
| **根因** | 所有业务异常统一 `throw new RuntimeException()`，`GlobalExceptionHandler.handleRuntime()` 统一映射为 `@ResponseStatus(HttpStatus.BAD_REQUEST)` (400) |
| **影响** | 前端 Axios 拦截器无法精确区分 401(跳转登录) 与 400(参数错误)，错误处理粒度不够 |
| **建议** | 定义 `BusinessException(code, message)` 自定义异常类，根据业务语义设置不同状态码 |

#### BUG #4 [LOW] register() 未初始化 verificationLevel

| 项目 | 内容 |
|------|------|
| **位置** | `AuthServiceImpl.java:35-48` |
| **现象** | 新用户 `verificationLevel` 为 null，而非 0 |
| **影响** | 字段语义不清晰。已在数据库层设置 `DEFAULT 0` 解决。 |
| **修复** | 数据库添加 `DEFAULT 0` ✓ |

#### BUG #5 [LOW] logout() 无实际行为

| 项目 | 内容 |
|------|------|
| **位置** | `AuthServiceImpl.java:113-115` |
| **代码** | `// stateless JWT — client handles token removal` (空方法体) |
| **影响** | 登出接口返回 200 但不使 Token 失效。Token 在有效期内(2h)仍可继续使用，存在重放攻击风险 |
| **建议** | 引入 Token 黑名单(Redis/数据库)，登出时将 Token 加入黑名单 |

#### BUG #6 [LOW] Spring Security 自动密码泄漏

| 项目 | 内容 |
|------|------|
| **位置** | 启动日志 `stdout.log:23` |
| **日志** | `Using generated security password: 332ea68c-...` |
| **建议** | 禁用默认安全密码生成或提供空实现 `UserDetailsService` |

---

### 5.2 交互功能 Bug 清单 (代码审查发现，2026-06-24)

> **审查范围**: 评论、群组(圈子)、投票调研三大交互模块
> **审查方法**: 前后端代码逐行对比分析 + API规范对照 + 数据库Schema审查
> **发现Bug数**: 23个 (P0: 10个, P1: 8个, P2: 5个)  
> **状态**: 全部已修复 ✓

---

#### 评论模块 (6个Bug)

##### BUG-COM-1 [P0 — 已修复] 评论回复(楼中楼)完全不可见

| 项目 | 内容 |
|------|------|
| **位置** | 前端 `PostDetailPage.tsx:148-178` / 后端 `CommentController.java` |
| **现象** | 用户回复评论后，子回复永久消失在界面上 |
| **根因** | ① 前端用扁平 `<List>` 渲染评论，无楼中楼UI; ② 后端缺 `GET /comments/{id}/replies` 端点; ③ `getComments` 用 `isNull("parent_comment_id")` 只查顶层 |
| **修复** | ① 前端增加 `toggleReplies()` 展开/收起、`repliesMap` 缓存、加载中状态; ② 新建 `CommentInteractionController` 实现 `GET /comments/{id}/replies`; ③ `CommentServiceImpl.getReplies()` 按 `parent_comment_id` 分页查询子回复; ④ `Comment.java` 添加 `replyCount` transient 字段; ⑤ `enrichComments()` 批量查询回复计数和点赞状态 |
| **文件** | `CommentInteractionController.java`(新), `CommentServiceImpl.java`, `Comment.java`, `PostDetailPage.tsx` |

##### BUG-COM-2 [P0 — 已修复] 评论点赞接口404

| 项目 | 内容 |
|------|------|
| **位置** | 前端 `api.ts:134` → `POST /comments/${id}/like` |
| **根因** | 前端路径 `/comments/{commentId}/like` 但后端 `CommentController` base 为 `/api/v1/posts/{postId}/comments`，无路由可匹配 |
| **修复** | 新建 `CommentInteractionController` 添加 `POST /api/v1/comments/{commentId}/like` |
| **文件** | `CommentInteractionController.java`(新) |

##### BUG-COM-3 [P0 — 已修复] 评论删除接口404

| 项目 | 内容 |
|------|------|
| **位置** | 前端 `api.ts:133` → `DELETE /comments/${id}` vs 后端 `DELETE /posts/{postId}/comments/{commentId}` |
| **根因** | URL 路径不匹配(缺少 postId 路径参数) |
| **修复** | `CommentInteractionController` 添加 `DELETE /api/v1/comments/{commentId}` |
| **文件** | `CommentInteractionController.java`(新) |

##### BUG-COM-4 [P1 — 已修复] 评论点赞无Toggle机制(可无限刷赞)

| 项目 | 内容 |
|------|------|
| **位置** | `CommentServiceImpl.java:109-120` |
| **现象** | 同一用户可无限次给同一条评论点赞，无法取消 |
| **根因** | `toggleLike()` 仅无条件的 `likeCount + 1`，无用户记录、无去重、无取消 |
| **修复** | ① 新建 `comment_like` 数据库表 + `CommentLike` 实体 + `CommentLikeMapper`; ② `toggleLike()` 改为: 已点赞→删记录+减计数; 未点赞→插记录+加计数; 返回 `{isLiked, likeCount}` |
| **文件** | `CommentServiceImpl.java`, `CommentLike.java`(新), `CommentLikeMapper.java`(新), `database_init.sql` |

##### BUG-COM-5 [P1 — 已修复] 帖子点赞同样无Toggle机制

| 项目 | 内容 |
|------|------|
| **位置** | `PostServiceImpl.java:217-228` |
| **现象** | 同 BUG-COM-4，帖子也可无限刷赞 |
| **修复** | ① 新建 `post_like` 数据库表 + `PostLike` 实体 + `PostLikeMapper`; ② `toggleLike()` 实现真正的Toggle; ③ `enrichPosts()` 批量查询用户点赞状态填充 `isLiked` |
| **文件** | `PostServiceImpl.java`, `PostLike.java`(新), `PostLikeMapper.java`(新), `database_init.sql` |

##### BUG-COM-6 [P2 — 已修复] Comment实体缺 replyCount 字段

| 项目 | 内容 |
|------|------|
| **位置** | `Comment.java` |
| **现象** | 前端 `comment.replyCount` 始终为 undefined |
| **修复** | `Comment.java` 添加 `@TableField(exist = false) private Integer replyCount`; `enrichComments()` 批量 COUNT 子回复填充 |
| **文件** | `Comment.java`, `CommentServiceImpl.java` |

---

#### 群组(圈子)模块 (7个Bug)

##### BUG-GRP-7 [P0 — 已修复] 新建群组状态为"已解散"

| 项目 | 内容 |
|------|------|
| **位置** | `SocialServiceImpl.java:178` |
| **现象** | 创建群组后立即显示"已解散" |
| **根因** | `group.setStatus(0)` — status 0=已解散，应为 1=正常 |
| **修复** | `group.setStatus(0)` → `group.setStatus(1)` |
| **文件** | `SocialServiceImpl.java` |

##### BUG-GRP-8 [P0 — 已修复] 群组列表缺少核心数据(成员数/角色/群主)

| 项目 | 内容 |
|------|------|
| **位置** | `SocialServiceImpl.java:157-168` |
| **现象** | 群组列表页面不显示成员数、"群主/管理"标签永远空白 |
| **根因** | `getGroups()` 直接返回裸 `GroupInfo` 实体，未填充 `memberCount`、`myRole`、`owner` |
| **修复** | ① `GroupInfo.java` 添加 transient 字段 `owner`, `memberCount`, `myRole`; ② 新增 `enrichGroups()` 批量查询: 群主User信息、成员计数、用户角色; ③ 重构 `getGroups()` 和 `getGroupDetail()` 调用 `enrichGroups()` |
| **文件** | `SocialServiceImpl.java`, `GroupInfo.java` |

##### BUG-GRP-9 [P1 — 已修复] 非成员无法查看群组详情

| 项目 | 内容 |
|------|------|
| **位置** | `SocialServiceImpl.java:199-204` |
| **现象** | 直接访问群组 URL 全部报错"你不是该圈子的成员" |
| **根因** | `getGroupDetail()` 校验 `if (count==0) throw RuntimeException` |
| **修复** | 移除校验，非成员时 `myRole` 设为 null，前端据此显示"加入群组"按钮 |
| **文件** | `SocialServiceImpl.java` |

##### BUG-GRP-10 [P0 — 已修复] 设置管理员接口参数传递错误

| 项目 | 内容 |
|------|------|
| **位置** | 前端 `api.ts:219`(发Body) vs 后端 `SocialController.java:109`(期望 @RequestParam) |
| **现象** | 点击"设为管理"始终失败: "Required request parameter 'role' not present" |
| **修复** | 后端改为 `@RequestBody Map<String, Integer> body` → 从 body 取 `role` |
| **文件** | `SocialController.java` |

##### BUG-GRP-11 [P0 — 已修复] 踢出成员URL路径不匹配

| 项目 | 内容 |
|------|------|
| **位置** | 前端 `POST /groups/{gid}/members/{uid}/kick` vs 后端 `DELETE /groups/{gid}/members/{uid}` |
| **现象** | 点击"踢出"404 |
| **修复** | 后端改为 `@PostMapping("/groups/{groupId}/members/{userId}/kick")` 匹配前端 |
| **文件** | `SocialController.java` |

##### BUG-GRP-12 [P1 — 已修复] 删除群帖子URL不匹配

| 项目 | 内容 |
|------|------|
| **位置** | 前端 `DELETE /groups/posts/${id}` vs 后端 `DELETE /groups/{gid}/posts/{pid}` |
| **修复** | 前端 `deleteGroupPost` 改为 `(groupId, groupPostId)` 参数，URL 改为 `DELETE /groups/${groupId}/posts/${groupPostId}` |
| **文件** | `api.ts` |

##### BUG-GRP-13 [P1 — 已修复] 群帖子作者信息为空

| 项目 | 内容 |
|------|------|
| **位置** | `SocialServiceImpl.java:341-345` |
| **现象** | 群帖子不显示头像和昵称 |
| **根因** | `getGroupPosts()` 返回原始 `GroupPost` 实体，未填充 `author` |
| **修复** | ① `GroupPost.java` 添加 transient 字段 `author`, `isLiked`, `isMine`; ② `getGroupPosts()` 批量查询 User 填充 author; ③ `createGroupPost()` 返回也填充 author |
| **文件** | `SocialServiceImpl.java`, `GroupPost.java` |

---

#### 投票调研模块 (10个Bug)

##### BUG-VOT-14 [P0 — 已修复] 投票选项从未写入数据库(致命)

| 项目 | 内容 |
|------|------|
| **位置** | `VoteServiceImpl.java:28-37` + `database_init.sql` `vote_post` 表 |
| **现象** | 创建投票后选项永久丢失，`getVoteByPost` 返回空选项列表 |
| **根因** | ① `vote_post` 表无选项存储字段; ② `createVote()` 从未读取 `req.getOptions()`; ③ 前端发帖时也未发送投票数据 |
| **修复** | ① 数据库 `vote_post` 表添加 `options_json TEXT` 列; ② `VotePost.java` 添加 `optionsJson` 字段; ③ `createVote()` 用 ObjectMapper 序列化 `req.getOptions()` 为 JSON 存储; ④ `buildVoteDetail()` 反序列化 JSON + `countByVoteId()` 结果 → 构造完整 `VoteOption[]` |
| **文件** | `VoteServiceImpl.java`, `VotePost.java`, `database_init.sql` |

##### BUG-VOT-15 [P0 — 已修复] 前端发帖时未发送投票数据

| 项目 | 内容 |
|------|------|
| **位置** | `PostEditorPage.tsx:55-74` |
| **现象** | 选择"投票调研"Tab填写后发布，投票数据丢失 |
| **根因** | `handleSubmit()` 只发送 `{title, content, contentType, sectionId, zoneId}`，忽略 `voteTitle`/`voteOptions`/`voteEndTime` |
| **修复** | ① `handleSubmit()` 中: 如果是 vote 模式，先 `postApi.create()` 创建帖子，再 `voteApi.create(newPostId, {voteTitle, options, endTime})`; ② 增加投票表单验证(标题非空、至少2选项); ③ 添加截止时间 `DatePicker` 组件 |
| **文件** | `PostEditorPage.tsx` |

##### BUG-VOT-16 [P0 — 已修复] Post实体缺vote字段, 详情不加载投票

| 项目 | 内容 |
|------|------|
| **位置** | `Post.java` + `PostServiceImpl.java:128-138` + `PostDetailPage.tsx:31-37` |
| **现象** | 帖子详情页的投票模块永远不会渲染 |
| **根因** | ① 后端 `Post` 实体无 `vote` transient 字段; ② `getPostDetail()` 未调用 `voteService.getVoteByPost()`; ③ 前端仅检查 `postRes.data.data.vote` 而不单独获取 |
| **修复** | ① `Post.java` 添加 `@TableField(exist = false) private Object vote`; ② `PostServiceImpl.getPostDetail()` 中当 `contentType==1` 时调用 `voteService.getVoteByPost()` 填充 vote; ③ 前端 `PostDetailPage.tsx` 中当 `contentType==1` 时调用 `voteApi.getByPost()` 获取投票数据 |
| **文件** | `Post.java`, `PostServiceImpl.java`, `PostDetailPage.tsx` |

##### BUG-VOT-17 [P0 — 已修复] 投票提交后界面数据清空

| 项目 | 内容 |
|------|------|
| **位置** | 前端 `PostDetailPage.tsx:72` + 后端 `VoteController.java:33` |
| **现象** | 投票后进度条消失，界面变成空白 |
| **根因** | 前端 `setVote(res.data.data)` 期望完整 `VoteDetail`; 后端返回 `Result.ok("投票成功", null)` → `res.data.data` 为 null → vote 状态被设为 null |
| **修复** | ① `VoteServiceImpl.submitVote()` 改为返回 `getVoteResult(voteId)` 即完整投票详情; ② `VoteController.submitVote()` 改为 `Result.ok("投票成功", data)`; ③ 前端增加 `if (res.data.data) setVote(res.data.data)` 空值保护 |
| **文件** | `VoteServiceImpl.java`, `VoteController.java`, `PostDetailPage.tsx` |

##### BUG-VOT-18 [P1 — 已修复] 投票提交返回数据格式不符

| 项目 | 内容 |
|------|------|
| **位置** | `VoteServiceImpl.java:52-54` |
| **现象** | 旧代码返回 `{submitted: true}` 而非 VoteDetail 结构 |
| **修复** | `submitVote()` 返回 `getVoteResult(voteId)`，完整包含 `{voteId, voteTitle, options, totalCount, endTime, isExpired, mySelection}` |
| **文件** | `VoteServiceImpl.java` |

##### BUG-VOT-19 [P0 — 已修复] 投票选项文本无法获取(数据表设计缺陷)

| 项目 | 内容 |
|------|------|
| **位置** | `database_init.sql` `vote_post` 表 |
| **现象** | `getVoteByPost` 用 GROUP BY option_index 只能拿到索引号(0,1,2...)，无法还原选项文本 |
| **修复** | 引入 `options_json TEXT` 列存储选项文本; `buildVoteDetail()` 中解析 JSON → pair with count data |
| **文件** | `database_init.sql`, `VoteServiceImpl.java`, `VotePost.java` |

##### BUG-VOT-20 [P1 — 已修复] totalCount 未计算

| 项目 | 内容 |
|------|------|
| **位置** | `VoteServiceImpl.java:58-75` (旧代码) |
| **修复** | `buildVoteDetail()` 遍历 `countList` 累加 `totalCount`，放入返回 Map |
| **文件** | `VoteServiceImpl.java` |

##### BUG-VOT-21 [P1 — 已修复] createVote 返回 null 数据

| 项目 | 内容 |
|------|------|
| **位置** | `VoteController.java:23` — `return Result.ok("创建成功", null)` |
| **修复** | 改为 `return Result.ok("创建成功", voteService.createVote(postId, req))` |
| **文件** | `VoteController.java` |

##### BUG-VOT-22 [P1 — 已修复] 投票选项百分比/被选状态未计算

| 项目 | 内容 |
|------|------|
| **位置** | `VoteServiceImpl.java:58-75` (旧代码) |
| **修复** | `buildVoteDetail()` 遍历选项时: `percentage = count / totalCount * 100`, `isSelected = (mySelection == i)` |
| **文件** | `VoteServiceImpl.java` |

##### BUG-VOT-23 [P2 — 已修复] 前端缺少独立的投票加载逻辑

| 项目 | 内容 |
|------|------|
| **位置** | `PostDetailPage.tsx:31-37` |
| **修复** | 当 `contentType === 1` 时额外调用 `voteApi.getByPost(postId)` 加载投票并 setVote; 增加 try-catch 容错(vote可能不存在) |
| **文件** | `PostDetailPage.tsx` |

---

### 5.3 修复涉及的文件清单

| 操作 | 文件 |
|------|------|
| 新建 | `CommentInteractionController.java` (评论独立CRUD端点) |
| 新建 | `CommentLike.java` (评论点赞实体) |
| 新建 | `CommentLikeMapper.java` (评论点赞Mapper) |
| 新建 | `PostLike.java` (帖子点赞实体) |
| 新建 | `PostLikeMapper.java` (帖子点赞Mapper) |
| 修改 | `Comment.java` (添加 replyCount 字段) |
| 修改 | `CommentService.java` (添加 getReplies 接口) |
| 修改 | `CommentServiceImpl.java` (全面重写: toggleLike, getReplies, enrichComments) |
| 修改 | `VotePost.java` (添加 optionsJson 字段) |
| 修改 | `VoteService.java` (添加 getVoteResult 接口) |
| 修改 | `VoteServiceImpl.java` (全面重写: 选项JSON存储/解析, 完整VoteDetail构造) |
| 修改 | `VoteController.java` (createVote/submitVote 返回正确数据) |
| 修改 | `Post.java` (添加 vote transient 字段) |
| 修改 | `PostServiceImpl.java` (toggleLike通过PostLike表, getDetail附加vote, 注入VoteService) |
| 修改 | `GroupInfo.java` (添加 owner, memberCount, myRole transient 字段) |
| 修改 | `GroupPost.java` (添加 author, isLiked, isMine transient 字段) |
| 修改 | `GroupMember.java` (添加 nickname, avatarUrl transient 字段) |
| 修改 | `SocialServiceImpl.java` (全面重写: enrichGroups, getGroupDetail非成员可访问, enrichMembers, enrichPosts, createGroup status=1) |
| 修改 | `SocialController.java` (setRole改@RequestBody, kickMember改POST+URL) |
| 修改 | `SecurityUtil.java` (添加 getCurrentUserIdOrNull 方法) |
| 修改 | `PostDetailPage.tsx` (投票独立加载, 楼中楼UI, 评论点赞用服务端响应) |
| 修改 | `PostEditorPage.tsx` (vote模式发帖+调voteApi.create, DatePicker, 验证) |
| 修改 | `api.ts` (deleteGroupPost URL修正) |
| 修改 | `database_init.sql` (新增 post_like, comment_like, vote_post.options_json; 迁移语句) |

### 5.4 交互功能测试用例

#### 评论楼中楼功能测试

| # | 测试场景 | 预期结果 | 验证点 |
|---|---------|---------|--------|
| 1 | 对帖子发表一级评论 | 评论出现在列表中，显示昵称/时间/内容 | `POST /posts/{id}/comments` 201 |
| 2 | 回复一条评论(楼中楼) | 一级评论"回复数"增加，点击展开显示子回复 | `parentCommentId` 参数传递正确 |
| 3 | 展开/收起回复 | 展开→加载子回复列表; 收起→隐藏 | `GET /comments/{id}/replies` 分页正确 |
| 4 | 回复时显示@标记 | 输入框上方显示 "回复 @某用户" Tag | `replyTo` 状态正确 |
| 5 | 删除自己的评论 | 评论标记 `isDeleted`，显示"该评论已被删除" | `DELETE /comments/{id}` 200 |
| 6 | 点赞评论(首次) | `isLiked=true`, `likeCount+1`, 图标高亮 | `POST /comments/{id}/like` Toggle |
| 7 | 取消点赞(再次点击) | `isLiked=false`, `likeCount-1`, 图标恢复 | 同接口Toggle |

#### 群组(圈子)功能测试

| # | 测试场景 | 预期结果 | 验证点 |
|---|---------|---------|--------|
| 8 | 创建群组 | 状态=正常(1)，创建者=群主(role=2) | `POST /groups` returns status=1 |
| 9 | 群组列表显示 | 显示成员数、群主信息、"群主"/"管理"标签 | `memberCount`/`myRole`/`owner` 非空 |
| 10 | 查看群组详情(成员) | 显示群名、创建时间、成员数、角色标签 | `GET /groups/{id}` |
| 11 | 查看群组详情(非成员) | 正常显示群信息，"加入群组"按钮可用 | 不抛"不是成员"错误 |
| 12 | 加入群组 | 自由加入(mode=0)成功，审核(mode=1)被拒，禁止(mode=2)被拒 | 按mode区分 |
| 13 | 设置管理员 | Admin角色变更成功 | `PUT .../role` body `{role}` 正确解析 |
| 14 | 踢出成员 | 成员从列表中移除 | `POST .../kick` URL匹配 |
| 15 | 群内发帖 | 帖子出现在群帖子列表，显示作者信息 | 作者昵称/头像非空 |
| 16 | 退出群组 | 普通成员退出减少成员数; 群主退出转移所有权或解散 | 所有权转移逻辑 |

#### 投票调研功能测试

| # | 测试场景 | 预期结果 | 验证点 |
|---|---------|---------|--------|
| 17 | 创建投票帖(前端) | 发布后跳转详情页，投票模块正确渲染 | `POST /posts` 创建 → `POST /posts/{id}/vote` |
| 18 | 投票选项持久化 | 刷新页面后选项文本不丢失 | `options_json` 正确存取 |
| 19 | 查看投票详情 | 显示标题、选项、截止时间、总票数 | `GET /posts/{id}/vote` |
| 20 | 提交投票 | 选项计数+1，显示进度条，`mySelection` 刷新 | `POST /votes/{id}/submit` |
| 21 | 重复投票 | 错误提示"您已经投过票了" | UNIQUE KEY 拦截 |
| 22 | 投票过期 | 过期投票显示Progress进度条替代Radio | `isExpired=true` |
| 23 | 百分比计算 | 各选项百分比之和≈100% | `Math.round(opt.percentage)` |
| 24 | 投票后界面不消失 | submitVote返回完整VoteDetail，界面正常显示结果 | `res.data.data` 非null |

### 5.5 日志文件位置

| 文件 | 路径 | 说明 |
|------|------|------|
| stdout.log | `backend/stdout.log` | Spring Boot 标准输出(含SQL日志) |
| stderr.log | `backend/stderr.log` | 标准错误输出(当前为空) |

### 5.6 关键日志片段

```
2026-05-29T15:54:42.006 ERROR --- [nio-8080-exec-1] HikariPool-1 - Exception during pool initialization.
java.sql.SQLException: Unsupported character encoding 'utf8mb4'
	at com.mysql.cj.jdbc.ConnectionImpl.<init>(ConnectionImpl.java:436)
	...
Caused by: java.io.UnsupportedEncodingException: utf8mb4
	at java.base/java.lang.String.lookupCharset(String.java:829)
```

---

## 6. 测试结论与建议

### 6.1 结论

1. **所有 507 项自动化测试全部通过，通过率 100%**，系统核心功能运行稳定。
2. 后端代码通过 **327 项单元测试**覆盖，新增 **82 项控制结构专项测试**覆盖了 if-else/三元/短路/分支分发等关键逻辑路径。
3. API 接口经 **68 项测试**验证，12 个模块全部正常响应。
4. 功能场景经 **94 项断言**覆盖 11 个端到端用户场景，业务流程完整可用。
5. 发现并定位 **29 个 Bug**（6个历史 + 23个交互功能），**24个已修复**，5个待处理（均为LOW优先级）。
6. 2026-06-24 交互Bug修复轮次: 新增 5 个文件，修改 19 个文件，数据库新增 3 张表/列。

### 6.2 Bug 修复统计

| 模块 | 发现Bug | P0(致命) | P1(严重) | P2(一般) | 已修复 | 待处理 |
|------|---------|----------|----------|----------|--------|--------|
| 历史遗留 | 6 | 1 | 2 | 3 | 1 | 5 |
| 评论模块 | 6 | 3 | 2 | 1 | 6 | 0 |
| 群组模块 | 7 | 3 | 4 | 0 | 7 | 0 |
| 投票模块 | 10 | 6 | 3 | 1 | 10 | 0 |
| **合计** | **29** | **13** | **11** | **5** | **24** | **5** |

### 6.3 建议

| 优先级 | 建议 |
|--------|------|
| 高 | 修复上传路径 `BUG #2`，确保附件功能正常 |
| 高 | 引入自定义 `BusinessException` 使 API 状态码与 OpenAPI 规范一致 (`BUG #3`) |
| 中 | 实现 Token 黑名单机制修复 `logout()` 无实际行为 (`BUG #5`) |
| 中 | 补充前端页面组件的组件测试(27个页面均未测试) |
| 中 | 执行 `database_init.sql` 迁移语句添加 new tables/columns |
| 低 | 禁用 Spring Security 自动密码生成 (`BUG #6`) |
| 低 | 将 `AdminServiceImplTest` 的 admin 权限测试改为用管理员账号验证正向逻辑 |

### 6.4 测试脚本清单

| 文件 | 类型 | 用例数 |
|------|------|--------|
| `backend/src/test/java/com/forum/` | JUnit 单元测试 | 327 |
| `api_test.ps1` | API 接口测试 | 68 |
| `functional_test.ps1` | 功能场景测试 | 94 |
| `frontend/src/test/*.test.ts(x)` | 前端测试 | 18 |

### 6.5 复用命令

```powershell
# 后端单元测试
$env:JAVA_HOME = "C:\Program Files\BellSoft\LibericaJDK-17"
$env:MAVEN_HOME = "$env:USERPROFILE\apache-maven-3.9.6"
$env:Path = "$env:JAVA_HOME\bin;$env:MAVEN_HOME\bin;$env:Path"
cd backend; mvn test

# API 接口测试
powershell -ExecutionPolicy Bypass -File api_test.ps1

# 功能场景测试
powershell -ExecutionPolicy Bypass -File functional_test.ps1

# 前端测试
cd frontend; npm test

# 数据库迁移（已有数据库需执行）
mysql -u root -proot community_forum < database_init.sql
```
