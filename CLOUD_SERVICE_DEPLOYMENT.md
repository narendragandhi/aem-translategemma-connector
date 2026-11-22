# Deployment

## Artifacts

`mvn clean install` produces:

- `target/aem-translategemma-connector-2.0.0-SNAPSHOT.jar` — OSGi bundle
- `target/aem-translategemma-connector-2.0.0-SNAPSHOT.zip` — content package

Both are needed: the bundle contains the code, the zip contains the OSGi config,
service-user mapping, repoinit and clientlib.

## Local author

1. Install the zip with the Package Manager
   (`http://localhost:4502/crx/packmgr`), then install the bundle via the
   Bundle console (`/system/console/bundles`) or the same Package Manager
   (it accepts bundles too).
2. Set `projectId`/`location` in
   `/system/console/configMgr` → "TranslateGemma Translation Service".
3. Provide credentials to the JVM
   (`GOOGLE_APPLICATION_CREDENTIALS=/path/to/key.json`).
4. Check the bundle is `Active` and the component is satisfied:
   `/system/console/components/com.example.aem.translation.impl.TranslateGemmaTranslationServiceImpl`.

## AEM as a Cloud Service

Two options:

- **Add to a project**: drop the sources into an existing AEM project as a new
  module pair (`ui.apps` for the bundle, `ui.config`/`all` for the content), or
  convert this repo into the usual `core`/`ui.apps`/`ui.config` layout.
- **Package-based**: install the zip with the Package Manager on the stage/prod
  instances and the bundle alongside it. Note that direct package install on
  Cloud Service is restricted to Cloud Manager pipelines for production
  environments.

Set `projectId` in Cloud Manager via an OSGi config with `${env:GCP_PROJECT_ID}`
placeholders if you want environment variables (see `env.example`). Provide GCP
credentials through the platform, not through config files in the repo.

## Service user

Repoinit creates `translate-gemma-service` and the service-user mapping
`com.example.aem.translation.translategemma:translate-gemma-service=translate-gemma-service`.

ACLs: `jcr:all` on `/var/translation-gemma` and `/var/log/translation-gemma`,
`jcr:read` on `/content` and `/content/dam`. If your translation workers write
elsewhere, extend the repoinit ACLs accordingly.

## Verify

```bash
curl -u admin:admin http://localhost:4502/bin/translation-gemma/audit?limit=5
curl -u admin:admin http://localhost:4502/bin/translation-gemma/metrics
```

Then create a translation project in AEM and select "Google Gemma (Vertex AI)".
