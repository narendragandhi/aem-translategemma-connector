package com.example.aem.translation.impl;

import com.example.aem.translation.util.HashUtils;
import com.example.aem.translation.service.TranslateGemmaTranslationService;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;

/**
 * Enterprise-grade background processor for Gemma 4 translation tasks.
 * Implements Idempotent Writes to ensure system resilience and token cost optimization.
 */
@Component(
    service = JobConsumer.class,
    property = {
        JobConsumer.PROPERTY_TOPICS + "=" + TranslationJobConsumer.JOB_TOPIC
    }
)
public class TranslationJobConsumer implements JobConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(TranslationJobConsumer.class);
    public static final String JOB_TOPIC = "com/example/aem/translation/gemma/batch";
    private static final String PROPERTY_IDEMPOTENCY_KEY = "gemma:translationKey";
    private static final String PROPERTY_TRANSLATED_PREFIX = "gemma:translation_";

    @Reference
    private TranslateGemmaTranslationService translationService;

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Override
    public JobResult process(Job job) {
        String jobId = job.getProperty("jobId", String.class);
        String objectId = job.getProperty("objectId", String.class); // JCR Path
        String sourceLanguage = job.getProperty("sourceLanguage", String.class);
        String targetLanguage = job.getProperty("targetLanguage", String.class);
        String content = job.getProperty("content", String.class);
        String contentType = job.getProperty("contentType", "PLAIN");

        String modelName = translationService.getTranslationServiceInfo().getTranslationServiceName();

        LOG.info("Processing durable translation job: {} for object: {}", jobId, objectId);

        try (ResourceResolver resolver = getServiceResolver()) {
            Resource targetResource = resolver.getResource(objectId);
            if (targetResource == null) {
                LOG.error("Target resource not found: {}. Abandoning job.", objectId);
                return JobResult.CANCEL;
            }

            // 1. Check Idempotency Key (Fingerprint)
            String currentFingerprint = HashUtils.generateIdempotencyKey(content, targetLanguage, modelName);
            String existingFingerprint = targetResource.getValueMap().get(PROPERTY_IDEMPOTENCY_KEY, String.class);

            if (currentFingerprint.equals(existingFingerprint)) {
                LOG.info("Idempotent hit for {}. Content and Model unchanged. Skipping LLM call.", objectId);
                return JobResult.OK;
            }

            // 2. Perform the actual (expensive) translation
            String sourceContent = content;
            if (sourceContent == null || sourceContent.isEmpty()) {
                sourceContent = extractContent(targetResource);
            }
            if (sourceContent == null || sourceContent.isEmpty()) {
                LOG.error("Job has no translatable content for {}. Abandoning job.", objectId);
                return JobResult.CANCEL;
            }

            com.adobe.granite.translation.api.TranslationConstants.ContentType parsedContentType;
            try {
                parsedContentType = com.adobe.granite.translation.api.TranslationConstants.ContentType
                        .valueOf(contentType.toUpperCase());
            } catch (IllegalArgumentException e) {
                parsedContentType = com.adobe.granite.translation.api.TranslationConstants.ContentType.PLAIN;
            }

            com.adobe.granite.translation.api.TranslationResult result = translationService.translateString(
                sourceContent,
                sourceLanguage,
                targetLanguage,
                parsedContentType,
                "general"
            );

            // 3. Persist the translated content next to the source and the fingerprint
            ModifiableValueMap mvm = targetResource.adaptTo(ModifiableValueMap.class);
            if (mvm != null) {
                mvm.put(PROPERTY_TRANSLATED_PREFIX + targetLanguage, result.getTranslation());
                mvm.put(PROPERTY_IDEMPOTENCY_KEY, currentFingerprint);
                resolver.commit();
            }

            return JobResult.OK;

        } catch (Exception e) {
            LOG.error("Failed to process idempotent translation job for {}. Error: {}", objectId, e.getMessage());
            return JobResult.FAILED; // Triggers automatic retry by Sling
        }
    }

    private String extractContent(Resource resource) {
        Resource contentResource = resource.getChild("jcr:content");
        org.apache.sling.api.resource.ValueMap vm =
                (contentResource != null ? contentResource : resource).getValueMap();
        for (String key : new String[]{"jcr:description", "dc:description", "jcr:title", "dc:title"}) {
            String value = vm.get(key, String.class);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private ResourceResolver getServiceResolver() throws Exception {
        Map<String, Object> authInfo = Collections.singletonMap(
            ResourceResolverFactory.SUBSERVICE, "translate-gemma-service"
        );
        return resolverFactory.getServiceResourceResolver(authInfo);
    }
}
