package com.example.aem.translation;

import com.example.aem.translation.impl.TranslateGemmaTranslationServiceImpl;
import com.example.aem.translation.model.ComplianceResult;
import com.example.aem.translation.config.TranslateGemmaConfig;
import com.example.aem.translation.terminology.TerminologyService;
import com.example.aem.translation.terminology.TerminologyMatch;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TranslateGemmaComplianceTest {

    @Spy
    private TranslateGemmaTranslationServiceImpl translationService = new TranslateGemmaTranslationServiceImpl();

    @Mock
    private TranslateGemmaConfig config;

    @Mock
    private GenerativeModel mockGenerativeModel;
    
    @Mock
    private TerminologyService terminologyService;

    @BeforeEach
    void setUp() throws Exception {
        lenient().when(config.projectId()).thenReturn("test-project");
        lenient().when(config.location()).thenReturn("us-central1");
        lenient().when(config.enableCaching()).thenReturn(false);
        lenient().when(config.enableMetrics()).thenReturn(false);
        
        // Mock Resilience4j config values
        lenient().when(config.retryMaxAttempts()).thenReturn(3);
        lenient().when(config.retryWaitDurationMs()).thenReturn(1000L);
        lenient().when(config.circuitBreakerFailureRateThreshold()).thenReturn(50);
        lenient().when(config.circuitBreakerSlowCallRateThreshold()).thenReturn(100);
        lenient().when(config.circuitBreakerSlowCallDurationMs()).thenReturn(5000);
        lenient().when(config.circuitBreakerWaitDurationS()).thenReturn(30);
        
        translationService.activate(config);
        translationService.setModel(mockGenerativeModel);
        translationService.setTerminologyService(terminologyService);
    }

    @Test
    void testAnalyzeComplianceWithViolations() throws Exception {
        String content = "We offer cheap cloud services.";
        String jsonResponse = "{\"flaggedTerm\": \"cheap\", \"suggestion\": \"cost-effective\", \"reason\": \"The term 'cheap' implies low quality.\", \"severity\": \"MEDIUM\"}";
        
        // Mock terminologyService to find 'cheap'
        TerminologyMatch match = new TerminologyMatch("cheap", "cost-effective", "brand", "en", "en", 1.0f, "id-1");
        when(terminologyService.findAllTerms(anyString(), anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(Collections.singletonList(match));
        
        doReturn(jsonResponse).when(translationService).executeGemmaPrompt(anyString());

        ComplianceResult result = translationService.analyzeCompliance(content);
        
        assertNotNull(result);
        assertFalse(result.isCompliant());
        assertEquals(1, result.getViolations().size());
        assertEquals("cheap", result.getViolations().get(0).getFlaggedTerm());
        assertEquals("cost-effective", result.getViolations().get(0).getSuggestion());
    }

    @Test
    void testAnalyzeComplianceNoViolations() throws Exception {
        String content = "We offer premium enterprise-grade cloud infrastructure.";
        
        when(terminologyService.findAllTerms(anyString(), anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(Collections.emptyList());

        ComplianceResult result = translationService.analyzeCompliance(content);
        
        assertNotNull(result);
        assertTrue(result.isCompliant());
        assertTrue(result.getViolations().isEmpty());
    }
}
