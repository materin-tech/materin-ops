# Kubernetes 部署参考

## 0. 镜像准备

Manifests 默认拉取 `ghcr.io/materin-tech/*`，构建并推送（任选 Registry，同步改镜像名）：

```bash
REG=ghcr.io/materin-tech
VER=v1.0.0
docker build -f deploy/docker/backend/Dockerfile -t $REG/materin-ops-backend:$VER .
docker build -f deploy/docker/frontend/Dockerfile -t $REG/materin-ops-web:$VER .
docker build -f h5/Dockerfile -t $REG/materin-ops-h5:$VER .
# PC/H5 镜像内含 dist（宿主先构建）：
(cd frontend/apps/web-antd && pnpm install && pnpm build)
(cd h5 && npm install && npm run build)
docker push $REG/materin-ops-backend:$VER
docker push $REG/materin-ops-web:$VER
docker push $REG/materin-ops-h5:$VER
```

> 注意：PC/H5 应用镜像内含 dist（`docker build` 前先在宿主完成前端构建再构建镜像）；
> backend 镜像为容器内 Maven 构建。web/h5 部署时的 nginx 反代配置由 ConfigMap
> （web-nginx-conf / h5-nginx-conf）覆盖镜像内配置，K8s 下无需 Docker 专用 resolver。

## 1. 一键部署

```bash
kubectl apply -f deploy/kubernetes/   # 按文件名顺序自动生效
kubectl -n materin-ops get pods -w    # 观察 mysql → redis → backend → web/h5
```

## 2. 访问

默认 Ingress 域名示例 `ops.example.com` / `m.ops.example.com`，按实际域名修改 `08-ingress.yaml`
并解析到集群 Ingress 入口。也可临时端口转发：

```bash
kubectl -n materin-ops port-forward svc/web 8081:80   # PC
kubectl -n materin-ops port-forward svc/h5  8082:80   # H5
```

## 3. 结构说明

| 文件 | 内容 |
|---|---|
| 00-namespace | 命名空间 materin-ops |
| 01-configmap / 02-secret | 后端环境配置与敏感项（生产改用 ExternalSecrets/SealedSecrets） |
| 03-mysql | StatefulSet + headless Service + 10Gi PVC，utf8mb4，自动建库 |
| 04-redis | Deployment + 2Gi PVC（AOF） |
| 05-backend | 2 副本 Deployment：initContainers 等待 DB/Redis、startupProbe 兜底 Flyway 冷启动、文件 PVC |
| 06-web / 07-h5 | nginx + ConfigMap 反代配置（K8s DNS resolver 无需 127.0.0.11）、2 副本 |
| 08-ingress | 双域名路由 + 50m 上传限制 |
| 09-hpa | 可选 backend CPU>70% 扩容（2→6） |

## 4. 与 Docker Compose 的差异

| 项 | Compose | K8s |
|---|---|---|
| 后端副本 | 1 | 2 + HPA（无状态，会话在 Redis，可水平扩展） |
| 文件存储 | 本地卷 | PVC（**多副本需 ReadWriteMany**，如 NFS/云盘共享；或切 OBS/MinIO 后端） |
| 反代 DNS | 127.0.0.11 | 集群 DNS（ConfigMap 中已内置） |
| 配置 | .env | ConfigMap + Secret |

> **重要**：backend 多副本时 `resource.storage.type=local` 的文件分散在各 Pod 本地盘，
> 生产建议直接切 `obs` 或 `minio`（配置项见 application.yml 注释），或把 backend-files PVC
> 换成 ReadWriteMany 存储类。

## 5. 升级

```bash
kubectl -n materin-ops set image deploy/backend backend=ghcr.io/materin-tech/materin-ops-backend:v1.1.0
kubectl -n materin-ops rollout status deploy/backend   # Flyway 增量迁移自动执行
```
