FROM amazoncorretto:25-alpine AS build
WORKDIR /app

COPY mvnw pom.xml ./
COPY .mvn .mvn
COPY api-gateway/pom.xml api-gateway/
COPY login-service/pom.xml login-service/
COPY statement-service/pom.xml statement-service/
COPY user-service/pom.xml user-service/
RUN ./mvnw -pl api-gateway dependency:go-offline -B

COPY api-gateway/src api-gateway/src
RUN ./mvnw -pl api-gateway clean package -DskipTests

FROM amazoncorretto:25-alpine AS runtime
WORKDIR /app

COPY --from=build /app/api-gateway/target/api-gateway.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]