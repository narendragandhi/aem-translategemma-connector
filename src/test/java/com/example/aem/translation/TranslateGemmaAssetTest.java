package com.example.aem.translation;

import com.example.aem.translation.impl.TranslateGemmaTranslationServiceImpl;
import com.example.aem.translation.model.AssetAnalysisResult;
import com.example.aem.translation.config.TranslateGemmaConfig;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TranslateGemmaAssetTest {

    @Spy
    private TranslateGemmaTranslationServiceImpl translationService = new TranslateGemmaTranslationServiceImpl();

    @Mock
    private TranslateGemmaConfig config;

    @Mock
    private GenerativeModel mockGenerativeModel;
    
    @Mock
    private ResourceResolver resolver;
    
    @Mock
    private Resource assetResource;
    
    @Mock
    private Resource metadataResource;
    
    @Mock
    private ValueMap metadataProps;

    @BeforeEach
    void setUp() throws Exception {
        lenient().when(config.projectId()).thenReturn("test-project");
        lenient().when(config.location()).thenReturn("us-central1");
        lenient().when(config.enableCaching()).thenReturn(false);
        lenient().when(config.enableMetrics()).thenReturn(false);
        
        // Resilience config...
        lenient().when(config.retryMaxAttempts()).thenReturn(3);
        lenient().when(config.retryWaitDurationMs()).thenReturn(1000L);
        lenient().when(config.circuitBreakerFailureRateThreshold()).thenReturn(50);
        lenient().when(config.circuitBreakerSlowCallRateThreshold()).thenReturn(100);
        lenient().when(config.circuitBreakerSlowCallDurationMs()).thenReturn(5000);
        lenient().when(config.circuitBreakerWaitDurationS()).thenReturn(30);
        
        translationService.activate(config);
        translationService.setModel(mockGenerativeModel);
    }

    @Test
    void testAnalyzeAsset() throws Exception {
        String assetPath = "/content/dam/test.jpg";
        String jsonResponse = "{\"altText\": \"A professional AEM developer working at a desk.\", \"keywords\": [\"AEM\", \"Developer\", \"Office\"], \"suggestedTitle\": \"AEM Workspace\"}";
        
        when(resolver.getResource(assetPath)).thenReturn(assetResource);
        when(assetResource.getChild("jcr:content/metadata")).thenReturn(metadataResource);
        when(metadataResource.getValueMap()).thenReturn(metadataProps);
        when(metadataProps.get("dc:title", String.class)).thenReturn("Test Image");
        when(metadataProps.get("dc:description", String.class)).thenReturn("Working on AEM.");
        
        doReturn(jsonResponse).when(translationService).executeGemmaPrompt(anyString());

        AssetAnalysisResult result = translationService.analyzeAsset(assetPath, resolver);
        
        assertNotNull(result);
        assertEquals("A professional AEM developer working at a desk.", result.getAltText());
        assertTrue(result.getKeywords().contains("AEM"));
        assertEquals("AEM Workspace", result.getSuggestedTitle());
    }
}
