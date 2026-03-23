# BEAD-0003: Implement App Builder Orchestrator Action

**Status:** In Progress
**Priority:** High
**Project:** Agentic-AEM (Content Intelligence Agent)
**Assignee:** Gemini CLI
**Date:** March 14, 2026

## Objective
Create an Adobe App Builder action that orchestrates the Sentiment and Brand Compliance analysis when content changes occur in AEM (via Adobe I/O Events).

## Requirements
1.  Initialize a new App Builder action `on-content-modified` in the `aem-translategemma-connector` project (under a new `app-builder` module).
2.  Configure the action to be triggered by Adobe I/O Events (e.g., `com.adobe.granite.workflow.event.WorkflowEvent.Type.STARTED` or similar).
3.  The action must call the AEM REST API to perform `analyzeSentiment` and `analyzeCompliance` on the modified content.
4.  Update the AEM resource metadata with the results (e.g., `gemma:sentiment-label`, `gemma:compliance-violations`).
5.  Implement **TDD**: Write a mock event handler test.

## Definition of Done (DoD)
-   App Builder project structure initialized.
-   `on-content-modified` action code exists.
-   `app.config.yaml` configured for AEM Eventing.
-   Tests pass using `aio app test`.

## Next Steps
-   Create `SPEC-0003-app-builder-orchestration.md` for technical architecture.
-   Bootstrap the `app-builder` module in `aem-translategemma-connector`.
