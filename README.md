# TechForge
SWP391 - Project

## ▶️ Running the Application

### Using Maven Wrapper

The project includes Maven Wrapper, so installing Maven globally is not required.

#### Windows

```powershell
.\mvnw.cmd spring-boot:run
```

#### Linux / macOS

```bash
./mvnw spring-boot:run
```

### Using Maven

If Maven is installed:

```bash
mvn spring-boot:run
```

After the application starts successfully, open:

```text
http://localhost:8080
```

> If the application uses another port, check `server.port` in `application.properties`.
