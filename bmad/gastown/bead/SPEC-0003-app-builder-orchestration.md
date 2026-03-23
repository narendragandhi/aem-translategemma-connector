# SPEC-0003: App Builder Orchestration Architecture

## 1. Goal
Decouple the intelligence orchestration from the AEM core using Adobe App Builder. This ensures a "Clean Core" and allows for independent scaling of the content intelligence logic.

## 2. Technical Design

### 2.1 Event-Driven Flow
1.  **AEM Cloud Service** emits an event (via Adobe I/O) when a Page or Asset is modified.
2.  **Adobe I/O Events** triggers the `on-content-modified` action in App Builder.
3.  **App Builder Action**:
    *   Authenticates with AEM via Service User (JWT/OAuth).
    *   Retrieves the modified content (text/properties).
    *   Calls the `TranslateGemma` REST API endpoint (to be implemented) for sentiment and compliance.
    *   Writes the resulting JSON back to the AEM resource metadata nodes.

### 2.2 App Builder Action (Node.js)
The action will use the `@adobe/aio-sdk` to handle events and AEM communication.

```javascript
async function main(params) {
  const { event } = params;
  const payloadPath = event.data.payload.path;
  
  // 1. Get Content from AEM
  const content = await getAemContent(payloadPath);
  
  // 2. Call Content Intelligence (Gemma)
  const sentiment = await callGemmaSentiment(content);
  const compliance = await callGemmaCompliance(content);
  
  // 3. Update AEM Metadata
  await updateAemMetadata(payloadPath, {
    'gemma:sentiment': sentiment,
    'gemma:compliance': compliance
  });
  
  return { statusCode: 200 };
}
```

### 2.3 AEM REST API (Sling Servlet)
We need a REST interface in `aem-translategemma-connector` to expose the new analysis methods.
- **Endpoint:** `/bin/translategemma/analyze`
- **Method:** `POST`
- **Payload:** `{ "path": "...", "type": "sentiment|compliance" }`

## 3. Test Cases (TDD)
-   **Event Parsing:** Verify the action correctly extracts the payload path from the Adobe I/O event structure.
-   **AEM Auth:** Verify the action can successfully obtain an access token for AEM.
-   **API Integration:** Mock the AEM REST endpoint and verify the action sends the correct content for analysis.

## 4. Security
-   Store AEM credentials in App Builder **Secret Variables**.
-   Use **Encrypted Parameters** for Vertex AI API keys if direct access is needed.
