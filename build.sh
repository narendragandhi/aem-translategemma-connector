#!/bin/bash

# AEM TranslateGemma Connector Build Script
# This script builds and optionally deploys the connector to AEM

set -e

# Configuration
AEM_HOST=${AEM_HOST:-localhost}
AEM_PORT=${AEM_PORT:-4502}
AEM_USER=${AEM_USER:-admin}
AEM_PASSWORD=${AEM_PASSWORD:-admin}
BUNDLE_NAME="aem-translategemma-connector"
VERSION="1.0.0-SNAPSHOT"

echo "🔧 Building AEM TranslateGemma Connector..."

# Clean and build
echo "📦 Building project..."
mvn clean install -DskipTests

# Check if build was successful
if [ $? -eq 0 ]; then
    echo "✅ Build successful!"
    echo "📄 Bundle created: target/${BUNDLE_NAME}-${VERSION}.jar"
else
    echo "❌ Build failed!"
    exit 1
fi

# Optional deployment
if [ "$1" == "deploy" ]; then
    echo "🚀 Deploying to AEM at ${AEM_HOST}:${AEM_PORT}..."
    
    # Install bundle using curl
    curl -u "${AEM_USER}:${AEM_PASSWORD}" \
         -F "action=install" \
         -F "bundlefile=@target/${BUNDLE_NAME}-${VERSION}.jar" \
         -F "start=true" \
         "http://${AEM_HOST}:${AEM_PORT}/system/console/bundles"
    
    if [ $? -eq 0 ]; then
        echo "✅ Bundle deployed successfully!"
        echo "🔗 Check status: http://${AEM_HOST}:${AEM_PORT}/system/console/bundles"
    else
        echo "❌ Deployment failed!"
        exit 1
    fi
fi

echo "🎉 Build process completed!"
echo ""
echo "Next steps:"
echo "1. Configure the service in AEM at /system/console/configMgr"
echo "2. Set up Translation Cloud Service configuration"
echo "3. Associate pages for translation"
echo ""
echo "Usage: $0 [deploy]"
echo "  deploy - Deploy the bundle to AEM (optional)"