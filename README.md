# academic

A Spring Boot service for academic data. It exposes a REST API backed by PostgreSQL.

## Requirements

- Java 17
- Maven (use the wrapper `./mvnw`)
- A PostgreSQL database

## Project structure

```
academic/
├── .mvn/
│   └── wrapper/                Maven wrapper
├── src/
│   ├── main/
│   │   ├── java/com/jerokysys/academic/
│   │   │   ├── AcademicApplication.java   Spring Boot entry point
│   │   │   ├── controllers/              REST controllers
│   │   │   ├── dto/                      data transfer objects
│   │   │   ├── mappers/                  MapStruct mappers
│   │   │   ├── repo/                     JPA repositories
│   │   │   └── services/                 business logic
│   │   └── resources/
│   │       ├── application.properties    Spring configuration
│   │       ├── static/                   static assets
│   │       └── templates/                view templates
│   ├── resources/
│   │   └── config/
│   │       ├── private.properties        database credentials (not committed)
│   │       └── private.properties.example
│   └── test/
│       └── java/com/jerokysys/academic/
│           └── AcademicApplicationTests.java
├── .gitignore
├── mvnw
├── mvnw.cmd
└── pom.xml
```

The folders `controllers`, `dto`, `mappers`, `repo`, `services`, `static`, and `templates` are empty for now. They exist to hold new code.

## Configure database credentials

The application reads the database credentials from `src/resources/config/private.properties`. This file is not committed to the repository.

1. Copy the example file.

```sh
cp src/resources/config/private.properties.example src/resources/config/private.properties
```

2. Open `src/resources/config/private.properties`.

3. Set the three values to match your database.

```properties
db.url=jdbc:postgresql://HOST/PORT/DATABASE
db.username=YOUR_USER
db.password=YOUR_PASSWORD
```

`application.properties` uses these values to build the datasource URL:

```properties
spring.datasource.url=${db.url}?user=${db.username}&password=${db.password}
```

**Security:** Do not commit the real `private.properties`. It contains passwords. The example file has placeholders only.

## Run the application

```sh
./mvnw spring-boot:run
```

The service starts on port `8082` by default. You can change it with the environment variable `STOCK_SERVER_PORT`.

## API documentation

OpenAPI docs are enabled. Access them at these URLs when the service runs:

- `http://localhost:8082/swagger-ui.html`
- `http://localhost:8082/v3/api-docs`

## Security

`academic` validates the JWT on its own (**second layer**): even hitting
`localhost:8082` directly requires a valid token. The API Gateway validates the
same token before routing (**first layer**), so a request never reaches this
service unauthenticated.

| Level | Where | Rule |
|---|---|---|
| Config | `jwt.*` in `application.properties`, secret in `config/private.properties` (`JWT_SECRET`) | Same key and claims as `jerokysys-auth`, HS256 |
| URL | `config/SecurityConfig.java` | Docs and `/error` public; every `GET` needs `ADMIN`, `PROFESSOR` or `SECRETARY`; every `POST`/`PUT`/`DELETE` needs `ADMIN` or `PROFESSOR` |
| Method | `@PreAuthorize` on the five services (`@EnableMethodSecurity`) | Class level: `hasAnyRole('ADMIN','PROFESSOR','SECRETARY')`; `create`, `update`, `delete`: `hasAnyRole('ADMIN','PROFESSOR')` |

| | ADMIN | PROFESSOR | SECRETARY |
|---|---|---|---|
| Read `classes`, `disciplines`, `enrollments`, `exams`, `exam-results` | yes | yes | yes |
| Write (POST/PUT/DELETE) on those | yes | yes | **403** |

Errors use the `Response` envelope and never expose stack traces:

```json
{"data":null,"message":"Access denied","code":"FORBIDDEN","status":403,"success":false}
```

Swagger (`/swagger-ui.html`) shows an **Authorize** button (`bearerAuth`), so the
401/403/2xx behaviour can be demonstrated straight from the UI.

`AcademicSecurityTest` covers: 401 for a missing, expired and wrongly signed
token, 403 for the wrong role, 2xx for the right role, and `@PreAuthorize`
allowing/denying a method call.

## Tests

```sh
./mvnw test
```

## Redis cache

The service caches DTOs in Redis to protect the database. Every microservice
runs its own Redis on `localhost:6379`, database `0`.

- Keys: `<key-prefix>:<domain>:<id>`, for example `academic:enrollment:5`
- Domains and TTLs: `cache.domains.*` in `src/main/resources/application.properties`
  (`0` = persistent, any other value = seconds)
- Reads by id go through `@Cacheable`, writes through `@CachePut` and `@CacheEvict`
- List and paginated results are never cached as a whole: each element is stored
  individually by `DomainCache`
- Every operation is logged as `Cache GET|SET|DELETE <key>` with HIT/MISS and TTL;
  the level is `logging.level.com.jerokysys.academic.config.LoggingCache`
- If Redis is unavailable, the request still succeeds against PostgreSQL and the
  failure is logged