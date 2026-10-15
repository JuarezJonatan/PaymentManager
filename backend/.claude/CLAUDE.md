# CLAUDE.md

This file defines the mandatory conventions for the `payment-manager` backend (folder `backend/` inside the `payment-manager` repository). All generated or modified code under `backend/` MUST comply with these rules. The frontend has its own conventions in `frontend/.claude/CLAUDE.md`.

## Project Context

- Language: Java 21
- Framework: Spring Boot 4.x (Spring Web MVC)
- Build tool: Maven, invoked through the wrapper (`./mvnw`)
- Base package: `com.jejuarez.paymentmanager`
- Persistence: PostgreSQL (local via Docker Compose, `compose.yaml` at the repository root) and deployed environments; in-memory H2 only for tests; Flyway migrations run only locally (see section 8)
- API documentation: springdoc-openapi

### Common Commands

Run `docker compose` from the repository root and `./mvnw` from `backend/`:

```bash
docker compose up -d         # start local PostgreSQL (host port 5433, override with POSTGRES_PORT)
./mvnw clean verify          # build and run all tests (unit tests via Surefire, *IT via Failsafe)
./mvnw test                  # run unit tests only (*IT classes are NOT executed)
./mvnw spring-boot:run       # start the application
```

## 1. Layered Architecture

The application MUST follow a strict three-layer architecture: **Controller → Service → Repository**.

| Layer | Responsibility | Allowed Dependencies |
|-------|----------------|----------------------|
| **Controller** | Expose REST endpoints, validate input, delegate to services, and return DTOs. It MUST NOT contain business logic. | Service, DTOs |
| **Service** | Contain all business logic, transaction boundaries, and orchestration. | Repository, Mapper, Entities, DTOs |
| **Repository** | Data access only (Spring Data interfaces). It MUST NOT contain business logic. | Entities |

Rules:

- A layer MUST only depend on the layer directly beneath it. Controllers MUST NOT access repositories directly.
- Controllers MUST NOT receive or return JPA entities; they exchange DTOs exclusively.
- Entities MUST NOT leak outside the service layer.
- Use constructor injection exclusively (via Lombok `@RequiredArgsConstructor` with `private final` fields). Field injection (`@Autowired` on fields) is forbidden.

### Package Structure

Organize the code by layer under the base package:

```
com.jejuarez.paymentmanager
├── controller
├── service
├── persistence
│   ├── entity
│   └── repository
├── dto
│   ├── request
│   └── response
├── mapper
└── common
    └── exception
```

`common` holds only cross-cutting code shared by any resource (e.g., exceptions and the global exception handler). Resource-specific classes (DTOs, mappers) MUST NOT be placed in `common`.

## 2. Behavior-Driven Development (BDD)

All features MUST be developed following BDD principles, with tests expressing the expected behavior of the system.

- Test method names MUST fully describe the scenario using the **given / when / then** structure, so that no explanatory comments are required.
- Naming format: `given<Precondition>_when<Action>_then<ExpectedOutcome>`
  - Example: `givenExistingPayment_whenFindById_thenReturnsPayment`
  - Example: `givenNonExistentPaymentId_whenFindById_thenThrowsResourceNotFoundException`
  - Example: `givenInvalidAmount_whenCreatePayment_thenReturnsBadRequest`
- Comments such as `// given`, `// when`, `// then` (or `// arrange`, `// act`, `// assert`) are **forbidden**. The three phases MUST be separated only by a blank line, and the intent MUST be conveyed through descriptive method names and well-named variables.
- Every test class and test method MUST have a JUnit `@DisplayName` with a short, human-readable English sentence describing the behavior (e.g., `@DisplayName("GET /payments/{id} returns 404 when the payment does not exist")`). The method name keeps the `given_when_then` format.
- Use Mockito's BDD API (`BDDMockito.given(...)`, `then(...).should(...)`) instead of `when(...)` / `verify(...)`.
- Use AssertJ (`assertThat`) for assertions.
- Each test MUST verify a single behavior.
- Unit tests for services MUST mock their collaborators; controller tests MUST use `@WebMvcTest` with mocked services.

Example:

```java
@Test
@DisplayName("Returns the mapped payment when the id exists")
void givenExistingPayment_whenFindById_thenReturnsPaymentResponse() {
    Payment payment = aPaymentWithId(PAYMENT_ID);
    PaymentResponse expectedResponse = aPaymentResponseWithId(PAYMENT_ID);
    given(paymentRepository.findById(PAYMENT_ID)).willReturn(Optional.of(payment));
    given(paymentMapper.toResponse(payment)).willReturn(expectedResponse);

    PaymentResponse response = paymentService.findById(PAYMENT_ID);

    assertThat(response).isEqualTo(expectedResponse);
}
```

### Integration Tests

