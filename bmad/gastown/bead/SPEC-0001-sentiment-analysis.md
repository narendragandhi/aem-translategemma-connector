# SPEC-0001: Content Sentiment Analysis Technical Specification

## 1. Goal
Integrate sentiment analysis capabilities into the `TranslateGemma` connector to allow AEM to automatically evaluate content quality and tone.

## 2. Technical Design

### 2.1 Interface Changes
Add `analyzeSentiment` to the `TranslateGemmaTranslationService` interface:

```java
public interface TranslateGemmaTranslationService extends TranslationService {
    // Existing methods...
    SentimentResult analyzeSentiment(String content) throws TranslateGemmaException;
}
```

### 2.2 Model Contract
Create a `SentimentResult` POJO:

```java
public class SentimentResult {
    private final String content;
    private final double score; // -1.0 to 1.0
    private final String label; // POSITIVE, NEGATIVE, NEUTRAL
    private final String explanation;

    // Constructor, getters, and toString
}
```

### 2.3 LLM Prompting Strategy
Use a specialized system prompt for Gemma:
> "Analyze the sentiment of the following text. Provide a score between -1.0 and 1.0 (where -1.0 is strongly negative and 1.0 is strongly positive), a one-word label (POSITIVE, NEGATIVE, NEUTRAL), and a brief one-sentence explanation. Respond ONLY in JSON format: {\"score\": 0.0, \"label\": \"NEUTRAL\", \"explanation\": \"...\"}"

### 2.4 Resilience and Performance
-   Apply the existing `Resilience4j` retry and circuit breaker logic.
-   Add sentiment results to the existing `Caffeine` cache with a "sentiment:" prefix for keys.

## 3. Test Cases (TDD)
-   **Positive:** "I absolutely love the new AEM authoring interface!" -> Score > 0.5, POSITIVE.
-   **Negative:** "The documentation is confusing and many links are broken." -> Score < -0.5, NEGATIVE.
-   **Neutral:** "The weather today is cloudy with occasional showers." -> Score ~0.0, NEUTRAL.
-   **Error Handling:** Simulate a `CIRCUIT_BREAKER_OPEN` scenario.

## 4. Dependencies
-   Existing Vertex AI SDK.
-   `GSON` for parsing Gemma's JSON response.
