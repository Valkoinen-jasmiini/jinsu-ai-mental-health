# 瑾肃AI心理健康助手

> 一个基于 Vue 3 + Spring Boot 的全栈 AI 心理健康咨询平台，提供 AI 情绪对话、情绪日记追踪、心理知识库和数据分析等功能。

## 项目简介

瑾肃AI心理健康助手是一个面向用户的智能心理健康服务平台。用户可以与 AI 助手进行流式对话获取心理支持，记录每日情绪日记并通过可视化日历追踪情绪变化，浏览心理科普知识库，管理员可通过数据分析面板查看平台运营情况。

## 技术栈

### 前端 (`ai-vue/`)

| 技术 | 说明 |
|------|------|
| Vue 3 | 组合式 API (Composition API) |
| Vite | 快速构建工具 |
| Element Plus | UI 组件库 |
| Vue Router 4 | 路由管理 + 权限守卫 |
| Axios | HTTP 请求封装 |
| SCSS | 样式预处理 |

### 后端 (`ai-springboot/`)

| 技术 | 说明 |
|------|------|
| Spring Boot 3.4 | 后端框架 |
| MyBatis-Plus 3.5 | ORM 框架 |
| MySQL 8.0 | 数据库 |
| Spring Security | 认证与授权 |
| JWT | 无状态令牌认证 |
| WebFlux (SSE) | 流式 AI 对话 |
| SiliconFlow API | 大模型推理服务 |

## 功能模块

### 用户端
- **AI 心理咨询** — 流式对话、情绪花园实时分析、会话管理（新建/重命名/删除）
- **情绪日记** — 日历可视化、心情评分(1-10)、触发因素记录、月度统计
- **知识库** — 分类浏览、全文搜索、文章收藏、阅读历史追踪
- **个人中心** — 资料修改、密码修改、个人统计（咨询次数/日记/收藏/阅读）

### 管理端
- **数据分析** — 用户数/会话数/日记数统计、30天趋势图表、管理员权限控制

## 项目结构

```
jinsu-ai-mental-health/
├── ai-vue/                      # 前端项目
│   ├── src/
│   │   ├── api/                 # API 请求封装
│   │   ├── assets/              # 静态资源
│   │   ├── components/           # 公共组件 (侧边栏/导航栏/布局)
│   │   ├── composables/         # 组合式函数
│   │   ├── router/              # 路由配置 + 权限守卫
│   │   ├── utils/               # Axios 请求拦截器
│   │   ├── views/               # 页面组件
│   │   │   ├── home.vue         # 首页
│   │   │   ├── consultation.vue # AI咨询
│   │   │   ├── emotional.vue    # 情绪日记
│   │   │   ├── knowledge.vue    # 知识库
│   │   │   ├── dashboard.vue    # 数据分析(管理员)
│   │   │   ├── profile.vue      # 个人中心
│   │   │   ├── login.vue        # 登录
│   │   │   └── register.vue     # 注册
│   │   ├── App.vue
│   │   └── main.js
│   ├── vite.config.js
│   └── package.json
│
├── ai-springboot/               # 后端项目
│   ├── src/main/java/org/example/aispringboot/
│   │   ├── AiService/           # AI 对话 & 情绪分析
│   │   ├── Controller/          # REST 控制器
│   │   ├── service/             # 业务逻辑层
│   │   ├── mapper/              # MyBatis-Plus Mapper
│   │   ├── entity/              # 实体类
│   │   ├── DTO/                 # 数据传输对象
│   │   ├── config/              # Spring Security / JWT / CORS
│   │   ├── common/              # 统一响应 & 异常处理
│   │   └── util/                # JWT 工具类
│   ├── src/main/resources/
│   │   ├── application.yml          # 配置文件(需自行填写)
│   │   └── application-example.yml # 配置模板
│   └── pom.xml
│
├── .gitignore
└── README.md
```

## 快速开始

### 环境要求

- Node.js >= 18
- Java 21
- MySQL 8.0+
- Maven 3.9+

### 1. 克隆项目

```bash
git clone https://github.com/你的用户名/jinsu-ai-mental-health.git
cd jinsu-ai-mental-health
```

### 2. 配置数据库

```bash
mysql -u root -p
```

```sql
CREATE DATABASE mental_health_assistant DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 3. 启动后端

```bash
cd ai-springboot

# 复制配置模板并填入你的配置
cp src/main/resources/application-example.yml src/main/resources/application.yml
# 编辑 application.yml，填入数据库密码、SiliconFlow API Key 等

# 编译运行
./mvnw spring-boot:run
```

后端启动在 `http://localhost:1236`

### 4. 启动前端

```bash
cd ai-vue

# 安装依赖
npm install

# 开发模式
npm run dev
```

前端启动在 `http://localhost:5173`

### 5. 测试账号

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 普通用户 | test901 | 123456 |
| 管理员 | 需自行注册后修改 user_type=2 | - |

## 核心特性

### 流式 AI 对话
使用 SSE (Server-Sent Events) 实现流式输出，用户发送消息后 AI 回复逐字显示，体验自然流畅。后端通过 Spring WebFlux 的 `Flux<ServerSentEvent>` 推送流式数据。

### 情绪花园
AI 对话过程中实时分析用户情绪，生成情绪评分(0-100)、情绪标签和个性化建议，以可视化环形进度条展示。

### 权限控制
- 前端：Vue Router 路由守卫，`requiresAdmin` meta 字段控制页面访问
- 后端：Spring Security + JWT，`PUBLIC_PATHS` 白名单 + `user_type` 角色校验

## License

MIT
