# Use Amazon Corretto as the base image for the build stage
FROM amazoncorretto:17 AS build

# Install Maven
RUN yum update -y && yum install -y maven

# Set the working directory
WORKDIR /app

# Copy project files
COPY pom.xml ./
COPY src ./src

# Build the application
RUN mvn package -DskipTests

# Use a lightweight image for the runtime
FROM amazoncorretto:17 AS runtime
WORKDIR /app

# Copy the built jar from the build stage
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
