# BEAD-0002: Implement Brand Compliance Analysis

**Status:** In Progress
**Priority:** High
**Project:** Agentic-AEM (Content Intelligence Agent)
**Assignee:** Gemini CLI
**Date:** March 14, 2026

## Objective
Extend the `TranslateGemma` connector to support Brand Compliance checks by comparing content against the `TerminologyService` and using Gemma to suggest corrections.

## Requirements
1.  Implement an `analyzeCompliance()` method in the `TranslateGemmaTranslationService`.
2.  The method must query the `TerminologyService` to find restricted or "off-brand" terms.
3.  Use Gemma (Vertex AI) to analyze the context and suggest brand-compliant alternatives for flagged terms.
4.  Return a `ComplianceResult` containing a list of `ComplianceViolation` objects (term, suggestion, severity).
5.  Follow **TDD**: Write the failing test case before implementation.

## Definition of Done (DoD)
-   `ComplianceResult` and `ComplianceViolation` models exist.
-   `TranslateGemmaTranslationService` interface updated.
-   Unit tests cover scenarios with multiple violations and clean content.
-   Integration with `TerminologyService` is verified via mocks.

## Next Steps
-   Create `SPEC-0002-brand-compliance.md` for detailed technical design.
-   Write failing test `TranslateGemmaComplianceTest.java`.
