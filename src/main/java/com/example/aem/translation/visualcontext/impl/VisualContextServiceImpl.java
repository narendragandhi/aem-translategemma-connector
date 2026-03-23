package com.example.aem.translation.visualcontext.impl;

import com.example.aem.translation.visualcontext.VisualContextService;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component(
    service = VisualContextService.class,
    immediate = true
)
public class VisualContextServiceImpl implements VisualContextService {

    private static final Logger LOG = LoggerFactory.getLogger(VisualContextServiceImpl.class);

    private static final int SCREENSHOT_WIDTH = 1280;
    private static final int SCREENSHOT_HEIGHT = 720;

    private final Map<String, ContextCapture> captureCache = new ConcurrentHashMap<>();

    @Override
    public ContextCapture capturePageContext(String pagePath) throws Exception {
        return capturePageContext(pagePath, null);
    }

    @Override
    public ContextCapture capturePageContext(String pagePath, String selector) throws Exception {
        LOG.info("Capturing visual context for page: {}", pagePath);

        String url = generateContextUrl(pagePath);
        if (selector != null) {
            url += "." + selector;
        }
        url += ".html";

        byte[] screenshot = captureScreenshot(url);

        Map<String, String> metadata = new HashMap<>();
        metadata.put("url", url);
        metadata.put("pagePath", pagePath);
        metadata.put("selector", selector);
        metadata.put("viewport", SCREENSHOT_WIDTH + "x" + SCREENSHOT_HEIGHT);
        metadata.put("captureMethod", "http-rendering");

        String cacheKey = pagePath + (selector != null ? ":" + selector : "");
        ContextCapture capture = new ContextCapture(pagePath, "page", screenshot, metadata);
        captureCache.put(cacheKey, capture);

        LOG.debug("Captured visual context for page: {}", pagePath);
        
        return capture;
    }

    @Override
    public ContextCapture captureFragmentContext(String fragmentPath) throws Exception {
        LOG.info("Capturing visual context for fragment: {}", fragmentPath);

        String containingPagePath = findContainingPage(fragmentPath);
        if (containingPagePath != null) {
            return capturePageContext(containingPagePath, getSelectorForFragment(fragmentPath));
        }

        String url = generateContextUrl(fragmentPath) + ".html";
        byte[] screenshot = captureScreenshot(url);

        Map<String, String> metadata = new HashMap<>();
        metadata.put("url", url);
        metadata.put("fragmentPath", fragmentPath);
        metadata.put("containingPage", containingPagePath);

        ContextCapture capture = new ContextCapture(fragmentPath, "fragment", screenshot, metadata);
        captureCache.put(fragmentPath, capture);

        return capture;
    }

    @Override
    public ContextCapture captureAssetContext(String assetPath) throws Exception {
        LOG.info("Capturing visual context for asset: {}", assetPath);

        String url = generateAssetPreviewUrl(assetPath);
        byte[] preview = captureScreenshot(url);

        Map<String, String> metadata = new HashMap<>();
        metadata.put("url", url);
        metadata.put("assetPath", assetPath);
        metadata.put("previewType", "thumbnail");

        ContextCapture capture = new ContextCapture(assetPath, "asset", preview, metadata);
        captureCache.put(assetPath, capture);

        return capture;
    }

    @Override
    public ContextCapture captureWithPreview(String contentPath, ViewportConfig viewport) throws Exception {
        LOG.info("Capturing visual context for {} with viewport {}", contentPath, viewport.getName());
        
        String url = generatePreviewUrl(contentPath, viewport);
        byte[] screenshot = captureScreenshot(url);
        
        Map<String, String> metadata = new HashMap<>();
        metadata.put("url", url);
        metadata.put("contentPath", contentPath);
        metadata.put("viewport", viewport.getName());
        metadata.put("width", String.valueOf(viewport.getWidth()));
        metadata.put("height", String.valueOf(viewport.getHeight()));
        
        PreviewInfo previewInfo = new PreviewInfo(url, viewport.getName(), viewport.getName(), 
                viewport.getWidth(), viewport.getHeight());
        
        return new ContextCapture(contentPath, "page", screenshot, metadata, previewInfo, null);
    }

    @Override
    public String generatePreviewUrl(String contentPath, ViewportConfig viewport) {
        String host = System.getProperty("aem.publish.host", "http://localhost:4503");
        String path = contentPath;
        
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        
        return host + path + ".html";
    }

    @Override
    public java.util.List<ContextCapture> getCapturesForTranslation(String translationJobId) {
        return new ArrayList<>(captureCache.values());
    }

    @Override
    public String generateContextUrl(String contentPath) {
        String host = System.getProperty("aem.publish.host", "http://localhost:4503");
        String path = contentPath;
        
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        
        return host + path;
    }

    @Override
    public void cleanupOldCaptures(int maxAgeHours) {
        if (maxAgeHours <= 0) {
            maxAgeHours = 24;
        }

        long cutoffTime = System.currentTimeMillis() - (maxAgeHours * 60 * 60 * 1000L);

        captureCache.entrySet().removeIf(entry -> entry.getValue().getCapturedAt() < cutoffTime);

        LOG.info("Cleaned up old visual context captures");
    }

