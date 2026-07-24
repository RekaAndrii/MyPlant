# AGENTS instructions for MyPlant

## Project overview
- Spring Boot **3.3.5** application (Java 21 source level, runs on JDK 21 in Docker) for a plant-tracking web app.
- Main entry point: [src/main/java/com/my/plant/configs/MyPlantApplication.java](src/main/java/com/my/plant/configs/MyPlantApplication.java)
- Server-rendered views use Thymeleaf: [src/main/resources/templates](src/main/resources/templates); static assets in [src/main/resources/static](src/main/resources/static).
- Persistence: `MongoTemplate` (primary). `BlockRepository` (extends `MongoRepository`) exists but is dead code — do not use it.
- Security: stateful session-based auth via `SecurityFilterChain @Bean` in [src/main/java/com/my/plant/configs/WebSecurityConfig.java](src/main/java/com/my/plant/configs/WebSecurityConfig.java).
- API docs available at `/swagger-ui.html` (Springdoc OpenAPI 2.6.0).

## Code organization
Keep the existing package structure — do not introduce new top-level packages:
- `configs/`     — Spring Boot app entry point, security, MongoDB, MVC configuration
- `controller/`  — Thin HTTP handlers only; no business logic
- `service/`     — Service interfaces
- `service/impl/` — Service implementations annotated `@Service`
- `model/`       — MongoDB document classes (`@Document`)
- `repository/`  — Unused `MongoRepository` stub; do not add to this layer
- `util/`        — Stateless helpers, comparators (`util/comparator/`), enums (`util/constant/`), DTOs (`util/dto/`)

## Build and test commands

**Run all tests (Windows):**
```
mvnw.cmd test
```

**Run all tests (Unix):**
```
./mvnw test
```

**Run a single test class:**
```
mvnw.cmd test -Dtest=HexEncodingTest
./mvnw test -Dtest=HexEncodingTest
```

**Run a single test method:**
```
mvnw.cmd test -Dtest=HexEncodingTest#hexEncodingRoundTripShouldWork
./mvnw test -Dtest=HexEncodingTest#hexEncodingRoundTripShouldWork
```

**Compile without running tests:**
```
mvnw.cmd compile
./mvnw compile
```

**Full build (skip tests):**
```
mvnw.cmd package -DskipTests
./mvnw package -DskipTests
```

There is no lint or code-format enforcer configured. There are no Cursor rules or Copilot instructions files in this repo.

## Runtime configuration
Settings live in [src/main/resources/application.yml](src/main/resources/application.yml). The app reads secrets from a `.env` file via `spring.config.import: optional:file:./.env[.properties]`.

Required environment variables:
- `MONGODB_URI` — MongoDB connection string (required)
- `MONGODB_DATABASE` — database name (optional, defaults to `MyPlant`)
- `PORT` — server port (optional, defaults to `8080`)

The JVM default timezone is forced to `Europe/Kiev` in `MyPlantApplication.@PostConstruct`. `BlockController.executeBlock` additionally applies `.minusHours(3)` when writing `lastExecution` — a hardcoded UTC+3 offset. Do not remove this without updating both.

## Naming conventions
- **Packages**: lowercase singular nouns (`controller`, `service`, `model`, `util`)
- **Classes**: `PascalCase`; service implementations follow `<Interface>Impl` (e.g., `BlockServiceImpl`)
- **Interfaces**: plain name, no `I` prefix (e.g., `BlockService`)
- **Methods/fields**: `camelCase`
- **Constants**: `UPPER_SNAKE_CASE`
- **MongoDB field name**: `userName` (camelCase) — used consistently in all models and queries
- **Preserved typo**: `getLogginedUserName()` in `UserUtil` (double-`g`) — all callers use this exact spelling; do not rename it

