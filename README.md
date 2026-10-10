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

## Tests

```sh
./mvnw test
```