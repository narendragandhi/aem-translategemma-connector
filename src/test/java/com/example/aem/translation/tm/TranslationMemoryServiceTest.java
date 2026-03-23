package com.example.aem.translation.tm;

import com.adobe.granite.translation.api.TranslationConstants;
import com.example.aem.translation.tm.impl.JcrTranslationMemoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TranslationMemoryServiceTest {

    private JcrTranslationMemoryService tmService;

    @BeforeEach
    void setUp() {
        tmService = new JcrTranslationMemoryService();
    }

    @Test
    void testStoreAndRetrieveExactMatch() throws Exception {
        String source = "Hello World";
        String target = "Hola Mundo";
        
        tmService.storeTranslation(source, target, "en", "es", 
            TranslationConstants.ContentType.PLAIN, "general", "/content/page", 5);

        List<TranslationMemoryService.TMEntry> matches = tmService.findMatches(source, "en", "es", 
            TranslationConstants.ContentType.PLAIN, "general", 0.85, 5);

        assertFalse(matches.isEmpty());
        assertEquals(target, matches.get(0).getTargetText());
    }

    @Test
    void testFindMatchesWithFuzzyMatch() throws Exception {
        tmService.storeTranslation("Hello World", "Hola Mundo", "en", "es", 
            TranslationConstants.ContentType.PLAIN, "general", "/page1", 5);
        tmService.storeTranslation("Hello There", "Hola Alla", "en", "es", 
            TranslationConstants.ContentType.PLAIN, "general", "/page2", 4);

        List<TranslationMemoryService.TMEntry> matches = tmService.findMatches("Hello World!", "en", "es", 
            TranslationConstants.ContentType.PLAIN, "general", 0.5, 5);

        assertFalse(matches.isEmpty());
    }

    @Test
    void testNoMatchForDifferentLanguagePair() throws Exception {
        tmStoreTranslation("Hello", "Bonjour", "en", "fr", TranslationConstants.ContentType.PLAIN);
        
        List<TranslationMemoryService.TMEntry> matches = tmService.findMatches("Hello", "en", "es", 
            TranslationConstants.ContentType.PLAIN, "general", 0.85, 5);

        assertTrue(matches.isEmpty());
    }

    @Test
    void testNoMatchForBlankSource() {
        List<TranslationMemoryService.TMEntry> matches = tmService.findMatches("", "en", "es", 
            TranslationConstants.ContentType.PLAIN, "general", 0.85, 5);

        assertTrue(matches.isEmpty());
    }

    @Test
    void testNoMatchForBlankTarget() throws Exception {
        tmStoreTranslation("Hello", "", "en", "es", TranslationConstants.ContentType.PLAIN);

        List<TranslationMemoryService.TMEntry> matches = tmService.findMatches("Hello", "en", "es", 
            TranslationConstants.ContentType.PLAIN, "general", 0.85, 5);

        assertTrue(matches.isEmpty());
    }

    @Test
    void testStatistics() throws Exception {
        tmStoreTranslation("Hello", "Hola", "en", "es", TranslationConstants.ContentType.PLAIN);
        tmStoreTranslation("World", "Mundo", "en", "es", TranslationConstants.ContentType.PLAIN);
        tmStoreTranslation("Hello", "Bonjour", "en", "fr", TranslationConstants.ContentType.PLAIN);

        Map<String, Integer> stats = tmService.getStatistics();

        assertEquals(3, stats.get("totalEntries"));
        assertEquals(2, stats.get("uniqueLanguagePairs"));
    }

    @Test
    void testClearMemory() throws Exception {
        tmStoreTranslation("Hello", "Hola", "en", "es", TranslationConstants.ContentType.PLAIN);

        tmService.clearMemory();

        Map<String, Integer> stats = tmService.getStatistics();
        assertEquals(0, stats.get("totalEntries"));
    }

    @Test
    void testStoragePath() {
        assertEquals("/var/translationmemory/translategemma", tmService.getStoragePath());
    }

    @Test
    void testMinScoreThreshold() throws Exception {
        tmStoreTranslation("The quick brown fox", "El rapido zorro cafe", "en", "es", 
            TranslationConstants.ContentType.PLAIN);

        List<TranslationMemoryService.TMEntry> highThreshold = tmService.findMatches("The quick brown fox", "en", "es", 
            TranslationConstants.ContentType.PLAIN, "general", 0.95, 5);
        
        List<TranslationMemoryService.TMEntry> lowThreshold = tmService.findMatches("The quick brown fox", "en", "es", 
            TranslationConstants.ContentType.PLAIN, "general", 0.5, 5);

        assertTrue(highThreshold.size() >= 1);
        assertTrue(lowThreshold.size() >= 1);
    }

    @Test
    void testContentTypeFiltering() throws Exception {
        tmStoreTranslation("<p>Hello</p>", "<p>Hola</p>", "en", "es", TranslationConstants.ContentType.HTML);
        tmStoreTranslation("Hello", "Hola", "en", "es", TranslationConstants.ContentType.PLAIN);

        List<TranslationMemoryService.TMEntry> htmlMatches = tmService.findMatches("<p>Hello</p>", "en", "es", 
            TranslationConstants.ContentType.HTML, "general", 0.85, 5);

        assertFalse(htmlMatches.isEmpty());
    }

    @Test
    void testMaxResultsLimit() throws Exception {
        for (int i = 0; i < 10; i++) {
            tmStoreTranslation("Text " + i, "Texto " + i, "en", "es", 
                TranslationConstants.ContentType.PLAIN);
        }

        List<TranslationMemoryService.TMEntry> limited = tmService.findMatches("Text", "en", "es", 
            TranslationConstants.ContentType.PLAIN, "general", 0.3, 3);

        assertTrue(limited.size() <= 3);
    }

    private void tmStoreTranslation(String source, String target, String sourceLang, 
                                   String targetLang, TranslationConstants.ContentType contentType) 
                                   throws Exception {
        tmService.storeTranslation(source, target, sourceLang, targetLang, 
            contentType, "general", "/content/test", 5);
    }
}