## Annotation and injection style
- Use **field injection** with `@Autowired` (the existing style); do not switch to constructor injection unless explicitly asked.
- Use `@Service`, `@Controller`, `@Repository`, `@Configuration` for stereotypes.
- Both `@RequestMapping(method = RequestMethod.GET)` and shorthand annotations (`@GetMapping`, `@PostMapping`, `@DeleteMapping`) appear in the codebase — prefer shorthand for new endpoints but do not normalize existing ones.
- Apply `@Operation(summary=..., operationId=...)` only to endpoints that need Swagger documentation.
- Do not add Lombok — all models use manually written no-arg constructors, full-arg constructors, getters, and setters.

## Data access pattern
- **Always use `MongoTemplate`** — inject it directly into `@Service` implementations.
- Build queries with `new Query()` + `query.addCriteria(Criteria.where("field").is(value))`.
- For multi-field AND conditions use `new Criteria().andOperator(Criteria.where(...), Criteria.where(...))`.
- **Every query must be scoped by `userName`** — the app is multi-tenant by username.
- New document classes go in `model/`, annotated `@Document(collection = "collectionName")`, with `@Id` on the `_id` field.
- `@Transient` fields are computed at read-time and never persisted (e.g., `Block.color`).

## Controller pattern
- View-rendering controllers return `ModelAndView`, call `model.setViewName("template")` and `model.addObject("key", value)`.
- REST/AJAX endpoints are on a `@Controller @ResponseBody` class (like `BlockController`) or use `@RestController`.
- AJAX success responses return `new AjaxResponse(false, "ok")`.
- Resolve the current user inline: `UserUtil.getLogginedUserName()` — do not thread username as a method parameter through service layers.
- Record history events in the **controller** (see `BlockController.executeBlock`), not inside service methods.

## Error handling
- In view controllers, wrap service calls in `try/catch`, log with `LOGGER.error(message, ex)`, and add an `"errorMessage"` string to the model — do not let exceptions propagate to the user as stack traces.
- AJAX controllers currently return `AjaxResponse(false, "ok")` for all success paths with no dedicated error response path — match this pattern unless adding structured error handling is explicitly requested.
- No `@ControllerAdvice` or global exception handler exists; add one only if explicitly asked.
- Use `LoggerFactory.getLogger(ClassName.class)` for loggers; declare them `private static final Logger LOGGER`.

## Code formatting
- 4-space indentation; opening braces on the same line.
- Java 8 features: `LocalDate`/`LocalDateTime`, streams, lambdas — all acceptable.
- Do **not** use `Optional` — the codebase uses manual null checks throughout.
- Wildcard imports (`java.util.*`, `org.springframework.web.bind.annotation.*`) are acceptable where a package has many used classes.
- Class-level Javadoc is the `/** Created by User on DD.MM.YYYY. */` style; do not add elaborate Javadoc unless asked.

## Color / staleness logic
- `ColorUtil.setColor(Block)` assigns `@Transient BlockColor`:
  - Same day → `GREEN`; 1 day ago → `YELLOW`; 2+ days or `null` → `RED`
- Blocks are sorted by `lastExecution` ascending (oldest/never-executed first) in `HomeController`.
- `BlockColor` values are CSS class names (`"red"`, `"yellow"`, `"green"`) applied in Thymeleaf via `th:classappend="${bl.color.value}"`.

## Frontend conventions
- jQuery 3.2.1, Bootstrap 4, Morris.js (bar charts), Raphael — all bundled under `src/main/resources/static/`.
- Do not add npm, a bundler, or transpilation steps — all JS is plain ES5-compatible.
- AJAX calls in `blocks.js` use `$.ajax`; follow the same pattern for any new client-side requests.
- Thymeleaf templates use `th:each`, `th:classappend`, `th:if`, and `th:text`; keep the same dialect.

