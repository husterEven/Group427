# 社区投资论坛 — 安装部署文档

## 一、项目概述

社区投资论坛系统是一套面向投资者的在线交流平台，支持帖子发布、评论互动、投票调研、板块导航、私信聊天、群组社交等完整功能。

| 项目 | 说明 |
|------|------|
| 技术栈 | React 18 + Spring Boot 3.2 + MyBatis-Plus 3.5.5 + MySQL 8.0 |
| 认证方案 | JWT 双 Token（Access 2h + Refresh 7d） |
| 前端端口 | **3000** (Vite 开发服务器) |
| 后端端口 | **8080** (Spring Boot 嵌入式 Tomcat) |
| 数据库端口 | **3306** (MySQL) |
| 开发语言 | TypeScript 5.5 + Java 17 |

---

## 二、环境要求

### 2.1 硬件要求

| 资源 | 最低配置 | 推荐配置 |
|------|---------|---------|
| CPU | 2 核 | 4 核 |
| 内存 | 4 GB | 8 GB |
| 磁盘 | 2 GB 可用空间 | 5 GB SSD |

### 2.2 软件依赖

| 软件 | 最低版本 | 说明 | 下载地址 |
|------|---------|------|---------|
| JDK | 17 | 后端运行环境 | https://bell-sw.com/pages/downloads/ (LibericaJDK) |
| Maven | 3.9+ | 后端构建工具 | https://maven.apache.org/download.cgi |
| MySQL | 8.0 | 关系型数据库 | https://dev.mysql.com/downloads/mysql/ |
| Node.js | 18+ | 前端运行环境 | https://nodejs.org/ |
| Git | 任意版本 | 版本控制（可选） | https://git-scm.com/ |

---

## 三、快速安装

### 3.1 获取源码

```bash
# 方式一：直接拷贝项目文件夹
# 将整个 Program/ 目录复制到目标机器

# 方式二：Git 克隆（如果已托管）
git clone <仓库地址>
cd Program
```

### 3.2 安装 MySQL 并初始化数据库

**步骤 1：启动 MySQL 服务**

```powershell
# 检查 MySQL 是否运行
Get-Service -Name "MySQL80"

# 如果未运行，启动服务
Start-Service -Name "MySQL80"
```

**步骤 2：创建数据库并导入表结构**

```powershell
# 使用 MySQL 客户端执行初始化脚本
& "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -p < database_init.sql
# 提示输入密码时输入你的 MySQL root 密码
```

> database_init.sql 将自动创建 `community_forum` 数据库及 27 张业务表。

**步骤 3：配置数据库连接**

编辑 `backend/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/community_forum?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: root
    password: 你的MySQL密码   # 修改此处
```

### 3.3 安装前端依赖

```powershell
cd frontend
npm install
```

> 如果 npm 下载缓慢，可设置国内镜像：
> `npm config set registry https://registry.npmmirror.com`

### 3.4 编译后端

```powershell
cd backend
mvn clean compile -DskipTests
```

---

## 四、启动服务

### 4.1 一键启动（推荐）

```powershell
# 项目根目录执行
.\start.bat
```

或直接运行 PowerShell 脚本：

```powershell
.\start.ps1
```

该脚本将依次：
1. 检查并启动 MySQL 服务
2. 编译并启动 Spring Boot 后端（最小化窗口）
3. 等待后端就绪（最长 60 秒）
4. 启动 Vite 前端开发服务器（最小化窗口）
5. 自动打开浏览器访问 `http://localhost:3000`

### 4.2 分别启动

**后端（终端 1）：**

```powershell
cd backend
mvn spring-boot:run
```

> 后端启动后，可通过 `http://localhost:8080/api/v1/health` 验证就绪。

**前端（终端 2）：**

```powershell
cd frontend
npm run dev
```

### 4.3 停止服务

```powershell
# 项目根目录执行
.\stop.bat
```

或手动终止进程：

