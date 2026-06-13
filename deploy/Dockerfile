FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY zest-sso-server/target/zest-sso-server-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 9000
ENTRYPOINT ["java", "-jar", "app.jar"]
