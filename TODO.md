# TODO

- Add real cross-venue aggregation: a `Portfolio` service that asks both gateways for balances/movements, normalises to a venue-agnostic model, and surfaces a unified view.
- Add position reconciliation: given local positions and a fresh venue snapshot, identify drift (missing fills, extra fills, size mismatches) and surface it as a structured report.
- Add per-venue rate-limit handling and retry policy (exchange-specific headers/codes).
- Replace raw `String` gateway responses with typed response models.
- Add recorded-response contract tests against saved exchange payloads.
- Migrate Apache HttpClient `RequestConfig.setConnectTimeout` (deprecated) to `ConnectionConfig` on the connection manager (both `binance/HttpMessageSender.kt` and `bitfinex/HttpMessageSender.kt`).
