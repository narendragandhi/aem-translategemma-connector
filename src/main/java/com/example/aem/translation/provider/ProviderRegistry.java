package com.example.aem.translation.provider;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component(service = ProviderRegistry.class)
public class ProviderRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(ProviderRegistry.class);

    private final Map<TranslationProvider.ProviderType, TranslationProvider> providers = new ConcurrentHashMap<>();
    private final List<TranslationProvider> providerList = new ArrayList<>();

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addProvider(TranslationProvider provider) {
        providers.put(provider.getProviderType(), provider);
        synchronized (providerList) {
            providerList.add(provider);
            providerList.sort((a, b) -> Double.compare(b.getMatchingScore(), a.getMatchingScore()));
        }
        LOG.info("Registered translation provider: {} ({})", provider.getProviderName(), provider.getProviderType());
    }

    public void removeProvider(TranslationProvider provider) {
        providers.remove(provider.getProviderType());
        synchronized (providerList) {
            providerList.remove(provider);
        }
        LOG.info("Unregistered translation provider: {}", provider.getProviderType());
    }

    public TranslationProvider getProvider(TranslationProvider.ProviderType type) {
        return providers.get(type);
    }

    public TranslationProvider getPrimaryProvider() {
        synchronized (providerList) {
            if (!providerList.isEmpty()) {
                return providerList.get(0);
            }
        }
        return providers.get(TranslationProvider.ProviderType.TRANSLATEGEMMA);
    }

    public TranslationProvider getAvailableProvider(TranslationProvider.ProviderType preferred) {
        TranslationProvider provider = providers.get(preferred);
        if (provider != null && provider.isAvailable()) {
            return provider;
        }
        return getPrimaryProvider();
    }

    public List<TranslationProvider> getAvailableProviders() {
        return providerList.stream()
                .filter(TranslationProvider::isAvailable)
                .collect(Collectors.toList());
    }

    public TranslationProvider findProviderForLanguagePair(String sourceLanguage, String targetLanguage) {
        synchronized (providerList) {
            for (TranslationProvider provider : providerList) {
                if (provider.isAvailable() && 
                    provider.supportsLanguagePair(sourceLanguage, targetLanguage)) {
                    return provider;
                }
            }
        }
        return null;
    }

    public boolean isAnyProviderAvailable() {
        return providerList.stream().anyMatch(TranslationProvider::isAvailable);
    }

    public Map<String, Object> getHealthStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        
        synchronized (providerList) {
            for (TranslationProvider provider : providerList) {
                Map<String, Object> providerStatus = new LinkedHashMap<>();
                providerStatus.put("available", provider.isAvailable());
                providerStatus.put("score", provider.getMatchingScore());
                providerStatus.put("avgResponseTime", provider.getAverageResponseTime() + "ms");
                status.put(provider.getProviderType().name(), providerStatus);
            }
        }
        
        return status;
    }
}
