# 文物修复展借与保险核验平台

## 原始需求

> 请实现文物修复展借与保险核验平台，Vue3 页面给博物馆藏品部、修复师、外借展馆、保险公估、安防负责人和运输监管人员共同使用，Spring Boot 服务端维护藏品编号、年代材质、病害图谱、修复方案、展借申请、环境要求、保险估值、包装标准、运输封签和归还验收。藏品部登记器物来源、历史修复、禁止展出条件和借展限制；修复室记录裂隙、剥落、虫蛀、颜料粉化、清洗试验和加固材料；外借展馆提交展厅温湿度、照度、震动、安保路线和布展时间；保险公估核对估值、免赔条款和运输责任；运输监管确认箱体、封签、押运员和到馆交接。平台要把修复前评估、修复实施、借展审批、保险生效、包装出库、展厅布置、巡展监测和归还复核连成一条证据链，让每次状态变化都能说明由谁确认、依据哪张照片、影响哪项责任。

## 项目简介

文物修复展借与保险核验平台是面向博物馆多角色协作的全流程管理系统，覆盖从藏品登记、病害诊断、修复实施、借展审批、保险核验、包装出库、运输监管、展厅布置、巡展监测到归还验收的完整证据链。

### 六大角色

| 角色 | 代码 | 职责 |
|------|------|------|
| 藏品部 | COLLECTION | 登记器物来源、历史修复、禁止展出条件和借展限制；审批借展申请 |
| 修复师 | RESTORER | 记录病害图谱（裂隙、剥落、虫蛀、颜料粉化等）、制定修复方案、执行修复步骤 |
| 外借展馆 | BORROWER | 提交借展申请、展厅环境要求（温湿度、照度、震动、安保路线） |
| 保险公估 | INSURER | 核对估值、免赔条款、运输责任，确认保险生效 |
| 安防负责人 | SECURITY | 确认包装标准、封签、展厅布置、巡展环境监测 |
| 运输监管 | TRANSPORT | 确认箱体、押运员、运输途中环境监控、到馆交接 |

### 证据链流程

修复前评估 → 修复实施 → 借展审批 → 保险生效 → 包装出库 → 展厅布置 → 巡展监测 → 归还复核

## 技术栈

- **前端**：Vue 3 + TypeScript + Vite + Tailwind CSS + Vue Router + Lucide Icons
- **后端**：Spring Boot + JPA + MySQL
- **部署**：Docker + Docker Compose + Nginx

## 启动方式

### Docker 一键启动（推荐）

#### 前置要求

- Docker 20.10+
- Docker Compose V2

#### 启动步骤

```bash
docker compose up --build
```

后台运行：

```bash
docker compose up --build -d
```

停止服务：

```bash
docker compose down
```

访问地址：http://localhost:3000

### 手动启动

#### 前置要求

- Node.js 18+
- Java 17+
- MySQL 8.0+

#### 1. 初始化数据库

```sql
CREATE DATABASE relic_platform CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

然后执行 `backend/target/classes/schema.sql` 和 `backend/target/classes/data.sql` 初始化表结构和演示数据。

#### 2. 启动后端

```bash
cd backend
java -jar target/relic-platform-1.0.0.jar
```

后端地址：http://localhost:8080

#### 3. 安装前端依赖

```bash
npm install
```

#### 4. 启动前端开发服务器

```bash
npm run dev
```

访问地址：http://localhost:5173

### 演示账号

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 藏品部 | zhangwei | 123456 |
| 修复师 | liuna | 123456 |
| 外借展馆 | wangqiang | 123456 |
| 保险公估 | zhaoli | 123456 |
| 安防负责人 | chenming | 123456 |
| 运输监管 | sunjie | 123456 |

## 目录结构

```
├── src/                          # 前端源码
│   ├── api/                      # API 服务层
│   ├── components/               # 共享组件
│   ├── composables/              # 组合式函数
│   ├── lib/                      # 工具库
│   ├── pages/                    # 页面组件
│   │   ├── LoginPage.vue
│   │   ├── DashboardPage.vue
│   │   ├── ArtifactListPage.vue
│   │   ├── ArtifactDetailPage.vue
│   │   ├── DiseaseMapPage.vue      # 病害图谱（含高清图标注）
│   │   ├── DiseaseReviewPage.vue   # 病害复核（藏品部）
│   │   ├── EnvironmentPreCheckPage.vue  # 展厅环境预审
│   │   ├── RestorationPlanPage.vue
│   │   ├── RestorationDetailPage.vue
│   │   ├── LoanApplyPage.vue
│   │   ├── LoanApprovalPage.vue  # 借展审批（含风险展示）
│   │   ├── InsuranceVerifyPage.vue
│   │   ├── PackagingPage.vue
│   │   ├── TransportMonitorPage.vue
│   │   ├── ExhibitionSetupPage.vue
│   │   ├── ExhibitionMonitorPage.vue
│   │   ├── ReturnAcceptancePage.vue
│   │   └── EvidenceChainPage.vue
│   ├── router/                   # 路由配置
│   ├── stores/                   # 状态管理
│   ├── App.vue
│   ├── main.ts
│   └── style.css
├── backend/                      # 后端
│   ├── src/
│   │   └── main/
│   │       ├── java/com/relic/platform/
│   │       │   ├── controller/   # 控制器层（10个）
│   │       │   ├── service/      # 服务层（10个）
│   │       │   ├── repository/   # 数据访问层（16个）
│   │       │   ├── entity/       # 实体类（16个）
│   │       │   ├── dto/          # 数据传输对象
│   │       │   ├── config/       # 配置类
│   │       │   └── RelicPlatformApplication.java
│   │       └── resources/
│   │           ├── application.yml
│   │           ├── schema.sql     # 数据库建表脚本
│   │           └── data.sql       # 初始化演示数据
│   ├── target/                   # 编译产物
│   │   └── relic-platform-1.0.0.jar
│   ├── pom.xml
│   ├── Dockerfile
│   └── .dockerignore
├── docker-compose.yml            # Docker 编排
├── Dockerfile                    # 前端 Docker 构建配置
├── nginx.conf                    # Nginx 配置
└── .dockerignore
```
