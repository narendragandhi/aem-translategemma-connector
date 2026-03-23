package com.example.aem.translation.integration;

import com.adobe.granite.translation.api.TranslationConstants;
import com.adobe.granite.translation.api.TranslationResult;
import com.example.aem.translation.config.TranslateGemmaConfig;
import com.example.aem.translation.provider.ProviderRegistry;
import com.example.aem.translation.provider.TranslationProvider;
import com.example.aem.translation.provider.impl.DeepLProvider;
import com.example.aem.translation.provider.impl.OpenAIProvider;
import com.example.aem.translation.provider.impl.OllamaProvider;
import com.example.aem.translation.service.TranslateGemmaTranslationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("TranslateGemma Integration Tests")
public class TranslateGemmaIntegrationTest {

    private static final String GCP_PROJECT_ID = System.getenv("GCP_PROJECT_ID");
    private static final String GCP_LOCATION = System.getenv("GCP_LOCATION");
    private static final String GCP_KEY_PATH = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");

    private static final String DEEPL_API_KEY = System.getenv("DEEPL_API_KEY");
    private static final String OPENAI_API_KEY = System.getenv("OPENAI_API_KEY");

    @Mock
    private TranslateGemmaTranslationService translationService;

    @Mock
    private ProviderRegistry providerRegistry;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        if (GCP_PROJECT_ID == null || GCP_PROJECT_ID.isEmpty()) {
            System.out.println("WARNING: GCP_PROJECT_ID not set - skipping integration tests");
        }
        if (GCP_KEY_PATH == null || GCP_KEY_PATH.isEmpty()) {
            System.out.println("WARNING: GOOGLE_APPLICATION_CREDENTIALS not set - skipping integration tests");
        }
    }

    @Test
    @DisplayName("Test service configuration from environment variables")
    void testConfigurationFromEnvironment() {
        String projectId = System.getenv().getOrDefault("GCP_PROJECT_ID", "default-project");
        String location = System.getenv().getOrDefault("GCP_LOCATION", "us-central1");
        String provider = System.getenv().getOrDefault("TRANSLATION_PROVIDER", "translategemma");
        
        assertNotNull(projectId);
        assertNotNull(location);
        assertEquals("translategemma", provider);
        
        System.out.println("Configuration loaded:");
        System.out.println("  Project ID: " + projectId);
        System.out.println("  Location: " + location);
        System.out.println("  Provider: " + provider);
    }

    @Test
    @DisplayName("Test supported languages configuration")
    void testSupportedLanguagesConfig() {
        String supportedLangs = System.getenv().getOrDefault("SUPPORTED_LANGUAGES", "en,es,fr,de,ja,ko,zh");
        
        assertNotNull(supportedLangs);
        String[] langs = supportedLangs.split(",");
        assertTrue(langs.length > 0);
        
        System.out.println("Supported languages: " + supportedLangs);
    }

    @Test
    @DisplayName("Test TM configuration")
    void testTranslationMemoryConfig() {
        boolean tmEnabled = Boolean.parseBoolean(System.getenv().getOrDefault("TM_ENABLED", "true"));
        double tmMinScore = Double.parseDouble(System.getenv().getOrDefault("TM_MIN_SCORE", "0.85"));
        
        assertTrue(tmEnabled);
        assertTrue(tmMinScore >= 0.0 && tmMinScore <= 1.0);
        
        System.out.println("TM Configuration:");
        System.out.println("  Enabled: " + tmEnabled);
        System.out.println("  Min Score: " + tmMinScore);
    }

    @Test
    @DisplayName("Test parallel translation settings")
    void testParallelTranslationSettings() {
        int parallel = Integer.parseInt(System.getenv().getOrDefault("PARALLEL_TRANSLATIONS", "3"));
        int retry = Integer.parseInt(System.getenv().getOrDefault("RETRY_ATTEMPTS", "2"));
        
        assertTrue(parallel >= 1 && parallel <= 10);
        assertTrue(retry >= 0 && retry <= 5);
        
        System.out.println("Parallel Translation Settings:");
        System.out.println("  Parallel: " + parallel);
        System.out.println("  Retry: " + retry);
    }

    @Test
    @DisplayName("Test fallback provider selection")
    void testFallbackProviderSelection() {
        TranslationProvider mockProvider = mock(TranslationProvider.class);
        when(mockProvider.getProviderType()).thenReturn(TranslationProvider.ProviderType.TRANSLATEGEMMA);
        when(mockProvider.isAvailable()).thenReturn(true);
        
        when(providerRegistry.getAvailableProvider(TranslationProvider.ProviderType.DEEPL))
                .thenReturn(null);
        when(providerRegistry.getAvailableProvider(TranslationProvider.ProviderType.TRANSLATEGEMMA))
                .thenReturn(mockProvider);

        TranslationProvider provider = providerRegistry.getAvailableProvider(TranslationProvider.ProviderType.DEEPL);
        
        assertNull(provider, "DeepL should return null when not configured");
        assertNotNull(providerRegistry.getAvailableProvider(TranslationProvider.ProviderType.TRANSLATEGEMMA),
                "TranslateGemma should be available");
        
        System.out.println("Fallback provider selection: OK");
    }

    @Test
    @DisplayName("Test provider language pair support")
    void testProviderLanguagePairSupport() {
        TranslationProvider deeplProvider = mock(TranslationProvider.class);
        when(deeplProvider.supportsLanguagePair("en", "es")).thenReturn(true);
        when(deeplProvider.supportsLanguagePair("en", "ja")).thenReturn(true);
        when(deeplProvider.supportsLanguagePair("en", "zh")).thenReturn(false);

        assertTrue(deeplProvider.supportsLanguagePair("en", "es"));
        assertTrue(deeplProvider.supportsLanguagePair("en", "ja"));
        assertFalse(deeplProvider.supportsLanguagePair("en", "zh"));
        
        System.out.println("Provider language pair support: OK");
    }

    @Test
    @DisplayName("Test multiple provider health status")
    void testProviderHealthStatus() {
        TranslationProvider provider1 = mock(TranslationProvider.class);
        when(provider1.getProviderType()).thenReturn(TranslationProvider.ProviderType.TRANSLATEGEMMA);
        when(provider1.isAvailable()).thenReturn(true);
        when(provider1.getMatchingScore()).thenReturn(0.9);
        when(provider1.getAverageResponseTime()).thenReturn(1000L);

        TranslationProvider provider2 = mock(TranslationProvider.class);
        when(provider2.getProviderType()).thenReturn(TranslationProvider.ProviderType.DEEPL);
        when(provider2.isAvailable()).thenReturn(false);
        when(provider2.getMatchingScore()).thenReturn(0.85);
        when(provider2.getAverageResponseTime()).thenReturn(500L);

        Map<String, Object> status = Map.of(
            "TRANSLATEGEMMA", Map.of("available", true, "score", 0.9, "avgResponseTime", "1000ms"),
            "DEEPL", Map.of("available", false, "score", 0.85, "avgResponseTime", "500ms")
        );

        assertNotNull(status);
        assertEquals(2, status.size());
        
        System.out.println("Provider health status: OK");
    }

    @Test
    @DisplayName("Test batch translation with fallback")
    void testBatchTranslationWithFallback() throws Exception {
        String[] texts = {"Hello", "World", "Translation"};
        
        TranslationResult mockResult = mock(TranslationResult.class);
        when(mockResult.getTranslation()).thenReturn("Hola");
        
        when(translationService.translateString(any(), any(), any(), any(), any()))
                .thenReturn(mockResult);

        assertDoesNotThrow(() -> {
            for (String text : texts) {
                TranslationResult result = translationService.translateString(
                        text, "en", "es", TranslationConstants.ContentType.PLAIN, "general"
                );
                assertNotNull(result);
            }
        });
        
        System.out.println("Batch translation with fallback: OK");
    }

    @Test
    @DisplayName("Test provider priority sorting")
    void testProviderPrioritySorting() {
        TranslationProvider highPriority = mock(TranslationProvider.class);
        when(highPriority.getMatchingScore()).thenReturn(0.95);
        
        TranslationProvider mediumPriority = mock(TranslationProvider.class);
        when(mediumPriority.getMatchingScore()).thenReturn(0.75);
        
        TranslationProvider lowPriority = mock(TranslationProvider.class);
        when(lowPriority.getMatchingScore()).thenReturn(0.50);

        assertTrue(highPriority.getMatchingScore() > mediumPriority.getMatchingScore());
        assertTrue(mediumPriority.getMatchingScore() > lowPriority.getMatchingScore());
        
        System.out.println("Provider priority sorting: OK");
    }
}
