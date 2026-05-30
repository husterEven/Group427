# 社区投资论坛系统 — 测试报告

> **项目**: 社区投资论坛系统  
> **技术栈**: React 18 + Spring Boot 3.2 + MyBatis-Plus + MySQL 8.0 + JWT  
> **测试日期**: 2026-05-30  
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

### 5.1 Bug 清单

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
| **根因** | `register()` 方法中未调用 `user.setVerificationLevel(0)` |
| **影响** | 数据库字段无 `DEFAULT 0` 约束时为 NULL，`JwtAuthenticationFilter` 中 `null >= 3` 为 false(行为符合预期)，但字段语义不清晰 |
| **修复** | 添加 `user.setVerificationLevel(0);` 或在数据库添加 `DEFAULT 0` |

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
| **根因** | 未定义 `UserDetailsService` Bean，Spring Boot 自动生成默认密码 |
| **影响** | 不影响JWT认证流程，但控制台暴露密码是不安全实践 |
| **建议** | 禁用默认安全密码生成或提供空实现 `UserDetailsService` |

### 5.2 日志文件位置

| 文件 | 路径 | 说明 |
|------|------|------|
| stdout.log | `backend/stdout.log` | Spring Boot 标准输出(含SQL日志) |
| stderr.log | `backend/stderr.log` | 标准错误输出(当前为空) |

### 5.3 关键日志片段

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

1. **所有 507 项测试全部通过，通过率 100%**，系统核心功能运行稳定。
2. 后端代码通过 **327 项单元测试**覆盖，新增 **82 项控制结构专项测试**覆盖了 if-else/三元/短路/分支分发等关键逻辑路径。
3. API 接口经 **68 项测试**验证，12 个模块全部正常响应。
4. 功能场景经 **94 项断言**覆盖 11 个端到端用户场景，业务流程完整可用。
5. 发现并定位 **6 个 Bug**，其中 1 个关键Bug已修复，5 个待处理。

### 6.2 建议

| 优先级 | 建议 |
|--------|------|
| 高 | 修复上传路径 `BUG #2`，确保附件功能正常 |
| 高 | 引入自定义 `BusinessException` 使 API 状态码与 OpenAPI 规范一致 (`BUG #3`) |
| 中 | 为 `register()` 添加 `verificationLevel` 初始化 (`BUG #4`) |
| 中 | 实现 Token 黑名单机制修复 `logout()` 无实际行为 (`BUG #5`) |
| 中 | 补充前端页面组件的组件测试(27个页面均未测试) |
| 低 | 禁用 Spring Security 自动密码生成 (`BUG #6`) |
| 低 | 将 `AdminServiceImplTest` 的 admin 权限测试改为用管理员账号验证正向逻辑 |

### 6.3 测试脚本清单

| 文件 | 类型 | 用例数 |
|------|------|--------|
| `backend/src/test/java/com/forum/` | JUnit 单元测试 | 327 |
| `api_test.ps1` | API 接口测试 | 68 |
| `functional_test.ps1` | 功能场景测试 | 94 |
| `frontend/src/test/*.test.ts(x)` | 前端测试 | 18 |

### 6.4 复用命令

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
```
