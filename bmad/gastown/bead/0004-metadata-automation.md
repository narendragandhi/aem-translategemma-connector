# BEAD-0004: Implement DAM Metadata Automation

**Status:** In Progress
**Priority:** Medium
**Project:** Agentic-AEM (Content Intelligence Agent)
**Assignee:** Gemini CLI
**Date:** March 14, 2026

## Objective
Enable the Agentic-AEM system to automatically generate Alt-Text and SEO keywords for DAM assets using Gemma's multi-modal capabilities (or descriptive metadata analysis).

## Requirements
1.  Extend `TranslateGemmaTranslationService` with `analyzeAsset(Resource asset)`.
2.  Implement logic to extract existing asset metadata (title, description, tags).
3.  Use Gemma to generate:
    *   **Alt-Text:** A concise, accessible description of the asset.
    *   **SEO Keywords:** A list of 5-10 relevant keywords for search optimization.
4.  Update the `TranslateGemmaAnalysisServlet` to handle `dam:Asset` resources.
5.  Follow **TDD**: Write failing tests for asset analysis.

## Definition of Done (DoD)
-   `AssetAnalysisResult` model exists.
-   `analyzeAsset` method implemented and tested.
-   App Builder action updated to trigger asset analysis on `AssetCreated` events.
-   Metadata is written back to `jcr:content/metadata` (e.g., `dc:description`, `dc:subject`).

## Next Steps
-   Create `SPEC-0004-metadata-automation.md`.
-   Write `TranslateGemmaAssetTest.java`.
