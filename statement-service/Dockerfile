FROM amazoncorretto:25-alpine AS build
WORKDIR /app

COPY mvnw pom.xml ./
COPY .mvn .mvn
COPY api-gateway/pom.xml api-gateway/
COPY login-service/pom.xml login-service/
COPY statement-service/pom.xml statement-service/
COPY user-service/pom.xml user-service/
RUN ./mvnw -pl statement-service dependency:go-offline -B

COPY statement-service/src statement-service/src
RUN ./mvnw -pl statement-service clean package -DskipTests

FROM amazoncorretto:25-alpine AS runtime
WORKDIR /app

COPY --from=build /app/statement-service/target/statement-service.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]