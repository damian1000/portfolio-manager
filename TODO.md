# TODO

## Roadmap (prioritized)

### P1 — done

All P1 items have landed:

- README rewritten to honest "two venue-local clients" framing (no false
  shared `Gateway` interface, no aggregation claim).
- Bitfinex withdrawal CLI: dry-run by default, `--confirm-withdrawal`
  required for live submission, positive-amount + currency-whitelist +
  address validation, address redaction in logs, audit row per attempt
  at `~/.portfolio-manager/bitfinex-withdrawals.log` (override with
  `PORTFOLIO_AUDIT_DIR`), sysexits-style exit codes.

### P2 — pick one depth investment

The review's "make this a real connectivity layer" framing is correct; one focused investment moves the needle more than nibbling. Pick one:

- **Real cross-venue aggregation.** A small `Portfolio` service that asks both gateways for balances/movements, normalises to a venue-agnostic model, and surfaces a unified view. This is the one that would justify reinstating the README's old "aggregation" framing.
- **Position reconciliation.** Given a local set of positions and a fresh venue snapshot, identify drift (missing fills, extra fills, size mismatches) and surface it as a structured report.

### P3 — stretch (later)

- Rate-limit handling + retry policy per venue (per-exchange headers/codes).
- Typed response models in place of raw `String` returns.
- Recorded-response contract tests against saved exchange payloads.

## Bug Fixes

- Replace broad `catch (e: Exception)` blocks in the Binance CLI entry point with targeted failures and non-zero process exits. (Bitfinex Main was rewritten as part of P1; Binance Main is still the old shape.)
- Remove unused fields and commented-out copied Bitfinex code from `BinanceGateway`.
- Avoid printing full signed Binance URLs in failure messages — `HttpMessageSender` currently includes the signed URL in the thrown `RuntimeException`, leaking signature material to error logs.

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

## Documentation

- Document exact commands for running the Binance and Bitfinex entry points separately. (Bitfinex withdraw flow is documented; Binance and Bitfinex read-only invocation could be clearer.)
- Add examples of expected environment-variable setup on Windows PowerShell and Unix shells.
- Document which API key permissions are required for read-only balance/history calls versus withdrawal calls.
- Add an architecture note explaining why venue-specific request signing is intentionally not shared.

## Cleanup

- Remove unused dependencies such as Lombok, Commons IO, Commons Lang, Mockito, or Hamcrest if they remain unused after test cleanup.
- Add a formatter or linter task and run it in CI.
