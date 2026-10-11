# materin-ops-backend

线上工单反馈与运维系统 — 后端（Maven 多模块）。

## 技术栈

- Java 21 / Maven 多模块
- Spring Boot 3.5.3 (Web / Validation)
- MyBatis-Flex 1.10.8（ORM，逻辑删除：`deleted` 字段）
- MySQL 8（`mysql-connector-j`）
- Flyway（数据库版本管理，`db/migration`）
- Sa-Token 1.44.0（登录态 + 注解鉴权，会话持久化到 Redis）
- Knife4j 4.5.0（OpenAPI 文档 /doc.html，接口清单同步数据源）
- Lombok

## 目录结构

```
backend/
├── pom.xml                # 父 pom（packaging=pom，三方依赖统一继承）
├── system/                # materin-ops-system：common + RBAC（module/system）
├── components/            # 业务组件聚合（packaging=pom）
│   ├── ops/               # materin-ops-components-ops：需求管理（ITR 工单）域
│   └── developer/         # materin-ops-components-developer：开发者中心 / 开放 API
└── server/                # materin-ops-server：启动类、config、Flyway V1-V4
```

包名统一前缀 `com.materin.tech.ops`。

## 本地启动

1. 建库：

   ```sql
   create database materin_ops default charset utf8mb4;
   ```

2. 按需修改 `src/main/resources/application.yml` 中的数据库连接（默认 root/root）。

3. 启动（Flyway 自动建表并写入种子数据）：

   ```bash
   mvn spring-boot:run
   ```

4. 默认账号：`admin / 123456`（首次启动由 DataInitializer 自动写入 BCrypt 密码）。

## 接口约定

- 所有响应统一包装为 `{ code, message, data }`，`code = 0` 表示成功。
- 登录后请求头携带 token：`Authorization: Bearer <accessToken>`（与前端 vben 对齐）。
- 第三方 APP 双通道调用：请求头 `X-App-Key / X-Timestamp / X-Nonce / X-Signature`（HMAC-SHA256，与登录体系同路径并行，详见 components/developer）。
- knife4j 文档：`/doc.html`（接口清单经 `/v3/api-docs` 同步进「开发者中心-接口管理」）。
- 主要接口：
  - `POST /auth/login` 登录（匿名）
  - `POST /auth/logout` 登出
  - `GET /user/info` 当前用户信息（vben5 约定）
  - `GET /auth/routes` 当前用户动态路由
  - `/system/users`、`/system/roles`、`/system/menus` RBAC 管理
