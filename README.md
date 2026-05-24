# Portfolio Manager

[![CI](https://github.com/damian1000/portfolio-manager/actions/workflows/ci.yml/badge.svg)](https://github.com/damian1000/portfolio-manager/actions/workflows/ci.yml)
[![Kotlin](https://img.shields.io/badge/kotlin-2.3.21-blueviolet)](https://kotlinlang.org/)
[![JDK](https://img.shields.io/badge/jdk-25-orange)](https://openjdk.org/projects/jdk/25/)

A small Kotlin tool that talks to **Binance** and **Bitfinex** REST APIs to retrieve wallet balances, transfer/movement history, and (on Bitfinex) submit withdrawals. Built to demonstrate authenticated exchange API access with HMAC request signing.

## What it demonstrates

- Authenticated exchange REST calls — **HMAC-SHA256** (Binance) and **HMAC-SHA384** (Bitfinex)
- Apache HttpClient 5.5 with the modern `HttpClientResponseHandler` API
- Secret handling via env vars only — no creds in code or config
- Kotlin 2.3 on JDK 25 toolchain (JVM target 25)
- Two independent CLI entry points (Binance, Bitfinex) sharing a small set of helpers
- Unit tests for the signing logic with no IO or network

## Prerequisites

- JDK 25 (Gradle toolchain will fetch one if missing)
- An exchange account with API key + secret on whichever venue you want to hit

## Setup

Copy the env template and fill in your credentials:

```bash
cp .env.example .env
# edit .env, then `source .env` (or use a tool like direnv)
```

Required env vars depending on which side you run:

| Variable | Side |
|---|---|
| `BINANCE_API_KEY` | Binance |
| `BINANCE_API_SECRET` | Binance |
| `BITFINEX_API_KEY` | Bitfinex |
| `BITFINEX_API_SECRET` | Bitfinex |

⚠️ **Use read-only API keys** for exploring. Withdrawal endpoints require trade/withdraw permissions — only enable those scopes if you really intend to call `submitWithdrawalRequest`.

## Running

The Gradle `application` plugin entry point is `binance.MainKt`:

```bash
./gradlew run
```

For the Bitfinex side, run the `bitfinex.MainKt` directly (or change the `mainClass` in `build.gradle`):

```bash
./gradlew run -PmainClass=bitfinex.MainKt   # if you wire a -P override
# or simply edit build.gradle: application { mainClass = 'bitfinex.MainKt' }
```

The bitfinex `Main.kt` example does:
1. Print wallet balances
2. Print recent movement history for a currency
3. Print API key settings
4. Optionally submit a withdrawal (uncomment when you really want to)

## Tests

```bash
./gradlew test
```

The unit tests cover HMAC signing for both venues. No network, no env vars needed.

## Stack

- Kotlin 2.3.21 (JVM target 25)
- Java 25 toolchain
- Apache HttpClient 5.5
- Jackson 2.20 (BOM-managed: core, databind, kotlin module)
- OkHttp 5 (for some endpoints)
- Apache Commons Codec / Lang3 / IO
- JUnit Jupiter 6.1, Mockito 5.23, Hamcrest 3
- Gradle 9.5.1

## Project layout

```
src/main/kotlin/
├── binance/        # Binance API client + HMAC-SHA256 signer
└── bitfinex/       # Bitfinex API client + HMAC-SHA384 signer
src/test/kotlin/
├── binance/SigningHelperTest.kt
└── bitfinex/SigningHelperTest.kt
```

## License

Apache 2.0 — see [LICENSE](LICENSE) and [NOTICE](NOTICE).
