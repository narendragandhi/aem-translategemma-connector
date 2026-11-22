# Local demo script (5 minutes)

No-cloud demo: local AEM author + local Ollama (`translategemma:latest`). Vertex AI
is not required and is not configured. Translation, sentiment and compliance all
run through the local model.

## Before you start

```bash
# AEM author must be running
open http://localhost:4502

# Ollama must be running with the model pulled
ollama serve &
ollama pull translategemma:latest
curl -s http://localhost:11434/api/tags | python3 -m json.tool
```

AEM: `http://localhost:4502`, user `admin`, password `admin`.
All commands below assume that user/pass.

## 1. Prove the model is reachable

```bash
curl -s -u admin:admin \
  -X POST http://localhost:4502/bin/translation-gemma/translate \
  -H 'Content-Type: application/json' \
  -d '{"text":"Hello, how are you today?","sourceLang":"en","targetLang":"fr"}'
```

Expected (wording may vary slightly):

```json
{"targetLang":"fr","sourceLang":"en","translation":"Bonjour, comment allez-vous aujourd'hui ?"}
```

Show auto-detection too:

```bash
curl -s -u admin:admin \
  -X POST http://localhost:4502/bin/translation-gemma/translate \
  -H 'Content-Type: application/json' \
  -d '{"text":"Bonjour, comment allez-vous ?","targetLang":"en"}'
```

## 2. Generate live insights for real pages

This calls the same local model for sentiment and compliance, then writes one
audit entry keyed to the page path.

```bash
for page in /content/wknd/us /content/wknd/language-masters /content/wknd/ca; do
  curl -s -u admin:admin \
    -X POST http://localhost:4502/bin/translategemma/analyze \
    -H 'Content-Type: application/json' \
    -d "{\"path\":\"$page\",\"types\":[\"sentiment\",\"compliance\"]}" | python3 -m json.tool
done
```

## 3. Show the transparency layer

```bash
curl -s -u admin:admin 'http://localhost:4502/bin/translation-gemma/audit?limit=10' | python3 -m json.tool
curl -s -u admin:admin 'http://localhost:4502/bin/translationgemma/dashboard' | python3 -m json.tool
```

Point out the real-path entries: `path`, source/target language, sentiment,
confidence, reasoning, tokens, timestamp and user.

## 4. Show it in the author UI

Open:

```text
http://localhost:4502/sites.html/content/wknd
```

Look for colored `Gemma: ...` badges next to the pages analyzed in step 2.
Confidence and wording come from the live model call, so they vary per run.
Hover a badge to read its reasoning tooltip. The overlay reads the same audit API
shown in step 3 and refreshes every 30 seconds.

## 5. Clear the audit log (optional)

```bash
curl -s -u admin:admin \
  'http://localhost:4502/bin/querybuilder.json?path=/var/log/translation-gemma/audit&type=nt:unstructured&p.limit=-1' \
  | python3 -c 'import json,sys; print("\n".join(h["path"] for h in json.load(sys.stdin)["hits"]))' \
  | while read -r p; do curl -s -u admin:admin -X DELETE "http://localhost:4502$p" > /dev/null; done
```

Reload `/sites.html/content/wknd` after clearing.

## Talk track

> One connector, two proof points. First, a real server-side translation path:
> AEM calls the installed model through the local Ollama provider, with automatic
> source-language detection. Second, the same setup runs live sentiment and
> compliance analysis; those results land in the audit API and the Sites console
> shows confidence badges straight from that data, so reviewers can see why a
> translation was flagged.

## Scope notes

- Translation, sentiment and compliance are live through `translategemma:latest`.
  GCP/Vertex AI is not needed for this demo.
- The audit log also contains `N/A` entries from the service's internal analysis
  calls; the page-keyed entries are the ones the overlay renders.
- `inputTokens` / `outputTokens` are not reported by the Ollama HTTP response, so
  those audit fields are `0` in local mode.
