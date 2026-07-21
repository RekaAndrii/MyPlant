# AGENTS instructions for MyPlant

## Project overview
- This repository is a Spring Boot 1.5.2 application for a small plant-tracking web app.
- The main entry point is [src/main/java/com/my/plant/configs/MyPlantApplication.java](src/main/java/com/my/plant/configs/MyPlantApplication.java).
- Server-rendered views use Thymeleaf under [src/main/resources/templates](src/main/resources/templates), with static assets in [src/main/resources/static](src/main/resources/static).
- Persistence uses Spring Data MongoDB and MongoTemplate; the main data-access implementation is in [src/main/java/com/my/plant/service/impl/BlockServiceImpl.java](src/main/java/com/my/plant/service/impl/BlockServiceImpl.java).

## Code organization
- Keep the existing package structure:
  - Controllers: [src/main/java/com/my/plant/controller](src/main/java/com/my/plant/controller)
  - Service interfaces: [src/main/java/com/my/plant/service](src/main/java/com/my/plant/service)
  - Service implementations: [src/main/java/com/my/plant/service/impl](src/main/java/com/my/plant/service/impl)
  - Models: [src/main/java/com/my/plant/model](src/main/java/com/my/plant/model)
  - Utilities: [src/main/java/com/my/plant/util](src/main/java/com/my/plant/util)
- Controllers should stay thin; put business logic in the service layer.
- Follow the existing style of plain Spring annotations and Java 8 code; avoid introducing newer frameworks or Lombok unless explicitly requested.

## Runtime and configuration
- Application settings live in [src/main/resources/application.yml](src/main/resources/application.yml).
- Security is configured in [src/main/java/com/my/plant/configs/WebSecurityConfig.java](src/main/java/com/my/plant/configs/WebSecurityConfig.java).
- The app expects MongoDB connectivity to be available through the configured Spring properties.

## Build and test
- Use the Maven wrapper for verification:
  - Unix-like shells: `./mvnw test`
  - Windows: `mvnw.cmd test`
- Keep changes small and targeted unless a broader refactor is explicitly requested.

## When making changes
- Prefer matching the existing naming, annotation style, and return types used in the current controllers and services.
- If adding or changing endpoints, keep them consistent with the patterns in [src/main/java/com/my/plant/controller/BlockController.java](src/main/java/com/my/plant/controller/BlockController.java).
- If changing data access, update the service interface and implementation rather than introducing a new persistence pattern.
