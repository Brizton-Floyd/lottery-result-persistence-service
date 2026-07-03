# PRD - API Hardening (single-instance)

**Status:** Approved (captain plan front, all 7 workstreams approved as scoped)
**Lane:** backend (Java)
**Branch:** `harden/api-persistence-hardening`
**Review record:** `.lavish/api-hardening-plan.html`

## Intent

Harden the single-instance `lottery-result-persistence-service` against **data corruption**,
**source-fetch hangs**, and **redundant full-catalog deserialization** - without changing any
success-path response contract.

## Scope decisions (locked)

- Single-instance, low-traffic deployment. In-memory cache is sufficient; no shared cache / DB this pass.
- Keep the `.ser` file store; no database migration in this effort.
- Success (2xx) response bodies, JSON shapes, endpoint paths/methods, and the on-disk
  `tmp/{STATE}/{GAME}.ser` layout are **unchanged**.
- Error responses move from `400` + `null` body to proper `404`/`5xx` + structured JSON
  (approved, failure-path only).
- The 8000-draw cap is left as-is (product decision, not a defect).

## Workstreams

| # | Area | Change | Status |
|---|------|--------|--------|
| W1 | models | Pin `serialVersionUID` on `LotteryGame` / `LotteryDraw` to the JVM-computed values so existing `.ser` files still deserialize | Done |
| W2 | serializer | Atomic + durable `.ser` write (temp -> fsync -> atomic rename); configurable base dir | Done |
| W3 | readers | 10s connect / 30s read timeouts on CSV & PDF fetch; `PdfFileReader` static state -> instance | Done |
| W4 | polling | Resilience4j retry + rate limiter per game; bounded parallel, failure-isolated poll; safe state lookup; log at error not debug | To build |
| W5 | cache | Spring in-memory cache on hot read/catalog methods, evicted on `PollCompletedEvent` after each poll | To build |
| W6 | errors | `@RestControllerAdvice` + structured `ApiError`; 404 for missing state/game, 5xx for failures; logger/`System.out` cleanup | To build |
| W7 | tests | Point read service at `@TempDir` via configurable base dir to test real production code; serializer round-trip + atomicity tests | To build |

## Backward compatibility

- Existing persisted `.ser` files continue to load (pinned UID equals prior implicit UID, verified with `serialver`).
- Only failure-path HTTP status codes change (approved).

## Risks & mitigations

- **Pinned UID mismatch** -> computed against the current class, equals implicit value; old files load.
- **Parallel poll shared state** -> fresh reader/processor per game; PDF static state removed; rate limiter caps outbound concurrency.
- **Stale cache** -> evicted on every `PollCompletedEvent`; poll is the only writer; max staleness = one poll cycle.
- **New dependencies** -> `resilience4j-spring-boot3` + Spring cache aligned to Spring Boot 3.3 / Java 17; replaces unused 1.6.1 jars; validated by full `mvn clean install`.

## Definition of done

- All 7 workstreams implemented.
- `mvn clean install` green, including rewritten real-code catalog test and new serializer tests.
- Validation via `no-mistakes` (captain-fired), PR opened for judgment.
