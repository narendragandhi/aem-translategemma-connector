# Local demo script (5 minutes)

No-cloud demo: local AEM author + local Ollama (`translategemma:latest`). Vertex AI
is not required and is not configured.

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

## 2. Show the transparency layer

```bash
curl -s -u admin:admin 'http://localhost:4502/bin/translation-gemma/audit?limit=5' | python3 -m json.tool
curl -s -u admin:admin 'http://localhost:4502/bin/translationgemma/dashboard' | python3 -m json.tool
```

Point out that every audit entry carries `path`, source/target language, sentiment,
confidence, reasoning, tokens, timestamp and user.

## 3. Show it in the author UI

Open:

```text
http://localhost:4502/sites.html/content/wknd
```

Look for the colored `Gemma: ...` badges next to:

- `/content/wknd/us` — `POSITIVE (94%)`, green
- `/content/wknd/language-masters` — `NEUTRAL (76%)`, amber
- `/content/wknd/ca` — `NEGATIVE (63%)`, red

Hover a badge to read its reasoning tooltip. The overlay reads the same audit API
shown in step 2 and refreshes every 30 seconds.

## 4. Seed the demo badges (only if missing)

The badges come from seeded audit nodes under `/var/log/translation-gemma/audit`.
Re-run this if the `audit` endpoint returns `[]`.

```bash
while IFS='|' read -r name path sentiment confidence reasoning; do
  curl -s -u admin:admin -X POST \
    -F 'jcr:primaryType=nt:unstructured' \
    -F "path=$path" \
    -F 'sourceLanguage=en' \
    -F 'targetLanguage=fr' \
    -F "timestamp=$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
    -F 'timestamp@TypeHint=Date' \
    -F 'userId=demo-user' \
    -F "sentiment=$sentiment" \
    -F "sentimentConfidence=$confidence" \
    -F 'sentimentConfidence@TypeHint=Double' \
    -F "sentimentReasoning=$reasoning" \
    "http://localhost:4502/var/log/translation-gemma/audit/$name" > /dev/null
done <<'EOF'
event-demo-1|/content/wknd/us|POSITIVE|0.94|Tone is welcoming and brand-aligned.
event-demo-2|/content/wknd/language-masters|NEUTRAL|0.76|Factual product copy with consistent terminology.
event-demo-3|/content/wknd/ca|NEGATIVE|0.63|Escalation language detected; reviewer attention recommended.
EOF
```

Reload `/sites.html/content/wknd`.

## 5. Reset

```bash
for id in event-demo-1 event-demo-2 event-demo-3; do
  curl -s -u admin:admin -X DELETE \
    "http://localhost:4502/var/log/translation-gemma/audit/$id" > /dev/null
done
```

## Talk track

> One connector, two proof points. First, a real server-side translation path:
> AEM calls the installed model through the local Ollama provider, with automatic
> source-language detection. Second, the same run is visible in the author UI:
> the Sites console shows sentiment and confidence badges straight from the audit
> API, so translators and reviewers can see why a translation was flagged.

## Scope notes

- Translation is live. Sentiment/compliance analysis is disabled in the shipped
  config (`enableSentimentAnalysis=false`, `enableComplianceCheck=false`), so the
  badges use seeded audit entries rather than a live analysis call.
- The `translategemma:latest` model is used for translation. GCP/Vertex AI is not
  needed for this demo.
