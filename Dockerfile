# Build stage: use official Maven (3.9.6) with Amazon Corretto 17
FROM maven:3.9.6-amazoncorretto-17 AS build

WORKDIR /app
COPY pom.xml ./
COPY src ./src
COPY src/main/resources/application.properties ./src/main/resources/application.properties

RUN mvn -B package -DskipTests

# Runtime stage: use Amazon Corretto 17 (JRE only, smaller image)
FROM amazoncorretto:17 AS runtime
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
