# SPEC-0004: DAM Metadata Automation Technical Specification

## 1. Goal
Automate the enrichment of DAM assets with high-quality Alt-Text and SEO keywords using LLM-based analysis of existing metadata and/or visual content.

## 2. Technical Design

### 2.1 Interface Changes
Add `analyzeAsset` to `TranslateGemmaTranslationService`:

```java
public interface TranslateGemmaTranslationService extends TranslationService {
    // ...
    AssetAnalysisResult analyzeAsset(String assetPath, ResourceResolver resolver) throws TranslateGemmaException;
}
```

### 2.2 Model Contract
Create `AssetAnalysisResult` POJO:

```java
public class AssetAnalysisResult {
    private final String altText;
    private final List<String> keywords;
    private final String suggestedTitle;

    // ...
}
```

### 2.3 Implementation Logic
1.  **Metadata Extraction:** Extract `dc:title`, `dc:description`, and any existing tags from the asset's metadata node.
2.  **LLM Prompting (Metadata-driven):**
    > "Given the following asset metadata (Title: '{title}', Description: '{description}'), generate a concise, accessible Alt-Text for a screen reader and a list of 10 relevant SEO keywords. Respond only in JSON: {\"altText\": \"...\", \"keywords\": [\"...\"], \"suggestedTitle\": \"...\"}"
3.  **Future Expansion (Visual):** If Vertex AI vision models are enabled, send the asset's low-res rendition for true visual analysis.

## 3. Test Cases (TDD)
-   **Enrichment:** Asset with minimal metadata -> Gemma generates full alt-text and keywords.
-   **Consistency:** Asset with existing metadata -> Gemma refines and expands the keywords.
-   **Error Handling:** Handle cases where the asset path is invalid or metadata is missing.

## 4. Metadata Mapping
- `altText` -> `dc:description` (if empty) or a custom `gemma:altText` property.
- `keywords` -> `dc:subject` (Dublin Core Tags).
