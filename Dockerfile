# 单阶段构建：使用最通用的 Maven+JDK17 镜像（所有云平台均支持）
FROM maven:3.8-openjdk-17
WORKDIR /app

# 1. 复制 pom.xml 并下载依赖（缓存依赖，后续构建更快）
COPY pom.xml .
# 阿里云 Maven 镜像加速（解决 Render 下载依赖超时问题）
RUN sed -i 's|http://central|https://mirrors.aliyun.com/maven/maven2|g' /usr/share/maven/conf/settings.xml
RUN mvn dependency:go-offline

# 2. 复制项目源码
COPY src ./src

# 3. 打包 Spring Boot 应用（跳过测试，加速构建）
RUN mvn clean package -DskipTests

# 4. 启动应用（监听 Render 分配的 PORT 环境变量）
ENTRYPOINT ["java", "-jar", "target/demotwo-0.0.1-SNAPSHOT.jar"]