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
            String entryPath = AUDIT_ROOT + "/" + entryName;

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
            }

            if (compliance != null) {
                props.put("compliant", compliance.isCompliant());
                props.put("complianceConfidence", compliance.getConfidence());
                props.put("complianceReasoning", compliance.getReasoning());
            }

            // In a real AEM environment, we would use ResourceUtil.getOrCreateResource
            // but for this sandbox, we'll assume the helper exists or use a simple create.
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
                
                // Sort by timestamp descending
                childList.sort((r1, r2) -> {
                    Long t1 = r1.getValueMap().get("timestamp", Calendar.class).getTimeInMillis();
                    Long t2 = r2.getValueMap().get("timestamp", Calendar.class).getTimeInMillis();
                    return t2.compareTo(t1);
                });

                for (int i = 0; i < Math.min(limit, childList.size()); i++) {
                    Resource res = childList.get(i);
                    ValueMap vm = res.getValueMap();
                    
                    SentimentResult sr = new SentimentResult(
                        vm.get("sentiment", "UNKNOWN"),
                        vm.get("sentimentConfidence", 0.0),
                        vm.get("sentimentReasoning", "")
                    );
                    
                    ComplianceResult cr = new ComplianceResult(
                        vm.get("compliant", true),
                        "", // feedback
                        vm.get("complianceConfidence", 0.0),
                        vm.get("complianceReasoning", "")
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
        // Mocking root creation for sandbox
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
