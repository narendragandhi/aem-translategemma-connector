# Status

## Verdict

Buildable, testable reference connector. `mvn clean install` produces an OSGi
bundle plus a content package; both install without changes to AEM. It runs
against Vertex AI when `projectId` and credentials are present, or entirely
offline against a local Ollama `translategemma:latest` model.

## What works

- Registered Granite `TranslationService` (`service.ranking=100`), usable from
  AEM translation projects / cloud services.
- Translation through Vertex AI `GenerativeModel` with retry, circuit breaker,
  in-memory cache, and metrics.
- Sling-job based batch translation with idempotent writes
  (`gemma:translation_<lang>`).
- Optional fallback to any `TranslationProvider` services installed in the
  instance; Ollama is shipped/configured for local offline translation.
- Feedback storage, audit log, analysis endpoint, demo translate endpoint, and a
  small UI overlay.
- Service user + repoinit ACLs, OSGi config, clientlib all packaged.

## What is not production grade

- Job dashboard is in memory (per instance, lost on restart).
- Bundle is ~40 MB because the Vertex AI/gRPC stack is inlined; netty/grpc
  optional imports are declared `resolution:=optional`.
- Sentiment/compliance are prompt heuristics, not enforcement.
- DeepL and OpenAI fallback providers are samples; only Ollama is wired up.
- No translation memory, terminology, MSM delta, DITA, DAM metadata, visual
  context, or i18n dictionary support (removed in 2.0.0).

## Verification

- `mvn test`: 10 tests, 0 failures (Vertex AI model mocked).
- `mvn clean install`: bundle (~40 MB) + content package built.
- Manifest: mandatory imports limited to Granite Translation API, Sling API/jobs,
  servlet API, SLF4J; everything else optional.
- Bundle component XML provides both `com.adobe.granite.translation.api.TranslationService`
  and `com.example.aem.translation.service.TranslateGemmaTranslationService`.
- Deployed to local AEM author: bundle/components active, repoinit paths
  created, audit/metrics/dashboard endpoints return JSON, and
  `POST /bin/translation-gemma/translate` translated via Ollama with and without
  an explicit source language.
