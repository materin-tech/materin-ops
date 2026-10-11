# deploy — 部署模块

## 文件说明

```
deploy/
├── docker-compose.yml              # 全栈：MySQL + Redis + 后端 + 前端(nginx)
├── docker-compose.middleware.yml   # 本地开发：仅中间件（MySQL + Redis），前后端跑在本机
├── .env.example                    # 环境变量模板，复制为 .env 使用
└── docker/
    ├── backend/Dockerfile          # 后端多阶段构建：容器内 mvn 打包 → JRE 运行
    │   └── maven-settings.xml      # 容器内 Maven 阿里云镜像
    └── frontend/Dockerfile         # 前端多阶段构建：容器内 pnpm build → nginx 托管
        └── nginx.conf              # SPA + /api 反代到 backend:8080
```

## 快速开始

### 全栈部署

```bash
cd deploy
cp .env.example .env   # 按需改端口/密码
docker compose up -d --build

# 验证
docker compose ps
curl -s http://localhost:8080/auth/login -X POST \
  -H "Content-Type: application/json" -d '{"username":"admin","password":"123456"}'
```

### 本地开发（仅中间件）

```bash
cd deploy
docker compose -f docker-compose.middleware.yml up -d
# 后端本机：cd backend && mvn spring-boot:run
# 前端本机：cd frontend && pnpm dev
```

## 关键约定

- **DDL 只由 Flyway 执行**：compose 只创建空库（`MYSQL_DATABASE=materin_ops`），
  表结构和种子数据由后端启动时的 Flyway 迁移落库，deploy 模块不放任何 DDL。
- **镜像源**：基础镜像统一走 `docker.m.daocloud.io`（docker hub 直连不稳定）。
- **Redis**：Sa-Token 会话持久化到 Redis（`sa-token-redis-jackson`），容器内通过
  `SPRING_DATA_REDIS_HOST/PORT` 环境变量注入连接配置。
- **数据库连接**：容器内通过 `SPRING_DATASOURCE_URL/USERNAME/PASSWORD` 环境变量注入，
  覆盖 application.yml（Spring 环境变量优先级高于 yml），无本地 profile 文件。
- **前端 API 地址**：生产构建固定 `/api`，由 nginx 反代到 `backend:8080`（转发时去掉 `/api` 前缀）。
- **安全端口**：MySQL 端口映射仅用于本地调试，生产部署删除 `mysql.ports` 映射即可。
