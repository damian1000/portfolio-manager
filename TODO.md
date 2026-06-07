# TODO

## Next (Highest Leverage)

- **HMAC signing tests for both venues.** The signing code is the most security-critical thing in this repo and currently has no unit-test coverage. Cover: canonical query-string ordering, `timestamp`/`recvWindow`, and the exact signature payload for Binance; nonce monotonicity and JSON body construction for Bitfinex. Use known-answer vectors so a future refactor can't silently break the signature.

## Bug Fixes

- Add tests for Binance signed request construction, including query-string ordering, `timestamp`, `recvWindow`, and `signature`.
- Add tests for Bitfinex authenticated request construction, including nonce, signature payload, headers, and JSON body.
- Add mapper tests using representative Binance and Bitfinex movement payloads, including null optional fields and short malformed arrays.
- Replace broad `catch (e: Exception)` blocks in CLI entry points with targeted failures and non-zero process exits.
- Remove unused fields and commented-out copied Bitfinex code from `BinanceGateway`.
- Validate CLI currency arguments before making network requests.
- Avoid printing full signed Binance URLs because the signature is sensitive request metadata.

## Safety

- Add a second explicit confirmation gate for withdrawals, such as `--confirm-withdrawal`, before calling live withdrawal endpoints.
- Add a dry-run mode for withdrawals that prints the request summary without submitting it.
- Require withdrawal destination, currency, and amount to be echoed back in a structured confirmation message.
- Add minimum validation for withdrawal amount format and positivity.
- Add structured audit logging for every authenticated call, redacting API keys, signatures, addresses where appropriate, and secrets.

## Design Review Changes

- Introduce a small shared `Gateway` interface if both venues need to be consumed polymorphically by a portfolio service.
- Keep signing, endpoint paths, and DTOs venue-local; avoid a shared base client unless duplication becomes substantial.
- Replace raw `String` responses from gateway methods with typed response models where API shape is stable.
- Move HTTP sender construction behind injectable dependencies so request builders can be unit-tested without real network clients.
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
