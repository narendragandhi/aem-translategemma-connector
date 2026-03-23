package com.example.aem.translation.mock;

import com.adobe.granite.translation.api.TranslationConstants;
import com.adobe.granite.translation.api.TranslationService;

import java.util.HashMap;
import java.util.Map;

public class MockTranslationServiceInfo implements TranslationService.TranslationServiceInfo {

    private final String serviceName;
    private final String serviceLabel;
    private final String attribution;
    private final TranslationConstants.TranslationMethod method;
    private final String configPath;

    public MockTranslationServiceInfo() {
        this.serviceName = "TranslateGemma Translation Service";
        this.serviceLabel = "Google TranslateGemma";
        this.attribution = "Powered by Google TranslateGemma";
        this.method = TranslationConstants.TranslationMethod.MACHINE_TRANSLATION;
        this.configPath = "/conf/global/settings/cloudconfigs/translate-gemma";
    }

    @Override
    public String getTranslationServiceName() {
        return serviceName;
    }

    @Override
    public String getTranslationServiceLabel() {
        return serviceLabel;
    }

    @Override
    public String getTranslationServiceAttribution() {
        return attribution;
    }

    @Override
    public TranslationConstants.TranslationMethod getSupportedTranslationMethod() {
        return method;
    }

    @Override
    public String getServiceCloudConfigRootPath() {
        return configPath;
    }
}
