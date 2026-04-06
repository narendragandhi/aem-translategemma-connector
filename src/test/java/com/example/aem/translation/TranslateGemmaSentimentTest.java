package com.example.aem.translation;

import com.example.aem.translation.impl.TranslateGemmaTranslationServiceImpl;
import com.example.aem.translation.model.SentimentResult;
import com.example.aem.translation.config.TranslateGemmaConfig;
import com.example.aem.translation.util.ResilienceHelper;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TranslateGemmaSentimentTest {

    @Spy
    private TranslateGemmaTranslationServiceImpl translationService = new TranslateGemmaTranslationServiceImpl();

    @Mock
    private TranslateGemmaConfig config;

    @Mock
    private GenerativeModel mockGenerativeModel;
    
    @Mock
    private ResilienceHelper resilienceHelper;

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
        
        // ResilienceHelper needs to be mocked to return the value of the lambda
        lenient().when(resilienceHelper.executeWithRetryAndCircuitBreaker(any(), anyString(), anyString()))
                .thenAnswer(invocation -> {
                    java.util.concurrent.Callable<String> callable = invocation.getArgument(0);
                    return callable.call();
                });
        
        // Use reflection or a setter if available to inject the mocked resilienceHelper
        // For now, we'll rely on partial mocking of the service if it creates its own
    }

    @Test
    void testAnalyzeSentimentPositive() throws Exception {
        String content = "I absolutely love the new AEM authoring interface!";
        String jsonResponse = "{\"sentiment\": \"POSITIVE\", \"confidence\": 0.9, \"reasoning\": \"The user expresses strong positive emotion.\", \"inputTokens\": 10, \"outputTokens\": 20 }";
        
        doReturn(jsonResponse).when(translationService).executeGemmaPrompt(anyString());

        SentimentResult result = translationService.analyzeSentiment(content);
        
        assertNotNull(result);
        assertEquals("POSITIVE", result.getSentiment());
        assertEquals(0.9, result.getConfidence());
        assertTrue(result.getConfidence() > 0.5);
    }

    @Test
    void testAnalyzeSentimentNegative() throws Exception {
        String content = "The documentation is confusing and many links are broken.";
        String jsonResponse = "{\"sentiment\": \"NEGATIVE\", \"confidence\": 0.8, \"reasoning\": \"The user is frustrated with documentation quality.\", \"inputTokens\": 12, \"outputTokens\": 22 }";
        
        doReturn(jsonResponse).when(translationService).executeGemmaPrompt(anyString());

        SentimentResult result = translationService.analyzeSentiment(content);
        
        assertNotNull(result);
        assertEquals("NEGATIVE", result.getSentiment());
        assertEquals(0.8, result.getConfidence());
        assertTrue(result.getConfidence() > 0.5);
    }

    @Test
    void testAnalyzeSentimentNeutral() throws Exception {
        String content = "The report was delivered on Tuesday afternoon.";
        String jsonResponse = "{\"sentiment\": \"NEUTRAL\", \"confidence\": 1.0, \"reasoning\": \"The statement is a factual observation.\", \"inputTokens\": 8, \"outputTokens\": 18 }";
        
        doReturn(jsonResponse).when(translationService).executeGemmaPrompt(anyString());

        SentimentResult result = translationService.analyzeSentiment(content);
        
        assertNotNull(result);
        assertEquals("NEUTRAL", result.getSentiment());
        assertEquals(1.0, result.getConfidence());
    }
}
