# AEM TranslateGemma Translation Connector

An AEM translation connector that translates content with a Google Gemma model
served by Vertex AI, wiring into AEM's OOTB translation framework
(`com.adobe.granite.translation.api.TranslationService`). With no Vertex AI
project configured it falls back to a local Ollama model
(`translategemma:latest`), so the demo runs entirely offline.

This is a demo/reference implementation, not a product. It is deliberately
small: the whole point is to show how a custom `TranslationService` is
registered, configured, and driven by AEM translation projects/jobs.

## What it actually does

| Area | Status |
| --- | --- |
| Registers as a Granite `TranslationService` | Yes (`service.ranking=100`) |
| Translates strings/content via Vertex AI `GenerativeModel` | Yes |
| Works with AEM translation projects and the translation cloud service | Yes |
| Async batch jobs via Sling Jobs | Yes (`com/example/aem/translation/gemma/batch`) |
| Redis-less in-memory cache, metrics, retry, circuit breaker | Yes |
| Feedback capture endpoint + audit log | Yes |
| Optional fallback to other `TranslationProvider`s | Yes; Ollama is configured for local offline use |
| Sentiment / compliance analysis | Opt-in; heuristic model prompts, not a policy engine |
| Translation memory, DITA/AEM Guides, visual context, MSM deltas, terminology | **Removed** — see CHANGELOG 2.0.0 |

## Requirements

- Java 21 (build and runtime)
- AEM 6.5.17+ / AEM as a Cloud Service with the Granite Translation API
  (`com.adobe.granite.translation.api`)
- Either a Google Cloud project with Vertex AI enabled and access to the model
  in `location` (default `gemma-3-27b-it`, `us-central1`) plus Application
  Default Credentials (or `GOOGLE_APPLICATION_CREDENTIALS`) for the AEM JVM
- Or Ollama running locally with `ollama pull translategemma:latest` (no cloud
  account or credentials needed)

## Build

```bash
mvn clean install
```

Produces two artifacts in `target/`:

- `aem-translategemma-connector-2.0.0-SNAPSHOT.jar` — OSGi bundle
  (`com.example.aem.translation.translategemma`). It embeds the Vertex AI SDK
  and its transitive dependencies (about 40 MB, gRPC included).
- `aem-translategemma-connector-2.0.0-SNAPSHOT.zip` — content package: OSGi
  config, service-user mapping, repoinit (service user + ACLs), clientlib
  overlay.

## Install

1. Install the zip (content package) and the bundle on author **and** publish,
   or place them in an AEM project's `all`/`ui.apps`/`ui.config` modules.
2. For Vertex AI: set `projectId` (and `location` if not `us-central1`) in
   `/system/console/configMgr` → "TranslateGemma Translation Service", and make
   GCP credentials available to the AEM process (ADC via
   `GOOGLE_APPLICATION_CREDENTIALS`, or workload identity on the host).
   For a local-only demo, leave `projectId` empty and start Ollama with
   `translategemma:latest` installed; the shipped
   `com.example.aem.translation.provider.impl.OllamaProvider` config points at
   `http://localhost:11434`.
3. Create a translation project (Sites → References → Language Copies →
   Translate) and pick "Google Gemma (Vertex AI)" as the translation provider.
   If the connector is not shown, enable it in
   `/libs/cq/translation/...` translation cloud service config or grant it via
   the `TranslationService` ranking.

The bundle maps service user `translate-gemma-service` and grants it
`jcr:all` on `/var/translation-gemma` and `/var/log/translation-gemma`, plus
`jcr:read` on `/content` and `/content/dam`.

## Configuration

All configuration is OSGi (`TranslateGemmaConfig`). Property names are the
attribute names below; the shipped config lives at
`/apps/example/translategemma/config`.

| Property | Default | Notes |
| --- | --- | --- |
| `projectId` | *(empty)* | GCP project; empty means ADC default project |
| `location` | `us-central1` | Vertex AI region |
| `enabled` | `true` | Master switch for the connector |
| `modelName` | `gemma-3-27b-it` | Model/deployment name |
| `defaultContentCategory` | `general` | Category reported in `TranslationServiceInfo` |
| `enableFallbackProviders` | `true` | Use `TranslationProvider`s if Gemma fails |
| `enableSentimentAnalysis` | `false` | Adds a sentiment/quality heuristic to prompts |
| `enableComplianceCheck` | `false` | Adds a compliance heuristic to prompts |
| `retryMaxAttempts`, `retryWaitDurationMs` | `3`, `1000` | Resilience4j retry |
| `circuitBreakerFailureRateThreshold`, `circuitBreakerSlowCallRateThreshold`, `circuitBreakerSlowCallDurationMs`, `circuitBreakerWaitDurationS` | `50`, `100`, `5000`, `30` | Resilience4j circuit breaker |
| `enableCaching`, `cacheMaxSize`, `cacheExpireAfterMinutes` | `true`, `1000`, `60` | Caffeine cache |
| `enableMetrics` | `true` | Registers a local `MeterRegistry` |

For the local demo, the shipped OSGi config sets both analysis flags to `true`.

## Endpoints

All endpoints are on the author instance by default and use the current
session; secure them before exposing publicly.

| Method | Path | Purpose |
| --- | --- | --- |
| POST | `/bin/translation-gemma/translate` | Quick translation: body `{"text": "Hello", "sourceLang": "en", "targetLang": "fr"}` (`sourceLang` optional, auto-detected) |
| POST | `/bin/translategemma/analyze` | Analyze a resource; body `{"path": "/content/...", "types": ["sentiment"]}` |
| GET | `/bin/translationgemma/dashboard` | Stats, all jobs (`?jobs=true`), `?jobId=...`, or `?status=...` |
| POST | `/bin/translationgemma/dashboard` | Job actions: `action=create/start/pause/cancel/addObjects` |
| GET | `/bin/translation-gemma/audit` | Recent audit entries (`?limit=N`) |
| GET | `/bin/translation-gemma/metrics` | Aggregate translation metrics |
| POST | `/bin/translation-gemma/feedback` | Record human feedback |

See `API.md` for request/response bodies.

## Development

```bash
mvn test     # unit tests (10)
mvn clean install
```

The tests stub out the Vertex AI `GenerativeModel` via the package-private
`setModel(...)` seam; no GCP credentials are needed to build or test.

## Known limitations

- The job dashboard is in-memory and per-instance; use Sling Jobs for
  production work (the consumer already persists to
  `gemma:translation_<lang>` on the target node).
- The bundle embeds the full Vertex AI/gRPC stack (~40 MB) and marks
  netty/grpc optional imports as `resolution:=optional`. Prefer a REST-based
  client if bundle size matters.
- Sentiment/compliance are prompt-level heuristics, not enforced policy.
- Fallback providers implement the project's own `TranslationProvider`
  interface. Ollama is configured locally; DeepL and OpenAI are samples only.

## License

Apache License 2.0.
