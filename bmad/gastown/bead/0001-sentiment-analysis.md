# BEAD-0001: Implement Content Sentiment Analysis

**Status:** In Progress
**Priority:** High
**Project:** Agentic-AEM (Content Intelligence Agent)
**Assignee:** Gemini CLI
**Date:** March 14, 2026

## Objective
Extend the `TranslateGemma` connector to support content sentiment analysis using Vertex AI (Gemma 2b-it).

## Requirements
1.  Implement an `analyzeSentiment()` method in the `TranslateGemmaTranslationService`.
2.  Use the existing Vertex AI integration to prompt Gemma for sentiment evaluation.
3.  The method must return a `SentimentResult` containing a score (-1.0 to 1.0) and a label (POSITIVE, NEGATIVE, NEUTRAL).
4.  Follow **TDD**: Write the failing test case before implementation.

## Definition of Done (DoD)
-   `SentimentResult` interface/POJO exists.
-   `TranslateGemmaTranslationService` interface updated.
-   Unit tests cover positive, negative, and neutral scenarios.
-   Code adheres to existing `Resilience4j` and caching patterns.

## Next Steps
-   Create `SPEC-0001-sentiment-analysis.md` for detailed technical design.
-   Write failing test `TranslateGemmaSentimentTest.java`.
