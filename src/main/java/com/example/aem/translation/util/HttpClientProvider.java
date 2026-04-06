package com.example.aem.translation.util;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Enterprise-grade Shared HTTP Client Provider.
 * Prevents Socket Exhaustion using a Managed Connection Pool.
 */
@Component(service = HttpClientProvider.class, immediate = true)
public class HttpClientProvider {

    private static final Logger LOG = LoggerFactory.getLogger(HttpClientProvider.class);
    private PoolingHttpClientConnectionManager connectionManager;
    private CloseableHttpClient httpClient;

    @Activate
    protected void activate() {
        connectionManager = new PoolingHttpClientConnectionManager();
        // Principal's Choice: Tuning for High-Concurrency AEM environments
        connectionManager.setMaxTotal(200); 
        connectionManager.setDefaultMaxPerRoute(50);
        
        httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setConnectionManagerShared(true) // Ensure the manager stays alive
                .build();
        
        LOG.info("Shared HttpClientProvider activated with pool size: 200");
    }

    @Deactivate
    protected void deactivate() {
        try {
            if (httpClient != null) httpClient.close();
            if (connectionManager != null) connectionManager.close();
        } catch (IOException e) {
            LOG.error("Error closing HttpClientProvider", e);
        }
    }

    public CloseableHttpClient getHttpClient() {
        return httpClient;
    }
}
