# 第一阶段：构建 Spring Boot 应用（用稳定的 Maven 镜像，避免标签找不到）
FROM maven:3.9.6-openjdk-17 AS build-stage
WORKDIR /app
COPY pom.xml .
# 阿里云 Maven 镜像加速（确保依赖下载不超时）
RUN sed -i 's/central/mirrors.aliyun.com\/maven\/maven2/' /usr/share/maven/conf/settings.xml
# 缓存 Maven 依赖
RUN mvn dependency:go-offline
COPY src ./src
# 打包 JAR（跳过测试，加速构建）
RUN mvn clean package -DskipTests

# 第二阶段：运行 JAR（用 AdoptOpenJDK 镜像，Render 100% 支持）
FROM adoptopenjdk:17-jre-slim
WORKDIR /app
# 从构建阶段复制 JAR 包（你的 JAR 文件名正确，无需修改）
COPY --from=build-stage /app/target/demotwo-0.0.1-SNAPSHOT.jar app.jar
# 启动应用（确保监听环境变量 PORT）
ENTRYPOINT ["java", "-jar", "app.jar"]