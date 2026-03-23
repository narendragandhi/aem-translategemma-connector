# SPEC-0002: Brand Compliance Agent Technical Specification

## 1. Goal
Build an automated brand compliance agent that identifies prohibited terminology in AEM content and suggests brand-approved alternatives.

## 2. Technical Design

### 2.1 Interface Changes
Extend `TranslateGemmaTranslationService` with `analyzeCompliance`:

```java
public interface TranslateGemmaTranslationService extends TranslationService {
    // Existing methods...
    ComplianceResult analyzeCompliance(String content) throws TranslateGemmaException;
}
```

### 2.2 Model Contract
Create `ComplianceResult` and `ComplianceViolation` POJOs:

```java
public class ComplianceResult {
    private final String content;
    private final List<ComplianceViolation> violations;
    private final boolean isCompliant;

    // ...
}

public class ComplianceViolation {
    private final String flaggedTerm;
    private final String suggestion;
    private final String reason;
    private final Severity severity; // LOW, MEDIUM, HIGH

    public enum Severity { LOW, MEDIUM, HIGH }
}
```

### 2.3 Implementation Logic
1.  **Terminology Check:** Call `terminologyService.findAllTerms()` to scan for prohibited or restricted words from the project's brand ontology.
2.  **LLM Prompting (Contextual Suggestions):** For each flagged term, use Gemma to generate a brand-appropriate suggestion based on the context.
    > "The text contains the following prohibited term: '{flaggedTerm}'. Based on the surrounding context: '{context}', suggest a more brand-aligned alternative. Respond only in JSON: {\"suggestion\": \"...\", \"reason\": \"...\", \"severity\": \"HIGH\"}"
3.  **Result Aggregation:** Combine terminology hits with Gemma's suggestions into a final `ComplianceResult`.

## 3. Test Cases (TDD)
-   **Violation Found:** "We offer cheap cloud services." -> Flag "cheap", suggest "cost-effective" or "affordable".
-   **No Violation:** "We offer premium enterprise-grade cloud infrastructure." -> `isCompliant = true`.
-   **Severity Context:** "This is a stupid error." -> Flag "stupid", High Severity.

## 4. Dependencies
-   Existing `TerminologyService` implementation.
-   Vertex AI for Gemma-based suggestions.
-   `Jackson` for JSON parsing.
