# AEM TranslateGemma Translation Connector

A custom AEM translation connector that integrates Google's TranslateGemma model for high-quality machine translation.

## Overview

This connector provides seamless integration between Adobe Experience Manager (AEM) and Google Cloud's TranslateGemma model through Vertex AI. It implements the AEM TranslationService interface to enable automatic content translation within AEM's translation workflow.

## Features

- **High-Quality Translation**: Leverages Google's TranslateGemma model for accurate translations
- **Multi-Language Support**: Supports 20+ major languages including English, Spanish, French, German, Chinese, Japanese, Arabic, and more
- **AEM Integration**: Native integration with AEM's Translation Integration Framework
- **OSGi Configuration**: Easy configuration through AEM's OSGi console
- **Both Sync and Async Support**: Supports both immediate and batch translation workflows
- **HTML Content Support**: Handles both plain text and HTML content preservation

## Prerequisites

1. **AEM as a Cloud Service** or **AEM 6.5+**
2. **Google Cloud Platform Account** with:
   - Vertex AI API enabled
   - TranslateGemma model access
3. **Java 11+**
4. **Maven 3.6+**

## Setup Instructions

### 1. Google Cloud Setup

1. Create a Google Cloud Project or use an existing one
2. Enable the Vertex AI API:
   ```bash
   gcloud services enable aiplatform.googleapis.com --project=YOUR_PROJECT_ID
   ```
3. Set up authentication (service account key or Application Default Credentials)
4. Ensure you have access to the TranslateGemma model in Vertex AI Model Garden

### 2. Build and Deploy

1. Clone or download this project
2. Build the bundle:
   ```bash
   mvn clean install
   ```
3. Deploy the bundle to AEM:
   - Using Package Manager: Upload the generated `.jar` file
   - Using Maven (if configured): `mvn clean install -PautoInstallPackage`

### 3. Configure the Service

1. Navigate to AEM Web Console: `/system/console/configMgr`
2. Find "TranslateGemma Translation Service Configuration"
3. Configure the following settings:
   - **Google Cloud Project ID**: Your GCP project ID
   - **Google Cloud Location**: e.g., `us-central1`
   - **Service Enabled**: Check to enable the service
   - **Default Source/Target Languages**: Set your preferred defaults
   - **Max Translation Length**: Maximum characters per request (default: 5000)

### 4. Configure AEM Translation Framework

1. Navigate to **Tools > Cloud Services > Translation Cloud Services**
2. Create a new cloud configuration for the TranslateGemma connector
3. In the Translation Integration Framework:
   - Select "TranslateGemma Translation Service" as the Translation Provider
   - Configure translation method (Machine Translation)
   - Set content categories and other preferences

### 5. Associate Pages for Translation

1. In Sites console, select pages to translate
2. View Properties > Cloud Services tab
3. Add the TranslateGemma configuration
4. Associate the Translation Integration Framework configuration

## Usage

### Manual Translation

1. Select content in AEM Sites
2. Use "Create Translation Project" from the tools menu
3. The TranslateGemma connector will automatically handle translation

### Programmatic Usage

```java
@Reference
private TranslateGemmaTranslationService translationService;

public void translateContent(String content, String targetLanguage) {
    try {
        TranslationResult result = translationService.translateString(
            content, "en", targetLanguage, ContentType.PLAIN, "general"
        );
        
        String translatedContent = result.getTranslatedText();
        // Use the translated content
        
    } catch (TranslationException e) {
        log.error("Translation failed", e);
    }
}
```

## Supported Languages

| Code | Language | Code | Language |
|------|----------|------|----------|
| en   | English  | fr   | French   |
| es   | Spanish  | de   | German   |
| pt   | Portuguese | it   | Italian  |
| ru   | Russian  | ja   | Japanese |
| ko   | Korean   | zh   | Chinese  |
| ar   | Arabic   | hi   | Hindi    |
| nl   | Dutch    | sv   | Swedish  |
| da   | Danish   | no   | Norwegian |
| fi   | Finnish  | pl   | Polish   |
| tr   | Turkish  |     |          |

## Configuration Properties

| Property | Description | Default |
|----------|-------------|---------|
| `projectId` | Google Cloud Project ID | Required |
| `location` | Google Cloud location | `us-central1` |
| `enabled` | Enable/disable service | `true` |
| `defaultSourceLanguage` | Default source language | `en` |
| `defaultTargetLanguage` | Default target language | `es` |
| `maxTranslationLength` | Max characters per request | `5000` |
| `connectionTimeout` | Connection timeout (seconds) | `30` |
| `readTimeout` | Read timeout (seconds) | `60` |

## Troubleshooting

### Common Issues

1. **Service Not Available**
   - Check Google Cloud authentication
   - Verify Vertex AI API is enabled
   - Confirm project ID and location are correct

2. **Translation Fails**
   - Verify source/target language codes
   - Check content length limits
   - Review Google Cloud quotas

3. **Configuration Issues**
   - Ensure OSGi configuration is saved
   - Check service ranking if multiple translators exist
   - Verify bundle is active

### Logging

Enable DEBUG logging for troubleshooting:

```xml
<Logger name="com.example.aem.translation" level="DEBUG"/>
```

## Development

### Project Structure

```
src/main/java/com/example/aem/translation/
├── config/
│   └── TranslateGemmaConfig.java         # OSGi configuration
├── service/
│   └── TranslateGemmaTranslationService.java  # Service interface
└── impl/
    ├── TranslateGemmaTranslationServiceImpl.java  # Main implementation
    └── TranslateGemmaBundleActivator.java     # Bundle lifecycle
```

### Running Tests

```bash
mvn test
```

### Building

```bash
mvn clean install
```

## Limitations

1. **API Rate Limits**: Subject to Google Cloud Vertex AI rate limits
2. **Content Size**: Maximum text length per request (configurable, default 5000 chars)
3. **Language Support**: Limited to languages supported by TranslateGemma model
4. **Synchronous Processing**: Uses synchronous API calls (async simulation available)

## Security Considerations

- Store Google Cloud credentials securely
- Use service accounts with minimum required permissions
- Consider using Application Default Credentials in production
- Monitor API usage and costs

## License

This project is provided as-is for demonstration purposes. Please ensure compliance with Google Cloud terms of service and Adobe licensing requirements.

## Support

For issues related to:
- **Google Cloud/Vertex AI**: Google Cloud Support
- **AEM Integration**: Adobe Experience League community
- **Connector Issues**: Project repository issues

## Version History

- **1.0.0-SNAPSHOT**: Initial release with TranslateGemma integration
  - Basic translation functionality
  - OSGi configuration
  - AEM Translation Framework integration
  - Support for 20+ languages