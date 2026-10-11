# Materin Ops — 线上工单反馈与运维系统

线上工单反馈和运维系统。前后端分离：前端基于 [Vue Vben Admin 5.x](https://doc.vben.pro/)，后端为 Spring Boot 3 多模块服务：system 基座 + components 业务组件（ops 需求管理 / developer 开发者中心），内置完整 RBAC 与开放 API 双通道鉴权。

## 技术栈

| 层 | 技术 | 版本 |
|---|---|---|
| 前端 | Vue Vben Admin（Vue 3.5 + Vite + TS，pnpm monorepo） | 5.7.0 |
| 后端 | Spring Boot 多模块（system / components.ops / components.developer / server） | 3.5.3 |
| 文档 | Knife4j（OpenAPI，/doc.html） | 4.5.0 |
| ORM | MyBatis-Flex | 1.10.8 |
| 认证 | Sa-Token（会话持久化到 Redis） | 1.44.0 |
| 缓存/会话 | Redis | 7 |
| 数据库 | MySQL 8（DDL 由 **Flyway** 管控） | — |
| JDK | Java | 21 |

## 目录结构

```
materin-ops/
├── backend/                     # Spring Boot 单体服务
│   └── src/main/resources/
│       └── db/migration/        # Flyway 迁移脚本（DDL 唯一来源，禁止手工改库）
│           ├── V1__init_rbac.sql        # RBAC 五张表
│           └── V2__seed_rbac_data.sql   # 角色/菜单/管理员种子数据
├── frontend/                    # Vue Vben Admin 5（monorepo，业务代码在 apps/web-antd）
├── deploy/                      # Docker Compose 部署（全栈 / 纯中间件两套编排）
│   └── ...                     # 详见 deploy/README.md
└── README.md
```

## 后端约定

- **统一响应**：所有接口返回 `{ code, message, data }`，`code = 0` 成功，`401` 未登录、`403` 无权限。
- **RBAC 模型**：`sys_user` / `sys_role` / `sys_menu` + 两张绑定表；菜单 `type`：`D` 目录 / `M` 菜单 / `B` 按钮，按钮权限用 `@SaCheckPermission` 注解校验。
- **默认账号**：`admin / 123456`（super_admin，权限标记 `*:*:*`）。
- **数据库变更**：只允许新增 Flyway 迁移脚本（`V<N>__desc.sql`），已合入的脚本不可修改。

## 本地启动

### 后端（需先建库）

```sql
CREATE DATABASE materin_ops DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
```

```bash
cd backend
# 按需修改 src/main/resources/application.yml 中的数据库账号密码
mvn spring-boot:run    # 启动后 Flyway 自动执行迁移建表
```

服务端口 `8080`。

### 前端

```bash
cd frontend
pnpm install
pnpm dev
```

开发环境通过 Vite 代理将 `/api` 转发到 `http://localhost:8080`（`apps/web-antd/vite.config.ts` 中 `proxy` 的 `target`，`/api` 前缀会被 rewrite 掉，后端路径无需前缀）。

## 路线图

- [x] 基础框架：后端骨架 + RBAC 表 + 认证/用户/角色/菜单 API
- [x] deploy 模块：Docker Compose 一键部署前后端与中间件
- [ ] 前端登录对接（替换 vben mock 接口为后端真实接口）
- [ ] 工单域：工单类型、工单提交、流转（受理/处理/关闭）、评论与附件
- [ ] 通知与统计看板

## 应用矩阵

| 端 | 地址 | 技术栈 | 说明 |
|---|---|---|---|
| PC 管理端 | http://localhost | Vue Vben Admin 5 + Antd | 全功能（含系统管理/开发者中心） |
| H5 移动端 | http://localhost:81 | Vue3 + Vant 4 | 工作台 / 产品 / 工单（无系统管理） |
| API 文档 | http://localhost/doc.html | Knife4j | 开放 API 对接依据 |

## 快速开始

```bash
# 前置：Docker + Node 22 + Java 21 + Maven
cp deploy/.env.example deploy/.env

# 后端（容器内构建）
docker compose -f deploy/docker-compose.yml build backend frontend h5
docker compose -f deploy/docker-compose.yml up -d

# 默认账号：admin / 123456（生产务必修改）

## 前端开发模式
cd frontend && pnpm install && pnpm dev
cd h5 && npm install && npm run dev
```

## 测试体系

| 层 | 工具 | 位置 |
|---|---|---|
| 产品功能用例 | 66 条矩阵 | docs/materin-ops-测试用例.xlsx（生成器 docs/generators/） |
| 后端单测 | JUnit5 + Mockito | backend/*/src/test，`mvn test` |
| E2E | Playwright | e2e/，`npx playwright test` |
| API 自动化/压测 | JMeter（Docker） | jmeter/，`bash jmeter/smoke.sh` 冒烟 12 项 |
| APP 鉴权专项 | python3 | docs/generators/verify_app_auth.py（16 项） |

详见 docs/test-plan.md 与 docs/product-spec.md。
