# API

All endpoints are Sling servlets registered by path on the AEM instance. They run
under the caller's session, so access control is whatever the repository allows
for that user. JSON in, JSON out (Jackson).

## POST /bin/translategemma/analyze

Runs optional analysis on a resource. Requires the `TranslateGemmaTranslationService`.

Body:

```json
{ "path": "/content/wknd/us/en/magazine", "types": ["sentiment"] }
```

`types` may contain any of `sentiment`, `compliance`, `asset` (asset only applies
to `dam:Asset` resources).

Response (fields present only for requested types):

```json
{
  "sentiment": { "sentiment": "POSITIVE", "confidence": 0.91, "reasoning": "...", "inputTokens": 120, "outputTokens": 8 },
  "compliance": { "compliant": true, "issues": [], "reasoning": "..." },
  "asset": { "altText": "...", "caption": "...", "description": "..." }
}
```

Errors: `400` when `path`/`types` are missing, `404` when the resource does not
exist, `500` with `{"error":"Analysis failed"}` otherwise.

## POST /bin/translation-gemma/translate

Quick translation through the registered `TranslationService`. Requires an
authenticated session.

Body:

```json
{
  "text": "Hello, how are you today?",
  "sourceLang": "en",
  "targetLang": "fr",
  "contentType": "plain",
  "category": "general"
}
```

`sourceLang`, `contentType` and `category` are optional. When `sourceLang` is
omitted it is auto-detected. `text` is limited to 4000 characters.

Response:

```json
{
  "translation": "Bonjour, comment allez-vous aujourd'hui ?",
  "sourceLang": "en",
  "targetLang": "fr"
}
```

Errors: `401` for anonymous callers, `400` for missing/invalid fields, `500`
with `{"error":"Translation failed: ..."}` otherwise. With no Vertex AI project
configured the shipped Ollama fallback serves the request.

## GET /bin/translationgemma/dashboard

Query parameters (all optional):

| Parameter | Meaning |
| --- | --- |
| *(none)* | Aggregate dashboard stats |
| `jobs=true` | All jobs |
| `status=running` | Jobs with that status (`PENDING`, `RUNNING`, `COMPLETED`, `FAILED`, `CANCELLED`, `PAUSED`) |
| `jobId=<id>` | Single job (404 if unknown) |

## POST /bin/translationgemma/dashboard

Query parameters:

| `action` | Parameters | Result |
| --- | --- | --- |
| `create` | `name`, `sourceLanguage`, `targetLanguage`, optional `description`, `dueDate` (ISO-8601 JSON date or epoch millis) | `{"jobId":"...","status":"created"}` |
| `start` | `jobId` | Queues one Sling job per object path, `{"jobId":"...","status":"started"}` |
| `pause` | `jobId` | `{"jobId":"...","status":"paused"}` |
| `cancel` | `jobId` | `{"jobId":"...","status":"cancelled"}` |
| `addObjects` | `jobId`, repeated `objects` | `{"jobId":"...","addedCount":N}` |

Missing `jobId` → `400 {"error":"jobId parameter required"}`. Unknown action →
`400 {"error":"Unknown action","action":"..."}`.

## DELETE /bin/translationgemma/dashboard?jobId=<id>

Removes the job from the in-memory dashboard and returns
`{"jobId":"...","status":"deleted"}`.

## GET /bin/translation-gemma/audit?limit=50

Returns the most recent audit entries (default 50, capped by whatever the audit
service holds). Each entry:

```json
{
  "path": "/content/wknd/us/en/magazine",
  "sourceLanguage": "en",
  "targetLanguage": "de",
  "sentiment": { "...": "..." },
  "compliance": { "...": "..." },
  "timestamp": 1730000000000,
  "userId": "admin"
}
```

`sentiment`/`compliance` are `null` unless those analyses ran.

## GET /bin/translation-gemma/metrics

Aggregates the last 1000 audit entries:

```json
{
  "totalInputTokens": 12345,
  "totalOutputTokens": 6789,
  "totalEvents": 120,
  "languageDistribution": { "de": 60, "fr": 60 }
}
```

## POST /bin/translation-gemma/feedback

Body:

```json
{
  "sourceString": "Hello",
  "originalTranslation": "Hallo",
  "humanCorrection": "Guten Tag",
  "sourceLang": "en",
  "targetLang": "de"
}
```

`sourceString`, `humanCorrection`, `sourceLang`, `targetLang` are required
(`400` otherwise). The feedback is stored under
`/var/translation-gemma/feedback` as the calling user. Success:
`{"status":"recorded"}`.

## Job topic

Batch translation processes consume the Sling job topic
`com/example/aem/translation/gemma/batch`. Job properties: `path`,
`sourceLanguage`, `targetLanguage`, optional `contentType` (`PLAIN` default,
`HTML` supported). On success the consumer writes `gemma:translation_<lang>` and
`gemma:translationKey` (content fingerprint) on the target resource, which makes
re-runs idempotent.
