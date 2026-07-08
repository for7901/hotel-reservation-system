# 使用本地已打包的 jar 运行（适合服务器部署：本地 mvnw package 后上传 target/*.jar）
FROM amazoncorretto:17-alpine
WORKDIR /app
COPY target/hotel-reservation-system-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
