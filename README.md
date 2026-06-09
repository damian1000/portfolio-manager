# Portfolio Manager

[![CI](https://github.com/damian1000/portfolio-manager/actions/workflows/ci.yml/badge.svg)](https://github.com/damian1000/portfolio-manager/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/damian1000/portfolio-manager/graph/badge.svg)](https://codecov.io/gh/damian1000/portfolio-manager)
[![Kotlin](https://img.shields.io/badge/kotlin-2.3.21-blueviolet)](https://kotlinlang.org/)
[![JDK](https://img.shields.io/badge/jdk-25-orange)](https://openjdk.org/projects/jdk/25/)

Two Kotlin **venue-local exchange clients** — `BinanceGateway` and `BitfinexGateway` — that read wallet balances and movement history from **Binance** and **Bitfinex** via their authenticated REST APIs. They share neither a base class nor an interface; what they share is shape and approach, not types. A third exchange would be one new package, modeled on the existing two.

## What it demonstrates

- **Per-venue gateways** — `BinanceGateway` covers system status / wallets; `BitfinexGateway` covers wallets, movements, settings, and (opt-in) withdrawals. Each is concrete and venue-local — no shared `Gateway` interface.
- **`MessageSender` boundary** — HTTP is behind an interface, so signing logic, request building, and response mapping are unit-testable with no network (see `binance/MessageSender.kt`, `bitfinex/MessageSender.kt`)
- **Withdrawal safety** — Bitfinex's `--withdraw` CLI is dry-run by default; an actual submission requires the explicit `--confirm-withdrawal` flag, and every attempt is appended to a redacted audit log. Mistakes here cost money, so the bar is "you have to mean it".
- **Secret handling** — API key/secret read from env vars only. No creds in code, no creds in committed config, no creds in the JAR
- **Per-venue request signing** — HMAC-SHA256 (Binance) and HMAC-SHA384 (Bitfinex), each with a unit-tested signer

## Architecture

```
   ┌─────────────────────┐                ┌──────────────────┐
   │  BinanceGateway     │───────────────▶│ Binance REST     │
   │   ├─ MessageSender  │  signed HTTP   │ api.binance.com  │
   │   └─ SigningHelper  │                └──────────────────┘
   └─────────────────────┘
   ┌─────────────────────┐                ┌──────────────────┐
   │  BitfinexGateway    │───────────────▶│ Bitfinex REST    │
   │   ├─ MessageSender  │  signed HTTP   │ api.bitfinex.com │
   │   └─ SigningHelper  │                └──────────────────┘
   └─────────────────────┘
```

Each venue lives in its own package — no shared "base client" inheritance, no premature unification. The two gateways look similar but are deliberately independent; if a `Portfolio` aggregation layer is ever added it would compose them, not generalise them.

## Prerequisites

- JDK 25
- An exchange account with API key + secret on whichever venue you want to hit

### Java compatibility

Java 25 is the supported baseline: Kotlin and Java sources compile to JVM 25 bytecode, so older runtimes are not supported. The checked-in Gradle wrapper pins the build tooling, but it does not install the JDK; install a JDK 25 distribution and make it available through `JAVA_HOME` or `PATH`.

```bash
java -version
./gradlew --version
```

## Setup

```bash
cp .env.example .env
# edit .env, then `source .env` (or use direnv)
```

| Variable | Used by |
|---|---|
| `BINANCE_API_KEY` | Binance |
| `BINANCE_API_SECRET` | Binance |
| `BITFINEX_API_KEY` | Bitfinex |
| `BITFINEX_API_SECRET` | Bitfinex |

> **Use read-only API keys when exploring.** Withdrawal endpoints need explicit trade/withdraw scopes. Only enable those scopes when you actually intend to call `submitWithdrawalRequest`.

## Running

The Gradle `application` plugin entry point is `binance.MainKt`:

```bash
./gradlew run
```

For the Bitfinex side, either change `mainClass` in `build.gradle` or run `bitfinex.MainKt` directly. The Bitfinex `Main` defaults to read-only behavior:

1. List wallet balances
2. List recent movements for a currency
3. Print API key permissions

Withdrawal is opt-in **and dry-run by default**:

```bash
# Dry run (default): logs the redacted intent + writes a DRY_RUN audit row, no network call.
./gradlew run --args="--withdraw BTC 0.01 destination-address"

# Actually submit. Requires the explicit confirm flag — easy to omit, by design.
./gradlew run --args="--withdraw BTC 0.01 destination-address --confirm-withdrawal"
```

Every withdrawal attempt (dry-run or live) appends a tab-separated row to
`~/.portfolio-manager/bitfinex-withdrawals.log` (override with `PORTFOLIO_AUDIT_DIR`).
Destination addresses are redacted in both logs and the audit file
(`bc1q***x9` style); the full address is only ever sent to the exchange.

Exit codes follow the BSD `sysexits.h` convention: `64` on bad arguments
(unknown currency, non-positive amount, blank address), `1` on read or
submission failure, `0` on success.

## Tests

```bash
./gradlew test
```

The test suite runs without network access, exchange credentials, or environment variables. It covers:

- Binance and Bitfinex HMAC signing against known payloads
- Gateway request construction and response handling with mocked senders
- Movement-response mapping for both venues
- HTTP sender success and failure paths with mocked clients
- Environment/property lookup behavior
- DTO construction and accessor contracts

## Why this design (for the reader)

- **Why no shared HTTP base?** Each venue has its own quirks — Binance puts the signature in the query string, Bitfinex in a header with a nonce. A shared base ends up as a soup of `if (venue == ...)` branches. One package per venue keeps quirks local.
- **Why `MessageSender` as an interface?** So the signer/payload-builder is testable without spinning up a mock HTTP server. The real `HttpMessageSender` is a thin Apache HttpClient wrapper.
- **What's missing for production?** A position keeper that reconciles balances against an internal ledger, idempotency keys on withdrawals, rate-limit handling per venue, and structured audit logs on every authenticated call. The current shape is a CLI demonstrator, not a production position service.

## Stack

- Kotlin 2.3.21 (JVM target 25), Java 25 toolchain
- Apache HttpClient 5.5
- Jackson 2.20 (BOM-managed)
- Apache Commons Codec / Lang3 / IO
- JUnit Jupiter 6.1, Mockito 5.23, Hamcrest 3
- Gradle 9.5.1

## Project layout

```
src/main/kotlin/
├── binance/        # Binance gateway, signer, message sender, DTOs
└── bitfinex/       # Bitfinex gateway, signer, message sender, DTOs
src/test/kotlin/
├── binance/        # gateway, mapper, sender, property, and signing tests
├── bitfinex/       # gateway, mapper, sender, property, and signing tests
└── PojoTest.kt     # shared DTO contract tests
```

## License

Apache 2.0 — see [LICENSE](LICENSE) and [NOTICE](NOTICE).