## When making changes
- Keep controllers thin — move logic to the service layer.
- New endpoints must follow `BlockController.java` patterns (method annotations, `@ResponseBody`, `AjaxResponse` return type).
- Changing data access means updating the service interface **and** its implementation — never bypass the service layer from a controller.
- Do not introduce new persistence patterns (e.g., reactive repositories, JPA) without an explicit request.
- Keep changes small and targeted; avoid unsolicited refactors.

## Frontend review checklist

After **every** change that touches `blocks.js`, `index.html`, or `blocks.css`, verify the following before considering the task done:

### jQuery selectors
- [ ] Use `$(this).children("span")` (direct child) not `$(this).find("span")` (deep search) when the tile contains nested elements that share the same tag.
- [ ] Any selector that targets elements by class (e.g. `.block-item`, `.block-edit-icons`) must **not** accidentally match newly added children with the same tag/class.

### Click handlers and event propagation
- [ ] Buttons nested inside a `.block-item` tile (edit icons, delete confirm) must call `e.stopPropagation()` to prevent the tile's own click handler from firing.
- [ ] Every new block-level click handler must start with `if (editModeActive) return;` so edit mode guards execute flow.

### Edit mode guard
- [ ] Any new clickable element on the home page that should only work in normal mode must check `editModeActive`.
- [ ] Any element that should only work in edit mode must also guard on `editModeActive`.

### DOM assumptions
- [ ] JS that reads a block name must use `data-block-name` attribute (set via `th:attr="data-block-name=${bl.name}"`), not inner text, whenever the tile contains other text nodes.
- [ ] Any new tile-type element added to the block grid (e.g. `#addBlockBtn`) must be excluded from block-execution selectors with `.not("#theId")`.

### Modal hygiene
- [ ] Every new modal must have: a `reset` function, a Cancel button that calls reset, a backdrop-click handler (`$(e.target).is("#modalId")`), and focus set on the primary input when opened.
- [ ] `resetXxxForm()` must clear **all** fields and sub-sections (checkboxes unchecked, conditional divs hidden, inputs emptied).

### CSS positioning
- [ ] New elements added **inside** `.block-item` (which is `position:relative`) that use `position:absolute` must set correct `z-index` so they layer above the tile content but below modals (`z-index < 1050`).
- [ ] Flex children of `.block-item` must not break the existing `align-items:center; justify-content:center` centering of the name span.

### `data-*` attributes
- [ ] Every block tile rendered by `th:each` must carry `th:attr="data-block-name=${bl.name}"`. Verify after any restructure of the tile markup.

### Thymeleaf / server-side
- [ ] New attributes driven by server data use `th:attr` or dedicated `th:` attributes — no string concatenation in `style` or `class` attributes.
- [ ] New template fragments follow the existing Bootstrap 4 grid (`col-xs-*`) and do not introduce inline widths that break the mobile layout.



Skills live under `.github/skills/`. Read the corresponding `SKILL.md` before executing. Trigger automatically on matching user phrases.

| Skill | Location | Trigger phrases |
|---|---|---|
| **myplant-e2e-test** | `.github/skills/myplant-e2e-test/` | run e2e test, smoke test, quick e2e, end-to-end test, full flow test, test user registration to cleanup |
| **myplant-run-and-token-saver** | `.github/skills/myplant-run-and-token-saver/` | run app, start app, start service, launch MyPlant, health check, reduce tokens, concise output |
| **myplant-verify-running** | `.github/skills/myplant-verify-running/` | verify app is running, is app up, check app status, check service health, ping app |
| **myplant-free-8080** | `.github/skills/myplant-free-8080/` | free 8080, kill process on 8080, stop port 8080, use 8080, do not use 8181 |
| **myplant-commit-with-push-confirmation** | `.github/skills/myplant-commit-with-push-confirmation/` | commit changes, create commit, git commit, save to git, push changes, commit and push |
| **myplant-ui-review** | `.github/skills/myplant-ui-review/` | review ui, review frontend, check ui, ui review, frontend review, review blocks.js, review index.html |
