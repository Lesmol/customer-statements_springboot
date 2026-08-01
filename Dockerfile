FROM amazoncorretto:25-alpine AS build
WORKDIR app/

COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN ./mvnw dependency:go-offline

COPY src ./src
RUN ./mvnw clean package -o -DskipTests

FROM amazoncorretto:25-alpine AS runtime
WORKDIR app/

COPY --from=build app/target/customer-statements.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]