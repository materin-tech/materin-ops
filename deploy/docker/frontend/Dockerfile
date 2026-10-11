# 前端镜像：容器内构建 vben monorepo → nginx 托管静态资源
# 构建上下文为仓库根目录（compose 中 context: ..）

FROM docker.m.daocloud.io/library/node:22-alpine AS builder
# 国内网络：corepack 与 pnpm 均走 npmmirror
ENV COREPACK_NPM_REGISTRY=https://registry.npmmirror.com
RUN corepack enable && corepack prepare pnpm@11.8.0 --activate
# vben 的 prepare 脚本(lefthook install)要求 git 仓库，容器内补一个
RUN apk add --no-cache git
RUN npm config set registry https://registry.npmmirror.com
WORKDIR /app

# 先装依赖再拷源码，利用层缓存
COPY frontend/package.json frontend/pnpm-lock.yaml frontend/pnpm-workspace.yaml ./
COPY frontend/internal ./internal
COPY frontend/packages ./packages
COPY frontend/apps ./apps
COPY frontend/scripts ./scripts
COPY frontend/docs ./docs
COPY frontend/playground ./playground

RUN git init -q .
RUN pnpm install --frozen-lockfile

RUN pnpm --filter @vben/web-antd build

FROM docker.m.daocloud.io/library/nginx:stable-alpine
COPY deploy/docker/frontend/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /app/apps/web-antd/dist/ /usr/share/nginx/html/
EXPOSE 80
