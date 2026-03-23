package com.example.aem.translation.impl;

import com.example.aem.translation.service.DitaTagProtectionService;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component(service = DitaTagProtectionService.class)
public class DitaTagProtectionServiceImpl implements DitaTagProtectionService {

    private static final Logger LOG = LoggerFactory.getLogger(DitaTagProtectionServiceImpl.class);
    
    // Pattern to catch DITA tags and their non-translatable attributes (id, href, conref, keyref, class)
    private static final Pattern TAG_PATTERN = Pattern.compile("<[^>]+>");
    private static final String PLACEHOLDER_PREFIX = "[[_DITA_";
    private static final String PLACEHOLDER_SUFFIX = "_]]";

    @Override
    public ProtectedContent protect(String rawXml) {
        if (rawXml == null || !rawXml.contains("<")) {
            return new ProtectedContent(rawXml, new HashMap<>());
        }

        Map<String, String> placeholders = new HashMap<>();
        StringBuilder masked = new StringBuilder();
        Matcher matcher = TAG_PATTERN.matcher(rawXml);
        
        int lastEnd = 0;
        int count = 0;
        
        while (matcher.find()) {
            masked.append(rawXml, lastEnd, matcher.start());
            
            String tag = matcher.group();
            String key = PLACEHOLDER_PREFIX + (count++) + PLACEHOLDER_SUFFIX;
            
            placeholders.put(key, tag);
            masked.append(key);
            
            lastEnd = matcher.end();
        }
        masked.append(rawXml.substring(lastEnd));

        LOG.debug("Masked {} DITA tags in content", count);
        return new ProtectedContent(masked.toString(), placeholders);
    }

    @Override
    public String restore(String translatedXml, Map<String, String> placeholders) {
        if (placeholders == null || placeholders.isEmpty()) {
            return translatedXml;
        }

        String result = translatedXml;
        // In reverse order to avoid substring collisions if placeholders are similar
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        
        return result;
    }
}
