# MyPlant

MyPlant is a Spring Boot web application for tracking plant-related blocks and actions. It uses Thymeleaf for server-rendered views and MongoDB for persistence.

## Features
- Simple authentication with in-memory users
- Plant/block management through web endpoints
- MongoDB-backed persistence
- Swagger UI support

## Requirements
- Java 11
- Maven Wrapper
- MongoDB connection provided through `MONGODB_URI`

## Run locally
From the project root, run:

```bash
./mvnw spring-boot:run
```

On Windows PowerShell:

```powershell
./mvnw.cmd spring-boot:run
```

The application will start on:

- http://localhost:8080

## Hot UI Reload (No Redeploy)
UI/template changes can be picked up during local development without rebuilding the jar.

- Run app normally: `./mvnw spring-boot:run` (or `./mvnw.cmd spring-boot:run` on Windows)
- Edit files under `src/main/resources/templates` or `src/main/resources/static`
- Refresh browser to see updates

Notes:

- `spring.thymeleaf.cache` is disabled for template refresh
- Static resource cache is disabled for CSS/JS updates
- Spring DevTools LiveReload is enabled (use a browser LiveReload extension for auto-refresh)

## Run with Docker
Build the image:

```bash
docker build -t myplant .
```

Run it with a MongoDB connection string:

```bash
docker run --rm -p 8080:8080 -e MONGODB_URI="mongodb+srv://<user>:<password>@<cluster>/MyPlant?retryWrites=true&w=majority" myplant
```

If your platform assigns a port, set `PORT` as well:

```bash
docker run --rm -p 8080:8080 -e PORT=8080 -e MONGODB_URI="mongodb+srv://<user>:<password>@<cluster>/MyPlant?retryWrites=true&w=majority" myplant
```

## Stop app on port 8080 (PowerShell)
If port 8080 is already in use, stop the process with:

```powershell
Get-NetTCPConnection -LocalPort 8080 -State Listen | Select-Object -ExpandProperty OwningProcess -Unique | ForEach-Object { Stop-Process -Id $_ -Force }
```

## OpenAPI
Swagger UI is available at:

- http://localhost:8080/swagger-ui/index.html

OpenAPI JSON is available at:

- http://localhost:8080/v3/api-docs

## Default login
The app includes two in-memory users:

- Username: `andrii` | Password: empty
- Username: `taras` | Password: `password`

## URL authentication for /home
You can authenticate directly from a URL using the quick-login endpoint, then get redirected to `/home`.

Endpoint:

- `GET /quick/home?login=<username>&password=<password>`

Examples:

- Empty password user (`andrii`):
	- `http://localhost:8080/quick/home?login=andrii&password=`
- User with password (`taras`):
	- `http://localhost:8080/quick/home?login=taras&password=password`

What happens:

- The endpoint authenticates the credentials.
- On success, it creates an authenticated session.
- It redirects to `/home`.

Important:

- Sending credentials in a URL is not secure for production because URLs can be logged in browser history, proxies, and server logs.
- Prefer standard form login (`/login`) for normal usage.

## Notes
- The current project uses Spring Boot 1.5.2 and Java 11 for compatibility.
- If you change the MongoDB connection settings, update [src/main/resources/application.yml](src/main/resources/application.yml) accordingly.
