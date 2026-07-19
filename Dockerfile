# ===== Stage 1: Build =====
# Maven 3.9 满足 pom 中插件的版本下限（本机 3.6.1 的 pin 仅为兼容下限）
FROM maven:3.9-eclipse-temurin-17-focal AS build
WORKDIR /build

# 先只拷贝 pom 预下载依赖，利用 Docker layer 缓存加速后续增量构建
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# 再拷贝源码构建 fat jar；跳过测试（不触发 verify 阶段的 jacoco 门禁）
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ===== Stage 2: Run =====
# 运行阶段只需 JRE，镜像体积远小于构建镜像
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

COPY --from=build /build/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
