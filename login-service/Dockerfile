FROM amazoncorretto:25-alpine AS build
WORKDIR /app

COPY mvnw pom.xml ./
COPY .mvn .mvn
COPY api-gateway/pom.xml api-gateway/
COPY login-service/pom.xml login-service/
COPY statement-service/pom.xml statement-service/
COPY user-service/pom.xml user-service/
RUN ./mvnw -pl login-service dependency:go-offline -B

COPY login-service/src login-service/src
RUN ./mvnw -pl login-service clean package -DskipTests

FROM amazoncorretto:25-alpine AS runtime
WORKDIR /app

COPY --from=build /app/login-service/target/login-service.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]