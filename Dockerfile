# 第一阶段：构建 Spring Boot 应用
FROM maven:3.8.8-openjdk-17 AS builder
WORKDIR /app
COPY pom.xml .
# 用阿里云 Maven 镜像加速依赖下载
RUN sed -i 's/central/mirrors.aliyun.com\/maven\/maven2/' /usr/share/maven/conf/settings.xml
# 缓存 Maven 依赖
RUN mvn dependency:go-offline
COPY src ./src
# 打包 JAR（跳过测试）
RUN mvn clean package -DskipTests

# 第二阶段：运行 JAR
FROM openjdk:17-jdk-slim
WORKDIR /app
# 复制具体的 JAR 包（替换为你的 JAR 文件名）
COPY --from=builder /app/target/demotwo-0.0.1-SNAPSHOT.jar app.jar
# 启动应用
ENTRYPOINT ["java", "-jar", "app.jar"]