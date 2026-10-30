# 声析 VOXINSIGHT

面向大学生和求职者的表达训练原型。学员选题并用麦克风录音，系统根据语音识别计算语速、停顿和流畅度，并对照题目要点给出内容分数，用于观察自己的变化与提升。

Slogan：让每一次表达，都有迹可循。

课程：《软件开发实践》。

## 原型链接

平台：墨刀

链接地址：[https://modao.cc/proto/design/pb2mv0a15xduvidj4](https://modao.cc/proto/design/pb2mv0a15xduvidj4)

## 设计思路

产品按「选题 → 录音 → 报告 → 回顾」组织，页面和表一一对应：首页看本周进度，题库选题，录音页完成一次训练，报告页看这一次，历史和个人中心看累计变化。

评分拆成两条分支：

- **表达分**：ffmpeg 把录音转成 16 kHz 单声道，Vosk 中文小模型识别文字。语速是识别出的汉字（含数字）除以音频分钟数。停顿是波形里超过 0.4 秒的句中静音。流畅度以约 170 字/分钟为参照，语速偏离和停顿都会扣分。没有识别出汉字时，语速、流畅度和总分都是 0。报告标注「语音识别结果」。
- **内容分**：对照该题 `topic_checkpoints` 里的要点。配置了 `DASHSCOPE_API_KEY` 时，由阿里云百炼判断识别文本是否覆盖这些要点；未配置或调用失败时，回退为关键词命中，覆盖比例即分数。结构建议仍按题目分类给出固定文案，不改写学员的回答。

同一题再次练习时，只有上一份「语音识别结果」才会用来比较停顿是否减少。

录音存在 `session_audios`（最大 8MB），不放进训练主表，避免首页和历史查询带出二进制。刷新或再次进入同一题时，可以提交已保存的草稿，或新录一段由服务端接到已有录音后面，也可以放弃重录。

账号、收藏、周目标和设置都写入数据库。关闭「保留训练历史」只是从首页、历史和个人统计里隐藏，重新打开会恢复；「清空训练记录」才会永久删除。通知只出现在首页，没有邮件或手机推送。

## 功能描述

### 用户端

移动端和桌面端调用同一组接口。

| 页面 | 路由 | 行为 |
| --- | --- | --- |
| 登录 | `/login` | 登录、注册。未登录访问其他页面会回到这里 |
| 首页 | `/` | 品牌、本周进度、最近一次成绩、推荐题、站内通知 |
| 题库 | `/topics` | 按分类和标题筛选，收藏题目 |
| 录音训练 | `/practice/:topicId` | 波形跟随麦克风音量；暂停、继续、结束、放弃重录；刷新后可接着处理已保存的录音 |
| 分析报告 | `/report/:reportId` | 总分、语速、停顿、流畅度、内容分、识别文本、反馈文案、回放录音 |
| 训练历史 | `/history` | 已完成记录，以及暂停、失败的训练。关闭历史保留时显示已隐藏 |
| 个人中心 | `/profile` | 资料和头像、累计统计、收藏、周目标（每周 1–7 次）、提醒与隐私、反馈、关于、退出 |

新报告按服务器当天计入本周进度，一周从周一开始。不在当前自然周时，首页进度分子是 0。累计次数和首页进度只统计已完成训练。

### 管理后台

管理员登录后可以：

- 查看用户数、题目数、训练数和待处理反馈数
- 搜索用户，修改资料或角色，删除用户
- 新建分类；新建、编辑、删除题目及其内容要点
- 按状态查看训练，打开详情和录音，删除记录
- 查看用户反馈并回复，状态为「待处理」或「已回复」

## 项目结构

`node_modules/`、`backend/target/`、`backend/.env` 和 `backend/models/` 是本地依赖或密钥，不作为源码提交。

```
VOXINSIGHT/
├── README.md                      本文
├── voxinsight.sql                 建库脚本：删库重建，含表、视图和种子数据
├── migrate-session-audio.sql      已有库补录音表
├── migrate-speech-score.sql       已有库补识别文本和内容分字段
├── migrate-accounts.sql           已有库补账号、令牌、通知、反馈
├── migrate-session-state.sql      已有库补训练状态流转
├── migrate-admin.sql              已有库补管理员角色和反馈回复
├── frontend/                      移动端原型（端口 5173）
├── web/                           桌面学员端（端口 5174）
├── admin/                         管理后台（端口 5175）
└── backend/                       Spring Boot 接口（端口 8080）
```

全新环境只执行 `voxinsight.sql`。已经导入过旧库、又不想清空数据时，再按上面顺序执行对应的 `migrate-*.sql`。某条语句提示列已存在，说明这一步做过，跳过该句即可。

### 数据库 `voxinsight.sql`

| 对象 | 作用 |
| --- | --- |
| `app_meta` | 品牌名、Slogan、关于文案。`as_of_date` 只保留演示日期，不参与统计 |
| `users` | 账号、角色（`user` / `admin`）、资料、头像二进制 |
| `training_goals` | 每周练习目标，首页进度的分母 |
| `notification_settings` | 练习提醒、报告通知、周目标通知和提醒时间 |
| `privacy_settings` | 是否保留训练历史、是否参与匿名统计 |
| `categories` / `topics` | 题目分类和题目。页面上的「全部」是筛选条件，不入库 |
| `topic_checkpoints` | 题目内容要点和逗号分隔的关键词 |
| `user_favorites` | 收藏，同一用户对同一题只保留一条 |
| `training_sessions` | 一次训练。状态：`pending`、`recording`、`paused`、`analyzing`、`completed`、`failed` |
| `session_audios` | 该次训练的录音 |
| `analysis_reports` | 表达分、语速、停顿、流畅度、识别文本、内容分 |
| `report_feedbacks` | 表现较好、可改进、改进建议，一条一行 |
| `help_articles` | 帮助文章 |
| `user_feedbacks` | 用户反馈和管理员回复 |
| `auth_tokens` | 登录令牌 |
| `user_notices` | 站内通知 |
| `v_user_dashboard` | 首页进度和最近成绩 |
| `v_user_training_stats` | 累计次数、时长、平均分 |
| `v_training_history` | 历史列表 |

### `frontend/` 移动端原型

Vue 3 + Vite + Vue Router + Pinia。页面画在手机框 `PhoneFrame` 里，底部是 `TabBar`。

| 文件 | 作用 |
| --- | --- |
| `package.json` | 依赖与脚本。`dev` / `build` / `preview` |
| `vite.config.js` | 开发服务器，把 `/api` 代理到 `8080`，默认端口 5173 |
| `src/main.js` | 应用入口 |
| `src/App.vue` | 根组件 |
| `src/router/index.js` | 路由和登录拦截 |
| `src/api/http.js` | 请求封装，附带 `Authorization: Bearer` |
| `src/stores/app.js` | 题库、设置等前端状态 |
| `src/views/LoginView.vue` | 登录、注册 |
| `src/views/HomeView.vue` | 首页 |
| `src/views/TopicsView.vue` | 题库 |
| `src/views/PracticeView.vue` | 录音训练 |
| `src/views/ReportView.vue` | 分析报告 |
| `src/views/HistoryView.vue` | 训练历史 |
| `src/views/ProfileView.vue` | 个人中心 |
| `src/components/PhoneFrame.vue` | 手机框 |
| `src/components/TabBar.vue` | 底部导航 |
| `src/components/PageHeader.vue` | 页头 |
| `src/utils/topic.js` | 题目展示用的小工具 |
| `src/data/demo.js` | 早期静态数据。现有页面走接口，主流程不读它 |

`AboutView.vue`、`HelloWorld.vue`、`TheWelcome.vue`、`WelcomeItem.vue`、`stores/counter.js`、`components/icons/*` 是 Vue 脚手架残留，路由没有引用。`frontend/README.md` 仍是 Vite 模板说明，以本文为准。

### `web/` 桌面学员端

Vue 3 + Vite + Vue Router，不用 Pinia。页面与移动端对应，外层换成侧栏 `Shell.vue`。

| 文件 | 作用 |
| --- | --- |
| `package.json` | 依赖与脚本 |
| `vite.config.js` | 固定端口 5174，代理 `/api` |
| `src/main.js` / `src/App.vue` | 入口 |
| `src/router.js` | 路由和登录拦截 |
| `src/api.js` | 请求封装 |
| `src/views/Shell.vue` | 侧栏和退出 |
| `src/views/LoginView.vue` | 登录、注册 |
| `src/views/HomeView.vue` | 首页 |
| `src/views/TopicsView.vue` | 题库 |
| `src/views/PracticeView.vue` | 录音训练 |
| `src/views/ReportView.vue` | 分析报告 |
| `src/views/HistoryView.vue` | 训练历史 |
| `src/views/ProfileView.vue` | 个人中心 |

### `admin/` 管理后台

Vue 3 + Vite + Vue Router。

| 文件 | 作用 |
| --- | --- |
| `package.json` | 依赖与脚本 |
| `vite.config.js` | 固定端口 5175，代理 `/api` |
| `src/main.js` / `src/App.vue` | 入口 |
| `src/router.js` | 路由和登录拦截 |
| `src/api.js` | 请求封装 |
| `src/views/LoginView.vue` | 管理员登录 |
| `src/views/Shell.vue` | 后台侧栏 |
| `src/views/OverviewView.vue` | 概览 |
| `src/views/UsersView.vue` | 学员管理 |
| `src/views/TopicsView.vue` | 题目列表 |
| `src/views/TopicFormView.vue` | 新建、编辑题目和要点 |
| `src/views/SessionsView.vue` | 训练记录和录音 |
| `src/views/FeedbackView.vue` | 反馈处理 |

### `backend/` 接口服务

Spring Boot 3.5、Java 21、MyBatis。入口是 `src/main/java/com/voxinsight/VoxinsightApplication.java`。

| 路径 | 作用 |
| --- | --- |
| `pom.xml` | Maven 依赖：Web、Validation、MyBatis、MySQL、Vosk |
| `.env.example` | 数据库和百炼密钥的示例。复制为 `.env` 后填写 |
| `src/main/resources/application.yml` | 端口 8080、数据源、上传上限 8MB、Vosk 模型路径、百炼地址 |
| `src/main/resources/mapper/*.xml` | SQL。表结构以根目录 `voxinsight.sql` 为准 |
| `config/WebConfig.java` | 允许 5173、5174、5175 以及 `*.trycloudflare.com` 跨域 |
| `config/AppProperties.java` | 读取模型路径和百炼配置 |
| `web/*Controller.java` | HTTP 接口，见下一节 |
| `web/AuthFilter.java` | 校验 `Authorization: Bearer`，管理接口再校验角色 |
| `web/ApiExceptionHandler.java` | 错误统一成 `{"message":"..."}` |
| `service/AuthService.java` | 注册、登录、退出。密码用 PBKDF2 |
| `service/SessionService.java` | 训练状态、录音草稿、结束并出报告 |
| `service/SpeechRecognizer.java` | 调用 Vosk |
| `service/PauseDetector.java` | 统计静音段 |
| `service/AudioDecoder.java` | 调用本机 ffmpeg 转 PCM |
| `service/AudioJoiner.java` | 把续录片段接到已有录音后面 |
| `service/AudioUpload.java` | 校验音频类型和大小 |
| `service/SpeechAssessment.java` | 表达分和四条反馈文案 |
| `service/ContentAssessment.java` | 关键词内容分 |
| `service/BailianContentJudge.java` | 百炼内容评价，失败则回退关键词 |
| `service/HomeService.java` 等 | 首页、题库、报告、历史、资料、帮助、通知 |
| `service/AdminService.java` | 管理后台业务 |
| `dto/` | 请求和响应字段 |
| `mapper/` | MyBatis 接口 |
| `src/test/java/` | 评分、要点、状态流转、停顿、密码散列、音频拼接的单元测试 |

模型目录是 `backend/models/vosk-model-small-cn-0.22`，需要自行放入，不进 Git。

## API 接口说明

基址：`http://127.0.0.1:8080`。除健康检查、登录、注册外，请求头都要带：

```http
Authorization: Bearer <token>
```

失败时 HTTP 状态为 400、401、403 或 404，正文是 `{"message":"说明"}`。成功删除类接口没有正文。

### 公开接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/health` | 检查服务和数据库。成功：`{"status":"ok","database":"ok"}` |
| `POST` | `/api/auth/login` | 登录 |
| `POST` | `/api/auth/register` | 注册 |

登录请求：

```json
{ "loginName": "youke", "password": "youke123" }
```

注册请求：`loginName` 3–32 字，`password` 6–32 字，`nickname` 可选、最长 32 字。

登录和注册都返回：

```json
{ "token": "...", "loginName": "youke", "nickname": "游客", "role": "user" }
```

`POST /api/auth/logout` 需要登录，使当前令牌失效。

### 首页、题库、帮助

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/home` | 品牌、昵称、本周完成数和目标、最近成绩、上一份分数、推荐题、未读通知 |
| `POST` | `/api/notices/{id}/read` | 把一条通知标为已读 |
| `GET` | `/api/topics?category=&keyword=` | 分类列表和题目。两个查询参数都可省略 |
| `POST` | `/api/topics/{id}/favorite` | 切换收藏。返回 `{ "topicId", "favorite" }` |
| `GET` | `/api/help` | 帮助文章，以及关于声析的品牌和版本文案 |

题目卡片字段：`id`、`category`、`title`、`difficulty`（入门 / 进阶 / 挑战）、`durationMin`、`durationMax`、`guide`、`favorite`。

### 录音训练

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/api/sessions/start` | 开始。正文 `{ "topicId": 1 }` |
| `GET` | `/api/sessions/active?topicId=` | 该题未完成的训练。没有时返回 204 |
| `POST` | `/api/sessions/{id}/pause` | 暂停。正文 `{ "durationSeconds": 30 }`，0–1800 |
| `POST` | `/api/sessions/{id}/resume` | 继续 |
| `POST` | `/api/sessions/{id}/audio` | 保存草稿。`multipart/form-data`：`durationSeconds`、`audio`，可选 `prefix` |
| `GET` | `/api/sessions/{id}/audio` | 读取草稿或已保存录音，响应体是音频字节 |
| `POST` | `/api/sessions/{id}/finish` | 结束并评分。`multipart/form-data`，`durationSeconds`、`audio`、`prefix` 都可省略（省略时使用已保存的草稿） |
| `POST` | `/api/sessions/{id}/retry` | 分析失败后重试 |
| `DELETE` | `/api/sessions/{id}` | 放弃这次未完成的训练 |

进行中的状态：

```json
{
  "sessionId": 1,
  "topicId": 1,
  "status": "recording",
  "durationSeconds": 30,
  "hasAudio": true,
  "failReason": null
}
```

结束和重试成功时返回 `{ "reportId": 1, "sessionId": 1 }`。

### 报告与历史

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/reports/{id}` | 报告详情 |
| `GET` | `/api/reports/{id}/audio` | 该次录音。种子数据里的旧记录没有音频 |
| `GET` | `/api/history` | 累计统计、记录列表、当前是否保留历史 |
| `DELETE` | `/api/history` | 永久清空当前用户的训练记录 |

报告主要字段：`id`、`topicId`、`title`、`date`、`shortDate`、`duration`、`score`、`speechRate`、`pauseCount`、`fluency`、`label`、`strengths`、`improvements`、`suggestions`、`transcript`、`hasAudio`、`contentScore`、`contentSummary`。

历史里每条记录含 `reportId`（未完成时为空）、`sessionId`、`status`、`score`、`failReason`。统计字段为 `count`、`durationSeconds`、`durationText`、`average`。

### 个人中心

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/profile` | 资料、累计统计、本周进度 |
| `PUT` | `/api/profile` | 修改昵称、学校、专业、简介 |
| `POST` | `/api/profile/avatar` | 上传头像。表单字段 `avatar`，JPG、PNG 或 WebP，512KB 以内 |
| `GET` | `/api/profile/avatar` | 读取当前用户头像 |
| `PUT` | `/api/profile/goal` | 修改每周目标。正文 `{ "targetCount": 3 }`，范围 1–7 |
| `GET` | `/api/profile/settings` | 通知和隐私设置 |
| `PUT` | `/api/profile/settings` | 保存设置，正文与读取结果同结构 |
| `GET` | `/api/profile/summary` | 打开匿名统计后的人数和本周次数，不含昵称 |
| `GET` | `/api/feedback` | 自己提交的反馈及处理状态 |
| `POST` | `/api/feedback` | 提交反馈 |

资料修改：

```json
{ "nickname": "游客", "school": "明德大学", "major": "软件工程", "bio": "正在准备秋招面试。" }
```

设置字段：`reminderOn`、`reportNoticeOn`、`goalNoticeOn`、`reminderTime`（如 `20:00`）、`keepHistory`、`anonymousStats`。

反馈正文：`category` 为「功能建议」「问题反馈」或「其他」，`content` 最长 500 字。

### 管理接口

以下路径都要求管理员令牌。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/admin/overview` | `userCount`、`topicCount`、`sessionCount`、`pendingFeedbackCount` |
| `GET` | `/api/admin/users?keyword=` | 学员列表，可按关键字筛选 |
| `PUT` | `/api/admin/users/{id}` | 修改 `nickname`、`school`、`major`、`bio`、`role` |
| `DELETE` | `/api/admin/users/{id}` | 删除用户及其关联数据 |
| `GET` | `/api/admin/categories` | 分类列表 |
| `POST` | `/api/admin/categories` | 新建分类。正文 `{ "name": "面试表达" }` |
| `GET` | `/api/admin/topics` | 题目列表 |
| `GET` | `/api/admin/topics/{id}` | 题目详情，含要点 |
| `POST` | `/api/admin/topics` | 新建题目 |
| `PUT` | `/api/admin/topics/{id}` | 更新题目 |
| `DELETE` | `/api/admin/topics/{id}` | 删除题目 |
| `GET` | `/api/admin/sessions?status=` | 训练列表。`status` 可省略 |
| `GET` | `/api/admin/sessions/{id}` | 训练详情，含分数和识别文本 |
| `GET` | `/api/admin/sessions/{id}/audio` | 该次录音 |
| `DELETE` | `/api/admin/sessions/{id}` | 删除这条训练 |
| `GET` | `/api/admin/feedbacks?status=` | 反馈列表。`status` 为「待处理」或「已回复」时可筛选 |
| `PUT` | `/api/admin/feedbacks/{id}` | 回复。正文 `{ "status": "已回复", "reply": "..." }` |

题目写入字段：`categoryId`、`title`、`difficulty`、`durationMin`、`durationMax`、`guide`、`recommended`、`sortOrder`、`checkpoints`。每个要点是 `{ "label": "身份", "keywords": "我是,我叫,名叫" }`。

## 环境要求

| 软件 | 版本 | 用途 |
| --- | --- | --- |
| JDK | 21 | 后端 |
| Maven | 3.9+ 即可 | 后端依赖和启动 |
| MySQL | 8.0+ | 库名 `voxinsight`，本机 `127.0.0.1:3306` |
| Node.js | `^22.18.0` 或 `>=24.12.0` | 三个前端。移动端 `package.json` 写了这个范围 |
| ffmpeg | 能在命令行直接运行 `ffmpeg` | 结束训练时转码。不在 PATH 里时，录音可以保存，分析会失败 |
| Vosk 中文小模型 | `vosk-model-small-cn-0.22` | 解压到 `backend/models/vosk-model-small-cn-0.22` |

浏览器需要允许麦克风。请用 `http://127.0.0.1` 打开页面；录音接口依赖浏览器的 `getUserMedia`。

百炼密钥可选。留空时内容分使用关键词对照，表达分不受影响。

## 项目启动步骤及说明

按这个顺序启动。后端必须先于前端，数据库必须先于后端。

### 1. 导入数据库

在 MySQL 中执行仓库根目录的 `voxinsight.sql`。脚本会删除并重建整个 `voxinsight` 库，已有数据会被清空。

导入成功后应能看到测试用户和管理员，以及若干道题和训练记录。

### 2. 配置后端

在 `backend` 目录把 `.env.example` 复制为 `.env`，填写数据库账号。后端从 `backend` 目录启动时会读这个文件。

```properties
DB_USERNAME=root
DB_PASSWORD=你的密码

# 留空则内容分使用关键词对照
DASHSCOPE_API_KEY=
DASHSCOPE_MODEL=qwen-plus
```

不要把 `.env` 提交到仓库。

### 3. 放入语音模型

将 Vosk 模型 `vosk-model-small-cn-0.22` 解压到：

```text
backend/models/vosk-model-small-cn-0.22
```

该目录下应能直接看到模型文件，而不是再套一层同名文件夹。`application.yml` 里的 `app.vosk-model` 指向这个相对路径。

确认本机可以执行 `ffmpeg -version`。

### 4. 启动后端

```powershell
cd backend
mvn spring-boot:run
```

看到服务监听 `8080` 后，用浏览器打开 http://127.0.0.1:8080/api/health 。数据库连不上时，这里不会返回 `database: ok`，页面上的请求也会失败。

单元测试：

```powershell
cd backend
mvn test
```

测试覆盖评分公式、内容要点、训练状态流转、停顿检测、演示密码散列和音频拼接，不连接 MySQL。

### 5. 启动前端

三个端可以按需要单独启动。每个目录第一次都要先安装依赖。

移动端原型：

```powershell
cd frontend
npm install
npm run dev
```

打开 http://127.0.0.1:5173 ，用 `youke` / `youke123` 登录。

用户学员端：

```powershell
cd web
npm install
npm run dev
```

打开 http://127.0.0.1:5174 ，用 `youke` / `youke123` 登录。

管理后台：

```powershell
cd admin
npm install
npm run dev
```

打开 http://127.0.0.1:5175 ，用 `youke` / `youke123` 登录。

Vite 把浏览器里的 `/api` 转到 `http://127.0.0.1:8080`，前端代码不写死后端地址。生产构建分别在对应目录执行 `npm run build`，静态文件在各自的 `dist/`。

### 6. 全流程演示路径

1. 用游客登录移动端或桌面端，首页能看到推荐题。种子记录不在本周时，进度分子为 0，分母是每周目标。
2. 进入题库，收藏一道题，再到个人中心确认收藏还在。刷新后仍然在。
3. 打开「请用 1 分钟做一个自我介绍。」，允许麦克风，录一段话并结束训练。
4. 报告页应显示「语音识别结果」、识别文本、语速、停顿、流畅度和内容分，并能回放刚才的录音。
5. 打开历史，最新一条就是这次练习。
6. 用 `youke` / `youke123` 登录后台，能看到这次新训练；在反馈页可以回复用户从个人中心提交的意见。

修改每周目标后，首页进度的分母会跟着变。把个人中心的「保留训练历史」关掉，首页和历史不再列出记录，再打开就会恢复。