```powershell
# 停止后端 (端口 8080)
netstat -ano | findstr ":8080" | findstr "LISTENING"
taskkill /PID <进程ID> /F

# 停止前端 (端口 3000)
netstat -ano | findstr ":3000" | findstr "LISTENING"
taskkill /PID <进程ID> /F
```

---

## 五、验证安装

### 5.1 后端健康检查

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/health"
```

正常响应：
```json
{
  "code": 200,
  "message": "success",
  "data": { "status": "UP", "version": "1.0.0" }
}
```

### 5.2 前端页面

浏览器访问 `http://localhost:3000`，应看到论坛首页。

### 5.3 注册测试账号

1. 访问 `http://localhost:3000/register`
2. 填写手机号/邮箱、昵称、密码
3. 注册成功后自动登录并跳转首页

---

## 六、生产部署

### 6.1 前端构建

```powershell
cd frontend
npm run build
```

生成的静态文件在 `frontend/dist/` 目录，可部署到 Nginx 等 Web 服务器。

### 6.2 后端打包

```powershell
cd backend
mvn clean package -DskipTests
```

生成的 JAR 包在 `backend/target/community-forum-1.0.0.jar`。

启动方式：

```bash
java -jar community-forum-1.0.0.jar --spring.profiles.active=prod
```

### 6.3 Nginx 配置示例

```nginx
server {
    listen 80;
    server_name your-domain.com;

    # 前端静态文件
    root /path/to/frontend/dist;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    # API 反向代理
    location /api/ {
        proxy_pass http://localhost:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

---

## 七、常见问题

| 问题 | 解决方案 |
|------|---------|
| **端口 8080 被占用** | `netstat -ano \| findstr 8080` 查看占用进程，修改 `application.yml` 中 `server.port` |
| **端口 3000 被占用** | 在 `frontend/vite.config.ts` 中修改 `server.port` |
| **MySQL 连接失败** | 检查 MySQL 服务是否运行：`Get-Service MySQL*`；确认 `application.yml` 中用户名密码正确 |
| **Maven 编译失败** | 确认 JDK 17 已安装并配置 `JAVA_HOME` 环境变量 |
| **npm install 失败** | 删除 `node_modules` 和 `package-lock.json` 后重试；切换到国内镜像源 |
| **前端 401 错误** | Token 已过期，重新登录即可 |
| **数据库导入失败** | 确认 MySQL 已启动且用户有 CREATE 权限 |
| **注册提示"账号已存在"** | 换用新手机号/邮箱注册，或直接登录 |

---

## 八、项目结构

```
Program/
├── frontend/                    # 前端（React + TypeScript + Ant Design）
│   ├── src/
│   │   ├── components/          # 布局组件
│   │   ├── pages/               # 页面组件（27 个页面）
│   │   ├── router/              # React Router 路由配置
│   │   ├── services/            # Axios API 封装
│   │   ├── store/               # Zustand 状态管理
│   │   └── types/               # TypeScript 类型定义
│   ├── package.json
│   └── vite.config.ts
├── backend/                     # 后端（Spring Boot + MyBatis-Plus）
│   ├── src/main/java/com/forum/
│   │   ├── config/              # 安全/JWT/CORS 配置
│   │   ├── common/              # 通用工具类
│   │   ├── controller/          # REST 控制器（12 个）
│   │   ├── service/             # 业务逻辑层
│   │   ├── mapper/              # MyBatis-Plus Mapper 接口（25 个）
│   │   ├── entity/              # 数据库实体类（27 个）
│   │   └── dto/                 # 数据传输对象（26 个）
│   ├── pom.xml
│   └── src/main/resources/application.yml
├── database_init.sql            # 数据库初始化脚本（27 张表）
├── api_spec.yaml                # OpenAPI 3.0 接口文档
├── start.bat / start.ps1        # 一键启动脚本
├── stop.bat / stop.ps1          # 停止服务脚本
└── uploads/                     # 文件上传目录
```
