# 第一阶段：构建 Spring Boot 应用
FROM maven:3.8.8-openjdk-17 AS builder
WORKDIR /app
COPY pom.xml .
# 缓存 Maven 依赖（避免每次构建都下载）
RUN mvn dependency:go-offline
COPY src ./src
# 打包为可执行 JAR（排除测试）
RUN mvn clean package -DskipTests

# 第二阶段：运行 JAR（使用轻量 Java 镜像）
FROM openjdk:17-jdk-slim
WORKDIR /app
# 从构建阶段复制 JAR 文件（注意：替换为你的 JAR 文件名，可在 target 目录查看）
COPY --from=builder /app/target/demotwo-0.0.1-SNAPSHOT.jar app.jar
# 暴露端口（Vercel 会忽略，但规范写法）
EXPOSE ${PORT}
# 启动命令（监听环境变量 PORT）
ENTRYPOINT ["java", "-jar", "app.jar"]