# Use Amazon Corretto as the base image for the build stage
FROM amazoncorretto:17 AS build

# Install Maven 3.9.6 (Maven provided by the base image is too old for modern Spring Boot plugins)
RUN yum update -y && \
    yum install -y wget tar && \
    wget https://archive.apache.org/dist/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.tar.gz -O /tmp/maven.tar.gz && \
    tar -xzf /tmp/maven.tar.gz -C /opt && \
    ln -s /opt/apache-maven-3.9.6/bin/mvn /usr/bin/mvn && \
    rm -f /tmp/maven.tar.gz

ENV MAVEN_HOME=/opt/apache-maven-3.9.6
ENV PATH=$MAVEN_HOME/bin:$PATH

# Set the working directory
WORKDIR /app

# Copy project files
COPY pom.xml ./
COPY src ./src

# Build the application (batch mode)
RUN mvn -B package -DskipTests

# Use a lightweight image for the runtime
FROM amazoncorretto:17 AS runtime
WORKDIR /app

# Copy the built jar from the build stage
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
