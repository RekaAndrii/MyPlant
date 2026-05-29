# Use a Java 17 JDK base image
FROM amazoncorretto:17

# Set the working directory in the container
WORKDIR /app

# Add application JAR file to the container
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar

# Expose the application port
EXPOSE 8080

# Command to run the application
ENTRYPOINT ["java", "-jar", "/app/app.jar"]