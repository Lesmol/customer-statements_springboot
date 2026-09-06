FROM amazoncorretto:25-alpine AS build
WORKDIR /app

COPY mvnw pom.xml ./
COPY .mvn .mvn
COPY api-gateway/pom.xml api-gateway/
COPY login-service/pom.xml login-service/
COPY statement-service/pom.xml statement-service/
COPY user-service/pom.xml user-service/
RUN ./mvnw -pl user-service dependency:go-offline -B

COPY user-service/src user-service/src
RUN ./mvnw -pl user-service clean package -DskipTests

FROM amazoncorretto:25-alpine AS runtime
WORKDIR /app

COPY --from=build /app/user-service/target/user-service.jar app.jar

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]
