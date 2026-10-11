# 后端镜像：多阶段构建（容器内 mvn 打包 → JRE 运行）
# 镜像源走 daocloud（docker hub 直连不稳定）

FROM docker.m.daocloud.io/library/maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app

# Maven 阿里云镜像，加速国内网络下的依赖下载
COPY deploy/docker/backend/maven-settings.xml /root/.m2/settings.xml
# 多模块：先拷全部 pom 让依赖层缓存，再拷各模块源码
COPY backend/pom.xml ./pom.xml
COPY backend/common/pom.xml ./common/pom.xml
COPY backend/system/pom.xml ./system/pom.xml
COPY backend/components/pom.xml ./components/pom.xml
COPY backend/components/ops/pom.xml ./components/ops/pom.xml
COPY backend/components/developer/pom.xml ./components/developer/pom.xml
COPY backend/components/notice/pom.xml ./components/notice/pom.xml
COPY backend/components/resource/pom.xml ./components/resource/pom.xml
COPY backend/components/resource/resource-core/pom.xml ./components/resource/resource-core/pom.xml
COPY backend/components/resource/resource-local/pom.xml ./components/resource/resource-local/pom.xml
COPY backend/components/resource/resource-obs/pom.xml ./components/resource/resource-obs/pom.xml
COPY backend/components/resource/resource-minio/pom.xml ./components/resource/resource-minio/pom.xml
COPY backend/components/ticket/pom.xml ./components/ticket/pom.xml
COPY backend/server/pom.xml ./server/pom.xml
RUN mvn -q -B dependency:go-offline || true

COPY backend/system/src ./system/src
COPY backend/components/ops/src ./components/ops/src
COPY backend/components/developer/src ./components/developer/src
COPY backend/components/notice/src ./components/notice/src
COPY backend/components/resource/resource-core/src ./components/resource/resource-core/src
COPY backend/components/resource/resource-local/src ./components/resource/resource-local/src
COPY backend/components/resource/resource-obs/src ./components/resource/resource-obs/src
COPY backend/components/resource/resource-minio/src ./components/resource/resource-minio/src
COPY backend/components/ticket/src ./components/ticket/src
COPY backend/common/src ./common/src
COPY backend/server/src ./server/src
RUN mvn -q -B package -DskipTests

FROM docker.m.daocloud.io/library/eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/server/target/*.jar app.jar
ENV TZ=Asia/Shanghai
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