    private byte[] captureScreenshot(String url) throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(url);
            request.setHeader("User-Agent", "AEM-TranslateGemma-Connector/1.0");
            request.setHeader("Accept", "text/html");

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                
                if (statusCode == 200) {
                    String html = EntityUtils.toString(response.getEntity());
                    return generateMockScreenshot(url, html);
                } else {
                    LOG.warn("Failed to fetch URL for screenshot: {} - Status: {}", url, statusCode);
                    return generatePlaceholderImage("Page unavailable: " + statusCode);
                }
            }
        } catch (Exception e) {
            LOG.warn("Could not capture screenshot via HTTP: {}", e.getMessage());
            return generatePlaceholderImage("Preview not available");
        }
    }

    private byte[] generateMockScreenshot(String url, String html) throws IOException {
        BufferedImage image = new BufferedImage(SCREENSHOT_WIDTH, SCREENSHOT_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        g2d.setColor(new Color(245, 245, 245));
        g2d.fillRect(0, 0, SCREENSHOT_WIDTH, SCREENSHOT_HEIGHT);

        g2d.setColor(new Color(51, 51, 51));
        g2d.fillRect(0, 0, SCREENSHOT_WIDTH, 50);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.drawString("AEM Visual Context", 20, 30);

        g2d.setColor(new Color(100, 100, 100));
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        
        String displayUrl = url.length() > 60 ? url.substring(0, 60) + "..." : url;
        g2d.drawString(displayUrl, 20, 80);

        g2d.setColor(new Color(220, 220, 220));
        g2d.fillRect(20, 100, SCREENSHOT_WIDTH - 40, SCREENSHOT_HEIGHT - 150);

        g2d.setColor(new Color(80, 80, 80));
        g2d.setFont(new Font("Arial", Font.PLAIN, 11));
        
        String title = extractPageTitle(html);
        if (title != null) {
            g2d.drawString("Page: " + title, 30, 120);
        }
        
        g2d.drawString("Content length: " + html.length() + " characters", 30, 140);
        
        int wordCount = html.split("\\s+").length;
        g2d.drawString("Approximate words: " + wordCount, 30, 160);

        g2d.setColor(new Color(200, 200, 200));
        g2d.drawRect(20, 100, SCREENSHOT_WIDTH - 40, SCREENSHOT_HEIGHT - 150);

        g2d.setColor(new Color(0, 122, 204));
        g2d.fillRect(20, SCREENSHOT_HEIGHT - 40, 100, 25);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 11));
        g2d.drawString("Translate", 35, SCREENSHOT_HEIGHT - 23);

        g2d.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return baos.toByteArray();
    }

    private byte[] generatePlaceholderImage(String message) throws IOException {
        BufferedImage image = new BufferedImage(SCREENSHOT_WIDTH, SCREENSHOT_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        g2d.setColor(new Color(240, 240, 240));
        g2d.fillRect(0, 0, SCREENSHOT_WIDTH, SCREENSHOT_HEIGHT);

        g2d.setColor(new Color(150, 150, 150));
        g2d.setFont(new Font("Arial", Font.PLAIN, 16));
        
        int stringWidth = g2d.getFontMetrics().stringWidth(message);
        g2d.drawString(message, (SCREENSHOT_WIDTH - stringWidth) / 2, SCREENSHOT_HEIGHT / 2);

        g2d.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return baos.toByteArray();
    }

    private String extractPageTitle(String html) {
        Pattern titlePattern = Pattern.compile("<title>([^<]+)</title>", Pattern.CASE_INSENSITIVE);
        Matcher matcher = titlePattern.matcher(html);
        
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        
        Pattern h1Pattern = Pattern.compile("<h1[^>]*>([^<]+)</h1>", Pattern.CASE_INSENSITIVE);
        matcher = h1Pattern.matcher(html);
        
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        
        return null;
    }

    private String generateAssetPreviewUrl(String assetPath) {
        String host = System.getProperty("aem.publish.host", "http://localhost:4503");
        
        String previewPath = assetPath;
        
        if (!previewPath.startsWith("/")) {
            previewPath = "/" + previewPath;
        }
        
        int lastDot = previewPath.lastIndexOf('.');
        if (lastDot > 0) {
            previewPath = previewPath.substring(0, lastDot);
        }
        
        return host + previewPath + ".thumb.png";
    }

    private String findContainingPage(String fragmentPath) {
        if (fragmentPath == null) {
            return null;
        }

        int xfIndex = fragmentPath.indexOf("/experience-fragments/");
        if (xfIndex > 0) {
            String basePath = fragmentPath.substring(0, xfIndex);
            return basePath;
        }

        int cfIndex = fragmentPath.indexOf("/content-fragments/");
        if (cfIndex > 0) {
            String basePath = fragmentPath.substring(0, cfIndex);
            return basePath + "/jcr:content";
        }

        return null;
    }

    private String getSelectorForFragment(String fragmentPath) {
        String relativePath = fragmentPath;
        
        int xfIndex = relativePath.indexOf("/experience-fragments/");
        if (xfIndex > 0) {
            String pathAfterXF = relativePath.substring(xfIndex + "/experience-fragments/".length());
            String[] parts = pathAfterXF.split("/");
            if (parts.length >= 2) {
                return parts[1];
            }
        }
        
        return null;
    }
}
