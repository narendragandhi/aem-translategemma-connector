package com.example.aem.translation;

import com.adobe.granite.translation.api.TranslationConstants;
import com.adobe.granite.translation.api.TranslationResult;
import com.adobe.granite.translation.api.TranslationService;
import com.example.aem.translation.config.TranslateGemmaConfig;
import com.example.aem.translation.mock.MockTranslationResult;
import com.example.aem.translation.mock.MockTranslationServiceInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AEM TranslateGemma Connector - Unit Tests")
public class TranslateGemmaConnectorTest {

    @Test
    @DisplayName("Test TranslationConstants.ContentType enum values")
    void testContentTypeEnum() {
        TranslationConstants.ContentType[] types = TranslationConstants.ContentType.values();
        assertEquals(3, types.length);
        assertNotNull(TranslationConstants.ContentType.PLAIN);
        assertNotNull(TranslationConstants.ContentType.HTML);
        assertNotNull(TranslationConstants.ContentType.MARKDOWN);
    }

    @Test
    @DisplayName("Test TranslationConstants.TranslationStatus enum values")
    void testTranslationStatusEnum() {
        TranslationConstants.TranslationStatus[] statuses = TranslationConstants.TranslationStatus.values();
        assertTrue(statuses.length >= 15);
        assertNotNull(TranslationConstants.TranslationStatus.DRAFT);
        assertNotNull(TranslationConstants.TranslationStatus.COMPLETE);
        assertNotNull(TranslationConstants.TranslationStatus.TRANSLATION_IN_PROGRESS);
    }

    @Test
    @DisplayName("Test TranslationConstants.TranslationMethod enum values")
    void testTranslationMethodEnum() {
        TranslationConstants.TranslationMethod[] methods = TranslationConstants.TranslationMethod.values();
        assertEquals(4, methods.length);
        assertNotNull(TranslationConstants.TranslationMethod.HUMAN);
        assertNotNull(TranslationConstants.TranslationMethod.MACHINE);
        assertNotNull(TranslationConstants.TranslationMethod.MACHINE_TRANSLATION);
        assertNotNull(TranslationConstants.TranslationMethod.HYBRID);
    }

    @Test
    @DisplayName("Test MockTranslationResult creation")
    void testMockTranslationResult() {
        MockTranslationResult result = MockTranslationResult.create(
            "en", "es", "Hello", "Hola"
        );

        assertNotNull(result);
        assertEquals("en", result.getSourceLanguage());
        assertEquals("es", result.getTargetLanguage());
        assertEquals("Hello", result.getSourceString());
        assertEquals("Hola", result.getTranslation());
        assertEquals(TranslationConstants.ContentType.PLAIN, result.getContentType());
    }

    @Test
    @DisplayName("Test MockTranslationServiceInfo creation")
    void testMockTranslationServiceInfo() {
        MockTranslationServiceInfo info = new MockTranslationServiceInfo();

        assertNotNull(info);
        assertEquals("TranslateGemma Translation Service", info.getTranslationServiceName());
        assertEquals("Google TranslateGemma", info.getTranslationServiceLabel());
        assertTrue(info.getTranslationServiceAttribution().contains("Google"));
        assertEquals(TranslationConstants.TranslationMethod.MACHINE_TRANSLATION,
                     info.getSupportedTranslationMethod());
        assertTrue(info.getServiceCloudConfigRootPath().contains("translate-gemma"));
    }

    @Test
    @DisplayName("Test TranslationResult interface contract")
    void testTranslationResultContract() {
        TranslationResult result = new TranslationResult() {
            @Override
            public String getSourceLanguage() { return "fr"; }
            @Override
            public String getTargetLanguage() { return "de"; }
            @Override
            public String getSourceString() { return "Bonjour"; }
            @Override
            public String getTranslation() { return "Guten Tag"; }
            @Override
            public TranslationConstants.ContentType getContentType() {
                return TranslationConstants.ContentType.PLAIN;
            }
            @Override
            public String getCategory() { return "general"; }
            @Override
            public int getRating() { return 5; }
            @Override
            public String getUserId() { return "test-user"; }
        };

        assertEquals("fr", result.getSourceLanguage());
        assertEquals("de", result.getTargetLanguage());
        assertEquals("Bonjour", result.getSourceString());
        assertEquals("Guten Tag", result.getTranslation());
        assertEquals(TranslationConstants.ContentType.PLAIN, result.getContentType());
        assertEquals("general", result.getCategory());
        assertEquals(5, result.getRating());
        assertEquals("test-user", result.getUserId());
    }

    @Test
    @DisplayName("Test TranslationService.TranslationServiceInfo interface contract")
    void testTranslationServiceInfoContract() {
        TranslationService.TranslationServiceInfo info = new TranslationService.TranslationServiceInfo() {
            @Override
            public String getTranslationServiceName() { return "Test Service"; }
            @Override
            public String getTranslationServiceLabel() { return "Test Label"; }
            @Override
            public String getTranslationServiceAttribution() { return "Test Attribution"; }
            @Override
            public TranslationConstants.TranslationMethod getSupportedTranslationMethod() {
                return TranslationConstants.TranslationMethod.HUMAN;
            }
            @Override
            public String getServiceCloudConfigRootPath() { return "/conf/test"; }
        };

        assertEquals("Test Service", info.getTranslationServiceName());
        assertEquals("Test Label", info.getTranslationServiceLabel());
        assertEquals("Test Attribution", info.getTranslationServiceAttribution());
        assertEquals(TranslationConstants.TranslationMethod.HUMAN, info.getSupportedTranslationMethod());
        assertEquals("/conf/test", info.getServiceCloudConfigRootPath());
    }

    @Test
    @DisplayName("Test TranslationStatus.fromString method")
    void testTranslationStatusFromString() {
        assertEquals(TranslationConstants.TranslationStatus.DRAFT,
                     TranslationConstants.TranslationStatus.fromString("DRAFT"));
        assertEquals(TranslationConstants.TranslationStatus.COMPLETE,
                     TranslationConstants.TranslationStatus.fromString("complete"));
        assertEquals(TranslationConstants.TranslationStatus.UNKNOWN_STATE,
                     TranslationConstants.TranslationStatus.fromString("invalid"));
    }
}
