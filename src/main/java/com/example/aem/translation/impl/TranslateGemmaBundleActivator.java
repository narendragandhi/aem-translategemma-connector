package com.example.aem.translation.impl;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bundle activator for TranslateGemma Translation Connector.
 */
public class TranslateGemmaBundleActivator implements BundleActivator {

    private static final Logger LOG = LoggerFactory.getLogger(TranslateGemmaBundleActivator.class);
    private ServiceRegistration<?> serviceRegistration;

    @Override
    public void start(BundleContext context) throws Exception {
        LOG.info("Starting TranslateGemma Translation Connector bundle");
        // Additional initialization if needed
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        LOG.info("Stopping TranslateGemma Translation Connector bundle");
        // Cleanup if needed
    }
}