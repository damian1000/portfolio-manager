# TODO

## Roadmap (prioritized)

### P1 — honest claims + withdrawal safety (do before pinning this repo)

- **Audit README against code.** An external review flagged that the README advertises position aggregation and a common Gateway interface, neither of which exists today. Either implement them (see P2) or rewrite the README so the project is positioned honestly as "two venue-local exchange clients with shared HTTP/signing utilities". Possibly rename the repo to "Crypto Exchange Connectivity".
- **Add withdrawal safety before any further promotion.** Currently `bitfinex/Main.kt --withdraw <currency> <amount> <address>` will submit a real withdrawal with one CLI flag. Minimum bar before showcasing this repo:
  - Dry-run by default; require `--confirm-withdrawal` to actually submit.
  - Positive-amount validation and currency whitelist check before any network call.
  - Address redaction in logs (current code logs the destination address in plain text).
  - Non-zero exit codes from the CLI on every failure path.
  - A persistent audit record for every authenticated withdrawal call (timestamp, currency, amount, redacted destination, response).
- Until those land, remove withdrawal usage from the prominent README example.

### P2 — pick one depth investment

The review's "make this a real connectivity layer" framing is correct; one focused investment moves the needle more than nibbling. Pick one:

- **Real cross-venue aggregation.** A small `Portfolio` service that asks both gateways for balances/movements, normalises to a venue-agnostic model, and surfaces a unified view. Demonstrates the shared `Gateway` interface honestly.
- **Position reconciliation.** Given a local set of positions and a fresh venue snapshot, identify drift (missing fills, extra fills, size mismatches) and surface it as a structured report.

### P3 — stretch (later)

- Rate-limit handling + retry policy per venue (per-exchange headers/codes).
- Typed response models in place of raw `String` returns.
- Recorded-response contract tests against saved exchange payloads.

## Bug Fixes

- Replace broad `catch (e: Exception)` blocks in CLI entry points with targeted failures and non-zero process exits.
- Remove unused fields and commented-out copied Bitfinex code from `BinanceGateway`.
- Validate CLI currency arguments before making network requests.
- Avoid printing full signed Binance URLs because the signature is sensitive request metadata.

## Design Review Changes

- Introduce a small shared `Gateway` interface if both venues need to be consumed polymorphically by a portfolio service.
- Keep signing, endpoint paths, and DTOs venue-local; avoid a shared base client unless duplication becomes substantial.
- Replace raw `String` responses from gateway methods with typed response models where API shape is stable.
- Extract common movement model only if downstream portfolio logic needs venue-agnostic movement handling.
- Separate CLI/demo code from gateway/library code so production code is not coupled to `println` and command-line parsing.

## Production Readiness

- Add rate-limit handling and retry policy per venue, respecting exchange-specific response headers and error codes.
- Add request timeouts to Apache HttpClient instances.
- Add idempotency handling for withdrawal workflows where supported, and local duplicate-request protection where not supported.
- Add typed error handling for authentication failures, permission failures, rate limits, invalid parameters, and exchange maintenance.
- Add integration-test stubs or contract tests using recorded exchange responses.
- Add dependency vulnerability scanning in CI.
- Consider lowering the Java toolchain requirement from JDK 25 if broad user compatibility matters.

## Documentation

- Document exact commands for running the Binance and Bitfinex entry points separately.
- Document the withdrawal command with a strong warning and a dry-run example once dry-run exists.
- Add examples of expected environment-variable setup on Windows PowerShell and Unix shells.
- Document which API key permissions are required for read-only balance/history calls versus withdrawal calls.
- Add an architecture note explaining why venue-specific request signing is intentionally not shared.

## Cleanup

- Remove unused dependencies such as Lombok, Commons IO, Commons Lang, Mockito, or Hamcrest if they remain unused after test cleanup.
- Add a formatter or linter task and run it in CI.
- Keep generated build outputs and IDE files out of version control; current `.gitignore` already covers these.
