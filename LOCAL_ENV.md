# Local Development Environment

This project is configured for local development with Docker-managed
infrastructure and a Maven/JDK 17 build container.

## Services

Build the backend JAR before starting the Docker services. Then create an
untracked `.env` file in the repository root:

```dotenv
DEEPSEEK_API_KEY=your-api-key
```

Start MySQL, Redis, MinIO, and the backend application:

```powershell
docker compose -f compose.dev.yml up -d
```

Stop them:

```powershell
docker compose -f compose.dev.yml down
```

The first MySQL startup imports:

- `script/sql/ry_vue_5.X.sql`
- `script/sql/ry_job.sql`
- `script/sql/ry_workflow.sql`

Local service defaults:

- `script/sql/update/add_agent_module.sql`
| Service | Address | Credentials |
| --- | --- | --- |
| MySQL | `localhost:3306`, database `ry-vue` | `root` / `root` |
| Redis | `localhost:6379` | password `ruoyi123` |
| MinIO API | `http://localhost:9000` | `ruoyi` / `ruoyi123` |
| MinIO Console | `http://localhost:9001` | `ruoyi` / `ruoyi123` |

Persistent service data is stored under `.docker/` and is ignored by Git.

## Backend Build

If JDK 17/21 and Maven are installed locally:

```powershell
mvn -P local -DskipTests package
```

Without local Java/Maven, use Docker:

```powershell
docker run --rm -v "${PWD}:/workspace" -v ruoyi-m2:/root/.m2 -w /workspace maven:3.9.11-eclipse-temurin-17 mvn -P local -DskipTests package
```

The built backend jar is:

```text
ruoyi-admin/target/ruoyi-admin.jar
```

## Backend Profile

`ruoyi-admin/src/main/resources/application-local.yml` includes the default
`dev` profile, then disables Spring Boot Admin client registration and SnailJob
client registration for a lighter first local startup.

Use the `local` Maven profile. It activates Spring profiles `dev,local`, so the
default dev settings are loaded first and local overrides win afterward.

If running without Maven resource filtering, set:

```powershell
$env:SPRING_PROFILES_ACTIVE="dev,local"
```
