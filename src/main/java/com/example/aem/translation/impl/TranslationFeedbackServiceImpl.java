package com.example.aem.translation.impl;

import com.example.aem.translation.service.TranslationFeedbackService;
import com.example.aem.translation.model.TranslationFeedback;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@Component(service = TranslationFeedbackService.class)
public class TranslationFeedbackServiceImpl implements TranslationFeedbackService {

    private static final Logger LOG = LoggerFactory.getLogger(TranslationFeedbackServiceImpl.class);
    private static final String FEEDBACK_ROOT = "/var/translation-gemma/feedback";

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Override
    public void recordFeedback(String sourceString, String originalTranslation, String humanCorrection, 
                                String sourceLang, String targetLang, String userId) {
        
        Map<String, Object> authInfo = new HashMap<>();
        authInfo.put(ResourceResolverFactory.SUBSERVICE, "translate-gemma-service");

        try (ResourceResolver resolver = resolverFactory.getServiceResourceResolver(authInfo)) {
            ensureRootExists(resolver);
            
            String entryName = "fb-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
            
            Map<String, Object> props = new HashMap<>();
            props.put("jcr:primaryType", "nt:unstructured");
            props.put("sourceString", sourceString);
            props.put("originalTranslation", originalTranslation);
            props.put("humanCorrection", humanCorrection);
            props.put("sourceLanguage", sourceLang);
            props.put("targetLanguage", targetLang);
            props.put("userId", userId != null ? userId : "system");
            props.put("timestamp", Calendar.getInstance());

            resolver.create(resolver.getResource(FEEDBACK_ROOT), entryName, props);
            resolver.commit();
            
            LOG.info("Recorded human feedback for {} in job {}", sourceLang + " -> " + targetLang, entryName);
        } catch (Exception e) {
            LOG.error("Failed to record human feedback", e);
        }
    }

    @Override
    public List<TranslationFeedback> getRelevantFeedback(String sourceString, String sourceLang, String targetLang, int limit) {
        List<TranslationFeedback> feedbackList = new ArrayList<>();
        Map<String, Object> authInfo = new HashMap<>();
        authInfo.put(ResourceResolverFactory.SUBSERVICE, "translate-gemma-service");

        try (ResourceResolver resolver = resolverFactory.getServiceResourceResolver(authInfo)) {
            Resource root = resolver.getResource(FEEDBACK_ROOT);
            if (root != null) {
                Iterator<Resource> children = root.listChildren();
                List<Resource> childList = new ArrayList<>();
                while (children.hasNext()) {
                    Resource r = children.next();
                    ValueMap vm = r.getValueMap();
                    // Basic filter by language pair for relevancy
                    if (sourceLang.equalsIgnoreCase(vm.get("sourceLanguage", "")) && 
                        targetLang.equalsIgnoreCase(vm.get("targetLanguage", ""))) {
                        childList.add(r);
                    }
                }
                
                // Sort by timestamp descending
                childList.sort((r1, r2) -> {
                    Long t1 = r1.getValueMap().get("timestamp", Calendar.class).getTimeInMillis();
                    Long t2 = r2.getValueMap().get("timestamp", Calendar.class).getTimeInMillis();
                    return t2.compareTo(t1);
                });

                for (int i = 0; i < Math.min(limit, childList.size()); i++) {
                    Resource res = childList.get(i);
                    ValueMap vm = res.getValueMap();
                    feedbackList.add(new TranslationFeedback(
                        vm.get("sourceString", ""),
                        vm.get("originalTranslation", ""),
                        vm.get("humanCorrection", ""),
                        vm.get("sourceLanguage", ""),
                        vm.get("targetLanguage", ""),
                        vm.get("userId", "system"),
                        vm.get("timestamp", Calendar.class).getTimeInMillis()
                    ));
                }
            }
        } catch (Exception e) {
            LOG.error("Failed to retrieve relevant feedback", e);
        }
        return feedbackList;
    }

    private void ensureRootExists(ResourceResolver resolver) throws Exception {
        Resource var = resolver.getResource("/var");
        if (var == null) return;
        
        Resource trans = resolver.getResource("/var/translation-gemma");
        if (trans == null) resolver.create(var, "translation-gemma", Collections.singletonMap("jcr:primaryType", "nt:unstructured"));
        
        Resource feedback = resolver.getResource(FEEDBACK_ROOT);
        if (feedback == null) resolver.create(resolver.getResource("/var/translation-gemma"), "feedback", Collections.singletonMap("jcr:primaryType", "nt:unstructured"));
    }
}
