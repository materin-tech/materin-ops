# H5 镜像：托管本地构建产物（vite build 在宿主执行，规避容器内 npm 网络抖动）
# 构建步骤：cd h5 && npm install && npm run build
FROM docker.m.daocloud.io/library/nginx:stable-alpine
COPY deploy/docker/h5/nginx.conf /etc/nginx/conf.d/default.conf
COPY h5/dist/ /usr/share/nginx/html/
EXPOSE 80