- Integration tests MUST run against an in-memory **H2** database. They MUST NOT depend on PostgreSQL or any other external database.
- H2 configuration for tests MUST be defined in `src/test/resources/application-test.yaml`, and integration test classes MUST activate it with `@ActiveProfiles("test")`.
- H2 MUST run in PostgreSQL compatibility mode (e.g., `jdbc:h2:mem:testdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH`) so that the SQL migrations behave consistently with PostgreSQL.
- The schema MUST be created by the Flyway migrations (Flyway is enabled for the `test` profile, as it runs locally); `spring.jpa.hibernate.ddl-auto` MUST be set to `validate` (or `none`), never `create` or `update`.
- Integration tests MUST be organized by use case, one class per use case (e.g., `CreatePaymentIT`, `GetPaymentByIdIT`), in the `integration` test package. They MUST extend `IntegrationTestSupport`, which holds the shared Spring Boot test configuration and data cleanup.
- Integration test classes MUST use the `IT` suffix and follow the same BDD naming rules described above. They are executed by the `maven-failsafe-plugin` during `verify`.
- Each test MUST be independent and leave no residual data, for example by using `@Transactional` rollback or cleaning the affected tables before each test.

## 3. DTOs as Java Records

- All DTOs (request and response objects) MUST be implemented as Java `record` types.
- DTOs MUST be immutable and MUST NOT contain business logic.
- Apply Jakarta Bean Validation annotations (`@NotNull`, `@NotBlank`, `@Positive`, etc.) directly on request record components, and trigger validation with `@Valid` in controllers.
- Naming: `<Resource>Request` for input DTOs and `<Resource>Response` for output DTOs (e.g., `CreatePaymentRequest`, `PaymentResponse`).

## 4. Lombok Usage

Lombok MUST be used wherever it eliminates boilerplate, including but not limited to:

- `@RequiredArgsConstructor`

Restrictions:

- Do NOT use `@Data` on JPA entities, since its generated `equals`/`hashCode`/`toString` cause problems with lazy loading and bidirectional relationships.
- Do NOT apply Lombok to records; records already provide constructors, accessors, `equals`, `hashCode`, and `toString`.

## 5. Mappers

- Conversion between entities and DTOs MUST be performed exclusively by dedicated mapper classes located in the `mapper` package (e.g., `PaymentMapper`).
- Mappers MUST be Spring components (`@Component`) injected into services.
- Mapping logic MUST NOT be placed in controllers, services, entities, or DTOs.
- Typical method names: `toEntity(<Request>)`, `toResponse(<Entity>)`, `toResponseList(List<Entity>)`.

## 6. Centralized Error Handling

- All exception handling MUST be centralized in a single class annotated with `@RestControllerAdvice`, named `GlobalExceptionHandler`, located in the `common.exception` package.
- Controllers and services MUST NOT catch exceptions in order to build HTTP responses; they throw domain-specific exceptions instead.
- Define custom, meaningful exceptions for domain errors, extending `RuntimeException`.
- Missing resources MUST be reported with the generic `ResourceNotFoundException(resourceName, resourceId)` (e.g., `new ResourceNotFoundException("Payment", id)`), which produces the message `"<resourceName> with id '<resourceId>' was not found"`. Do NOT create one not-found exception per resource.
- Each handled exception MUST be mapped to the appropriate HTTP status code, for example:
  - Resource not found → `404 Not Found`
  - Validation errors (`MethodArgumentNotValidException`) → `400 Bad Request`
  - Business rule violations → `409 Conflict` or `422 Unprocessable Entity`
  - Unexpected exceptions → `500 Internal Server Error`
- Error responses MUST use a consistent structure: either Spring's `ProblemDetail` (RFC 9457) or a dedicated `ErrorResponse` record containing, at minimum, the timestamp, HTTP status, error message, and request path.
- This project uses `ProblemDetail`: `GlobalExceptionHandler` extends `ResponseEntityExceptionHandler` (so standard Spring MVC errors share the same format), and every custom handler sets `title`, `detail`, and a `timestamp` property; Spring fills `instance` with the request path.
- Unexpected exceptions MUST be logged; internal details (stack traces, SQL messages) MUST NOT be exposed to clients.

## 7. General Code Style

- Code MUST be self-explanatory through clear naming; avoid redundant comments.
- Prefer immutability (`final` fields, records, unmodifiable collections).

## 8. Database Migrations (Flyway)

- Flyway MUST be enabled **only in the local environment**: the `local` profile (PostgreSQL via Docker Compose) and the `test` profile used by integration tests.
- Flyway MUST be disabled in every other environment by setting `spring.flyway.enabled=false` in the base `application.yaml` and enabling it explicitly only in `application-local.yaml` and `src/test/resources/application-test.yaml`.
- In non-local environments, the schema is managed outside the application; `spring.jpa.hibernate.ddl-auto` MUST be set to `validate` (or `none`).
- Schema changes MUST be written as Flyway migrations in `src/main/resources/db/migration`, following the naming convention `V<version>__<description>.sql`. Applied migrations MUST NOT be modified; create a new versioned migration instead.
