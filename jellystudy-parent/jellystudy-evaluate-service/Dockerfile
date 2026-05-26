# 已迁移至 docker/Dockerfile.jvm-service（build context = 项目根目录）
# 请使用: docker compose up -d --build
# 本文件保留仅为向后兼容说明，不再直接使用。
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/jellystudy-evaluate-service-1.0.0-SNAPSHOT.jar app.jar
ENV SERVER_PORT=8083
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
