# AEM Cloud Service Deployment Guide

This guide covers deploying the TranslateGemma Translation Connector to AEM as a Cloud Service.

## Prerequisites

1. **AEM as a Cloud Service** environment access
2. **Google Cloud Platform** account with:
   - Vertex AI API enabled
   - TranslateGemma model access
   - Service account credentials (JSON key or environment variables)
3. **Adobe Cloud Manager** access
4. **Maven 3.6+** installed locally

## Cloud Service Configuration

### Required Configurations

In AEM Cloud Service console, configure:

**1. Environment Variables (Cloud Manager):**
```bash
# Add to your Cloud Service environment configuration
GOOGLE_APPLICATION_CREDENTIALS=/path/to/service-account-key.json
GOOGLE_CLOUD_PROJECT=your-project-id
GOOGLE_CLOUD_LOCATION=us-central1
```

**2. OSGi Configuration:**
Navigate to `https://<author>.adobeaemcloud.net/system/console/configMgr`

Create/Edit `TranslateGemma Translation Service Configuration`:
- **Project ID**: Your GCP project ID
- **Location**: Google Cloud region (default: `us-central1`)
- **Enable Service**: `true`
- **Default Source Language**: `en`
- **Default Target Language**: `es`
- **Max Translation Length**: `5000` (characters)
- **Connection Timeout**: `30` (seconds)
- **Read Timeout**: `60` (seconds)

## Building the Package

### Local Build
```bash
# Clean and build content package
mvn clean package -Pcloud-service

# Output: target/aem-translategemma-connector-1.0.0-SNAPSHOT.zip
```

### Profile-based Build
```bash
# Cloud Service profile (recommended)
mvn clean package -Pcloud-service

# Development profile (with debug logging)
mvn clean package -Pdev
```

## Deployment

### Option 1: Cloud Manager (Recommended)

1. Log in to [Cloud Manager](https://experience.adobe.com/cloud)
2. Select your program and environment
3. Navigate to **Upload Package**
4. Upload `aem-translategemma-connector-1.0.0-SNAPSHOT.zip`
5. Select the **TranslateGemma** application
6. Configure OSGi settings as described above
7. Click **Build & Deploy**

### Option 2: Cloud Manager CLI

```bash
# Install Cloud Manager CLI
npm install -g @adobe/aio-cli

# Login
aio cloudmanager:login

# Deploy
aio cloudmanager:upload-package <program-id> <environment-id> \
  --package target/aem-translategemma-connector-1.0.0-SNAPSHOT.zip \
  --application aem-translategemma-connector

# Wait for deployment to complete
aio cloudmanager:wait-for-build <program-id> <environment-id> <build-id>
```

## Google Cloud Setup

### 1. Create Service Account
```bash
# Create service account in GCP Console
gcloud iam service-accounts create translategemma-sa \
  --display-name="TranslateGemma Service Account" \
  --project=YOUR_PROJECT_ID
```

### 2. Grant Permissions
Grant the service account:
- **Vertex AI User** role
- **Service Account Token Creator** role

### 3. Create Key
```bash
# Download service account key
gcloud iam service-accounts keys create translategemma-sa \
  --iam-account=translategemma-sa@YOUR_PROJECT_ID.iam.gserviceaccount.com \
  --key-file-type=json \
  --key-file=translategemma-key.json
```

### 4. Enable Vertex AI
```bash
gcloud services enable aiplatform.googleapis.com --project=YOUR_PROJECT_ID
```

### 5. Set Credentials in Cloud Service
Upload the service account key to Cloud Manager secrets or environment variables.

## Verification

After deployment:

1. **Check Bundle Status:**
   ```
   https://<author>.adobeaemcloud.net/system/console/bundles
   ```
   - Find "AEM TranslateGemma Translation Connector"
   - Status should be **Active**

2. **Verify Service Registration:**
   ```
   https://<author>.adobeaemcloud.net/system/console/components
   ```
   - Search for "TranslateGemma"
   - Should show service ranking: 100

3. **Test Configuration:**
   ```
   https://<author>.adobeaemcloud.net/system/console/configMgr
   ```
   - Open "TranslateGemma Translation Service Configuration"
   - Verify all properties are set

4. **Test Translation:**
   - Navigate to **Sites** > **Create Translation Project**
   - Select "TranslateGemma Translation Service" as provider
   - Translate a test page

## Cloud Service Specific Considerations

### Security
- Service account keys stored in Cloud Manager secrets
- Use least-privilege IAM roles
- Enable Cloud Service security features (CSP, etc.)

### Performance
- Vertex AI calls may have rate limits
- Implement caching for frequently translated content
- Use async translation jobs for bulk content

### Monitoring
- Enable Cloud Service monitoring
- Track Vertex API usage and costs
- Set up alerts for translation failures

### Scaling
- Cloud Service auto-scales based on load
- Translation services run in containerized environment
- Consider regional deployment for lower latency

## Troubleshooting

### Common Cloud Service Issues

**1. Bundle Not Active**
- Check logs via Cloud Manager
- Verify Google Cloud credentials are accessible
- Ensure Vertex AI API is enabled

**2. Translation Fails**
- Verify service account has Vertex AI permissions
- Check Google Cloud quotas and limits
- Review network connectivity from Cloud Service environment

**3. Configuration Not Persisted**
- Use Cloud Service configuration (not OSGi console)
- Ensure config matches required schema
- Check for conflicting configurations

### Cloud Manager Logs

```bash
# Download logs
aio cloudmanager:download-logs <program-id> <environment-id> <log-id>

# Tail logs in real-time
aio cloudmanager:tail-logs <program-id> <environment-id> <service-id>
```

## Support

For Cloud Service-specific issues:
- Adobe Experience League: https://experienceleague.adobe.com/
- AEM Cloud Service Documentation: https://experienceleague.adobe.com/docs/cloud-service/
- Google Cloud Support: https://cloud.google.com/support

## Next Steps

After successful deployment:
1. Configure translation workflows
2. Set up content translation rules
3. Test with real content
4. Monitor performance and usage
5. Configure cost alerts for Vertex AI
