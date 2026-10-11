# materin-ops 格物 · 部署指南

> 适用版本：v1.0.0+ ｜ 两种部署方式：**Docker Compose 源码构建**（推荐）与 **Release 产物部署**

## 一、前置条件

| 依赖 | 版本要求 | 说明 |
|---|---|---|
| Docker + Docker Compose | v2+ | 所有服务容器化 |
| Node.js | 22（前端/H5 构建时需要） | 含 pnpm（corepack） |
| Java / Maven | 21 / 3.9（可选） | 仅本地构建 jar 或跑单测时需要 |

## 二、方式 A：Docker Compose 源码构建（推荐）

```bash
git clone https://github.com/materin-tech/materin-ops.git
cd materin-ops

# 1. 准备环境变量
cp deploy/.env.example deploy/.env   # 可按需修改端口/密码

# 2. 构建并启动全部服务（首次构建约 5-10 分钟）
docker compose -f deploy/docker-compose.yml build backend frontend h5
docker compose -f deploy/docker-compose.yml up -d

# 3. 查看启动日志（Flyway V1-V10 自动建表迁移）
docker logs -f materin-ops-backend
```

启动完成后：

| 服务 | 地址 | 说明 |
|---|---|---|
| PC 管理端 | http://localhost | 全功能后台 |
| H5 移动端 | http://localhost:81 | 移动端工作台/工单/产品 |
| API 文档 | http://localhost/doc.html | Knife4j |
| MySQL | localhost:3306 | 库名 materin_ops |
| Redis | localhost:6379 | 会话存储 |

**默认账号：`admin / 123456`（生产环境务必修改）**

## 三、方式 B：Release 产物部署

从 [Releases](https://github.com/materin-tech/materin-ops/releases) 下载产物，自备 MySQL 8 + Redis 7：

### 3.1 后端

```bash
# 需要 Java 21；数据库连接可用环境变量覆盖
export DB_USERNAME=root
export DB_PASSWORD=your-password
java -jar materin-ops-server.jar \
  --spring.datasource.url="jdbc:mysql://localhost:3306/materin_ops?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true"
```
首次启动 Flyway 自动执行 V1-V10 迁移（建表 + 种子数据）。

### 3.2 PC 前端

```bash
unzip materin-ops-web-dist.zip -d /usr/share/nginx/html
```
nginx 关键配置（SPA + API 反代，**rewrite 必须有，resolver 用于容器动态解析**）：

```nginx
location /api/ {
    set $backend_upstream http://backend:8080;
    rewrite ^/api/(.*)$ /$1 break;
    proxy_pass $backend_upstream;
}
location / { try_files $uri $uri/ /index.html; }
```

### 3.3 H5

同 3.2（解压 `materin-ops-h5-dist.zip` 到独立站点，如 81 端口）。

## 四、配置说明（backend，均可用环境变量覆盖）

| 配置 | 环境变量 | 默认 | 说明 |
|---|---|---|---|
| 数据库 | `DB_USERNAME` / `DB_PASSWORD` / `SPRING_DATA_REDIS_HOST` | root / root / localhost | 容器部署由 compose 注入 |
| token 有效期 | `sa-token.timeout` | 30 天 | Sa-Token 会话持久化在 Redis |
| 文件存储 | `resource.storage.type` | local | local / obs / minio；OBS/MinIO 参数见 application.yml 注释 |
| H5 端口 | `H5_PORT` | 81 | compose 映射 |

## 五、验证清单

```bash
# 登录
curl -s -X POST http://localhost/api/auth/login -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"123456"}'
# 工单列表（token 从上一步取）
curl -s http://localhost/api/ticket/tickets -H "Authorization: Bearer <token>"
# 开放 API 文档
curl -s -o /dev/null -w '%{http_code}' http://localhost/doc.html   # 200
```
UI 验证：登录 → 工作台统计有数 → 提交工单 → 列表可见。

## 六、常见问题（实战踩坑）

| 现象 | 原因与解决 |
|---|---|
| 登录报 `Invalid CORS request` | Sa-Token 自带 CORS 过滤器校验 Origin；新端点接入时在 `CorsConfig` 禁用之（本仓库已处理） |
| API 全部 502 | nginx 反代用了变量 `proxy_pass` 但缺 `resolver 127.0.0.11`；或后端容器未启动 |
| 登录 401（路径正确） | 反代未去掉 `/api` 前缀，补 `rewrite ^/api/(.*)$ /$1 break;` |
| Flyway 校验失败 | 迁移文件（V1-V10）字节级不能改；新环境用 `spring.flyway.clean-disabled=false` + 空库重跑 |
| 压测时登录偶发 500 | 并发连接池竞争，调大 HikariCP `maximum-pool-size` |
| knife4j 文档 500 | springdoc 需 ≥2.8（knife4j 4.5.0 传递的 2.2 与 Boot 3.5 不兼容，仓库已强制 2.8.9） |
| 图片/附件上传 404 | 资源存储默认 local，路径 `./data/files`，容器部署请挂卷持久化 |

## 七、升级

1. 备份 MySQL（`materin_ops` 库）与资源目录（`./data/files`）
2. 拉取新代码 / 新 Release 产物
3. 重建镜像并 `up -d`——Flyway 增量迁移自动执行
