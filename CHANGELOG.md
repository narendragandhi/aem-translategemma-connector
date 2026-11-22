# Changelog

## 2.0.0 — 2026-11-22

Rewrite for correctness rather than feature count.

### Fixed

- Connector is now an actual OSGi bundle. Previously the build produced only a
  content-package zip, so the connector never ran.
- Registers as `com.adobe.granite.translation.api.TranslationService` (ranking
  100) in addition to the project's own service interface.
- Removed the vendored copy of the Granite Translation API; compiles against
  `aem-sdk-api`.
- Vertex AI client is created lazily and closed in `@Deactivate`; the meter
  registry is closed too.
- Content-type handling accepts only `PLAIN` and `HTML` (the enum values that
  exist in the SDK) and defaults to `PLAIN`.
- Fixed SDK API usage: `getTranslationObjectSourcePath()`,
  `getTranslationObjectInputStream()`, `getMimeType()`, existing
  `TranslationException.ErrorCode` values.
- Content package now contains valid `sling:Folder` / `sling:OsgiConfig` /
  `cq:ClientLibraryFolder` nodes; service-user mapping BSN matches the bundle.
- Repoinit path/ACL alignment: `/var/translation-gemma`,
  `/var/log/translation-gemma`.
- Dashboard and analysis servlets no longer leak exception messages; paths are
  validated; the dashboard is parameter-driven (Sling does not expose
  `pathInfo` for path-bound servlets).
- UI overlay escapes model output (no HTML injection) and no longer stacks
  duplicate polling timers.
- Sentiment/compliance/transparency work is opt-in instead of always-on.
- Guava is pinned to a current version rather than the ancient copy pulled in
  by the SDK uber-jar.
- Micrometer is embedded: `SimpleMeterRegistry` is used at runtime and AEM does
  not export it.
- OSGi configs moved to `/apps/example/translategemma/config` (JCR installer
  ignores paths deeper than four levels) and repoinit is a real
  `sling:OsgiConfig`; service-user mapping uses principal syntax.
- Fallback providers are considered when deciding service availability, so
  translation works without a Vertex AI project; language detection falls back
  too.
- Structured analysis prompts (sentiment/compliance/asset) use the local Ollama
  provider when Vertex AI is not configured.
- Ollama responses are parsed with Jackson, and Jackson annotations are aligned
  with the AEM runtime so `@JsonCreator` models deserialize correctly.
- The author overlay clientlib is included in the Sites console via the
  `cq.sites.collectionpage` category.
- Build uses Java 21.

### Removed

- Fake/placeholder subsystems: translation memory, terminology, MSM delta,
  DITA tag protection, visual context, DAM metadata translation, i18n
  dictionaries, multi-modal translation, prompt templates, provider registry,
  continuous localization, "Sites"/"XF"/"CF integration" packages, mock
  implementations and their tests.
- Misleading documents: `CLOUD_SERVICE_READY.md`, `SPEC_VALIDATION_REPORT.md`,
  `PRODUCTION_READINESS_CHECKLIST.md`, `DESIGN_SPECIFICATION.md`.

### Kept and wired

- Batch Sling jobs (`com/example/aem/translation/gemma/batch`).
- Retry, circuit breaker, cache, metrics.
- Feedback, audit, analysis endpoints, plus a `POST /bin/translation-gemma/translate`
  demo endpoint.
- Fallback providers (DeepL, Ollama, OpenAI); Ollama is configured for local
  `translategemma:latest` use.

Earlier 1.x entries described features that were never functional and have been
dropped together with the code.
