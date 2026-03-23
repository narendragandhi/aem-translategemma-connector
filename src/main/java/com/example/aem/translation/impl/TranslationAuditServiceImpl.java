package com.example.aem.translation.impl;

import com.example.aem.translation.service.TranslationAuditService;
import com.example.aem.translation.model.ComplianceResult;
import com.example.aem.translation.model.SentimentResult;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@Component(service = TranslationAuditService.class)
public class TranslationAuditServiceImpl implements TranslationAuditService {

    private static final Logger LOG = LoggerFactory.getLogger(TranslationAuditServiceImpl.class);
    private static final String AUDIT_ROOT = "/var/log/translation-gemma/audit";

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Override
    public void logEvent(String path, String sourceLanguage, String targetLanguage,
                         SentimentResult sentiment, ComplianceResult compliance, String userId) {
        
        Map<String, Object> authInfo = new HashMap<>();
        authInfo.put(ResourceResolverFactory.SUBSERVICE, "translate-gemma-service");

        try (ResourceResolver resolver = resolverFactory.getServiceResourceResolver(authInfo)) {
            String entryName = "event-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
            
            Map<String, Object> props = new HashMap<>();
            props.put("jcr:primaryType", "nt:unstructured");
            props.put("path", path);
            props.put("sourceLanguage", sourceLanguage);
            props.put("targetLanguage", targetLanguage);
            props.put("timestamp", Calendar.getInstance());
            props.put("userId", userId != null ? userId : "system");

            if (sentiment != null) {
                props.put("sentiment", sentiment.getSentiment());
                props.put("sentimentConfidence", sentiment.getConfidence());
                props.put("sentimentReasoning", sentiment.getReasoning());
                props.put("inputTokens", sentiment.getInputTokens());
                props.put("outputTokens", sentiment.getOutputTokens());
            }

            if (compliance != null) {
                props.put("compliant", compliance.isCompliant());
                props.put("complianceConfidence", compliance.getConfidence());
                props.put("complianceReasoning", compliance.getReasoning());
                // We add to existing tokens if sentiment was logged, or set new ones
                int in = (Integer) props.getOrDefault("inputTokens", 0) + compliance.getInputTokens();
                int out = (Integer) props.getOrDefault("outputTokens", 0) + compliance.getOutputTokens();
                props.put("inputTokens", in);
                props.put("outputTokens", out);
            }

            ensureRootExists(resolver);
            resolver.create(resolver.getResource(AUDIT_ROOT), entryName, props);
            resolver.commit();
            
            LOG.debug("Logged translation audit event for {}", path);

        } catch (Exception e) {
            LOG.error("Failed to log translation audit event", e);
        }
    }

    @Override
    public List<AuditEntry> getRecentEntries(int limit) {
        List<AuditEntry> entries = new ArrayList<>();
        Map<String, Object> authInfo = new HashMap<>();
        authInfo.put(ResourceResolverFactory.SUBSERVICE, "translate-gemma-service");

        try (ResourceResolver resolver = resolverFactory.getServiceResourceResolver(authInfo)) {
            Resource root = resolver.getResource(AUDIT_ROOT);
            if (root != null) {
                Iterator<Resource> children = root.listChildren();
                List<Resource> childList = new ArrayList<>();
                while (children.hasNext()) {
                    childList.add(children.next());
                }
                
                childList.sort((r1, r2) -> {
                    Calendar c1 = r1.getValueMap().get("timestamp", Calendar.class);
                    Calendar c2 = r2.getValueMap().get("timestamp", Calendar.class);
                    if (c1 == null || c2 == null) return 0;
                    return c2.compareTo(c1);
                });

                for (int i = 0; i < Math.min(limit, childList.size()); i++) {
                    Resource res = childList.get(i);
                    ValueMap vm = res.getValueMap();
                    
                    SentimentResult sr = new SentimentResult(
                        vm.get("sentiment", "UNKNOWN"),
                        vm.get("sentimentConfidence", 0.0),
                        vm.get("sentimentReasoning", ""),
                        vm.get("inputTokens", 0),
                        vm.get("outputTokens", 0)
                    );
                    
                    ComplianceResult cr = new ComplianceResult(
                        vm.get("compliant", true),
                        "", 
                        vm.get("complianceConfidence", 0.0),
                        vm.get("complianceReasoning", ""),
                        0, 0 // Tokens already tracked in SR for this audit entry
                    );

                    entries.add(new AuditEntry(
                        vm.get("path", ""),
                        vm.get("sourceLanguage", ""),
                        vm.get("targetLanguage", ""),
                        sr, cr,
                        vm.get("timestamp", Calendar.class).getTimeInMillis(),
                        vm.get("userId", "system")
                    ));
                }
            }
        } catch (Exception e) {
            LOG.error("Failed to retrieve audit entries", e);
        }
        return entries;
    }

    private void ensureRootExists(ResourceResolver resolver) throws Exception {
        Resource var = resolver.getResource("/var");
        if (var == null) return; 
        
        Resource log = resolver.getResource("/var/log");
        if (log == null) resolver.create(var, "log", Collections.singletonMap("jcr:primaryType", "nt:unstructured"));
        
        Resource trans = resolver.getResource("/var/log/translation-gemma");
        if (trans == null) resolver.create(resolver.getResource("/var/log"), "translation-gemma", Collections.singletonMap("jcr:primaryType", "nt:unstructured"));
        
        Resource audit = resolver.getResource(AUDIT_ROOT);
        if (audit == null) resolver.create(resolver.getResource("/var/log/translation-gemma"), "audit", Collections.singletonMap("jcr:primaryType", "nt:unstructured"));
    }
}
