import com.adobe.granite.translation.api.TranslationConstants;
import com.adobe.granite.translation.api.TranslationException;
import com.adobe.granite.translation.api.TranslationObject;
import com.adobe.granite.translation.api.TranslationResult; // Explicitly import TranslationResult
import com.example.aem.translation.mock.MockTranslationState;
import com.adobe.granite.translation.api.TranslationState;
import com.example.aem.translation.config.TranslateGemmaConfig;
import com.example.aem.translation.impl.TranslateGemmaTranslationServiceImpl;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.api.Candidate;
import com.google.cloud.vertexai.api.Content;
import com.google.cloud.vertexai.api.Part;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.ResponseHandler;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.Job;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TranslateGemmaTranslationServiceImplTest { 

    @Spy 
    private TranslateGemmaTranslationServiceImpl translationService = new TranslateGemmaTranslationServiceImpl();

    @Mock
    private TranslateGemmaConfig config;
    
    @Mock
    private TranslationObject translationObject;

    @Mock
    private GenerativeModel mockGenerativeModel;

    @Mock
    private JobManager jobManager;

    @Mock
    private Job mockJob;

    @BeforeEach
    void setUp() {
        lenient().when(config.projectId()).thenReturn("test-project");
        lenient().when(config.location()).thenReturn("us-central1");
        lenient().when(config.modelName()).thenReturn("google/gemma-4-26b-a4b-it");
        lenient().when(config.enableMetrics()).thenReturn(false);
        lenient().when(config.enableCaching()).thenReturn(false);
        lenient().when(config.retryMaxAttempts()).thenReturn(3);
        lenient().when(config.retryWaitDurationMs()).thenReturn(1000L);
        lenient().when(config.circuitBreakerFailureRateThreshold()).thenReturn(50);
        lenient().when(config.circuitBreakerSlowCallRateThreshold()).thenReturn(100);
        lenient().when(config.circuitBreakerSlowCallDurationMs()).thenReturn(5000);
        lenient().when(config.circuitBreakerWaitDurationS()).thenReturn(30);
        
        // Mock JobManager behavior
        lenient().when(jobManager.addJob(anyString(), anyMap())).thenReturn(mockJob);
        lenient().when(mockJob.getId()).thenReturn("test-job-id-123");

        translationService.activate(config);
        translationService.setModel(mockGenerativeModel);
    }

    @Test
    void testCreateTranslationJob() throws TranslationException {
        String jobId = translationService.createTranslationJob("test-job", "A test job", "en", "es", new Date(), new MockTranslationState(TranslationConstants.TranslationStatus.DRAFT), null);
        assertNotNull(jobId);
        assertEquals(TranslationConstants.TranslationStatus.DRAFT, translationService.getTranslationJobStatus(jobId));
    }

    @Test
    void testUploadAndQueueJob() throws TranslationException, IOException {
        String originalContent = "Hello, world!";

        when(translationObject.getContent()).thenReturn(originalContent);
        when(translationObject.getPath()).thenReturn("test-object-id");
        when(translationObject.getContentType()).thenReturn(TranslationConstants.ContentType.PLAIN);

        String jobId = translationService.createTranslationJob("test-job", "A test job", "en", "es", new Date(), new MockTranslationState(TranslationConstants.TranslationStatus.DRAFT), null);

        String objectId = translationService.uploadTranslationObject(jobId, translationObject);
        assertEquals("test-object-id", objectId);

        // Verify that a Sling Job was actually added to the topic
        verify(jobManager, times(1)).addJob(eq("com/example/aem/translation/gemma/batch"), anyMap());
    }

    @Test
    void testTranslateStringSuccess() throws TranslationException, IOException {
        String sourceString = "Hello, world!";
        String targetLanguage = "es";
        String translatedString = "Hola, mundo!";

        GenerateContentResponse mockResponse = GenerateContentResponse.newBuilder()
            .addCandidates(Candidate.newBuilder()
                .setContent(Content.newBuilder().addParts(Part.newBuilder().setText(translatedString))))
            .build();

        when(mockGenerativeModel.generateContent(anyString())).thenReturn(mockResponse);

        TranslationResult result = translationService.translateString(sourceString, "en", targetLanguage, TranslationConstants.ContentType.PLAIN, "general");

        assertNotNull(result);
        assertEquals("en", result.getSourceLanguage());
        assertEquals(targetLanguage, result.getTargetLanguage());
        assertEquals(sourceString, result.getSourceString());
        assertEquals(translatedString, result.getTranslation());
        verify(mockGenerativeModel, times(1)).generateContent(anyString());
    }

    @Test
    void testDetectLanguageSuccess() throws TranslationException, IOException {
        String detectSource = "Hello, world!";
        String detectedLanguage = "en";

        GenerateContentResponse mockResponse = GenerateContentResponse.newBuilder()
            .addCandidates(Candidate.newBuilder()
                .setContent(Content.newBuilder().addParts(Part.newBuilder().setText(detectedLanguage))))
            .build();

        when(mockGenerativeModel.generateContent(anyString())).thenReturn(mockResponse);

        String result = translationService.detectLanguage(detectSource, TranslationConstants.ContentType.PLAIN);

        assertNotNull(result);
        assertEquals(detectedLanguage, result);
        verify(mockGenerativeModel, times(1)).generateContent(anyString());
    }
}

