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
        String jsonResponse = "{\"compliant\": false, \"feedback\": \"The term 'cheap' implies low quality.\", \"confidence\": 0.9, \"reasoning\": \"Brand guidelines recommend 'cost-effective' instead.\", \"inputTokens\": 10, \"outputTokens\": 25}";
        
        doReturn(jsonResponse).when(translationService).executeGemmaPrompt(anyString());

        ComplianceResult result = translationService.analyzeCompliance(content);
        
        assertNotNull(result);
        assertFalse(result.isCompliant());
        assertEquals("The term 'cheap' implies low quality.", result.getFeedback());
        assertEquals(0.9, result.getConfidence());
    }

    @Test
    void testAnalyzeComplianceNoViolations() throws Exception {
        String content = "We offer premium enterprise-grade cloud infrastructure.";
        String jsonResponse = "{\"compliant\": true, \"feedback\": \"Content is compliant.\", \"confidence\": 1.0, \"reasoning\": \"No issues found.\", \"inputTokens\": 12, \"outputTokens\": 15}";
        
        doReturn(jsonResponse).when(translationService).executeGemmaPrompt(anyString());

        ComplianceResult result = translationService.analyzeCompliance(content);
        
        assertNotNull(result);
        assertTrue(result.isCompliant());
        assertEquals(1.0, result.getConfidence());
    }
}
