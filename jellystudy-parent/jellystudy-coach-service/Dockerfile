# 已迁移至 docker/Dockerfile.jvm-service（build context = 项目根目录）
# 请使用: docker compose up -d --build
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/jellystudy-coach-service-1.0.0-SNAPSHOT.jar app.jar
ENV SERVER_PORT=8084
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
