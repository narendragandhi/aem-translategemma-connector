package com.example.aem.translation.visualcontext;

import java.util.Map;
import java.util.List;

public interface VisualContextService {

    class ContextCapture {
        private final String contentPath;
        private final String contentType;
        private final byte[] screenshot;
        private final Map<String, String> metadata;
        private final long capturedAt;
        private final PreviewInfo previewInfo;
        private final List<SegmentHighlight> highlights;

        public ContextCapture(String contentPath, String contentType, byte[] screenshot, 
                            Map<String, String> metadata) {
            this(contentPath, contentType, screenshot, metadata, null, null);
        }

        public ContextCapture(String contentPath, String contentType, byte[] screenshot, 
                            Map<String, String> metadata, PreviewInfo previewInfo,
                            List<SegmentHighlight> highlights) {
            this.contentPath = contentPath;
            this.contentType = contentType;
            this.screenshot = screenshot;
            this.metadata = metadata;
            this.capturedAt = System.currentTimeMillis();
            this.previewInfo = previewInfo;
            this.highlights = highlights;
        }

        public String getContentPath() { return contentPath; }
        public String getContentType() { return contentType; }
        public byte[] getScreenshot() { return screenshot; }
        public Map<String, String> getMetadata() { return metadata; }
        public long getCapturedAt() { return capturedAt; }
        public PreviewInfo getPreviewInfo() { return previewInfo; }
        public List<SegmentHighlight> getHighlights() { return highlights; }
    }

    class PreviewInfo {
        private final String previewUrl;
        private final String viewport;
        private final String device;
        private final int width;
        private final int height;

        public PreviewInfo(String previewUrl, String viewport, String device, int width, int height) {
            this.previewUrl = previewUrl;
            this.viewport = viewport;
            this.device = device;
            this.width = width;
            this.height = height;
        }

        public String getPreviewUrl() { return previewUrl; }
        public String getViewport() { return viewport; }
        public String getDevice() { return device; }
        public int getWidth() { return width; }
        public int getHeight() { return height; }
    }

    class SegmentHighlight {
        private final String segmentId;
        private final String text;
        private final String boundingBox;
        private final String source;

        public SegmentHighlight(String segmentId, String text, String boundingBox, String source) {
            this.segmentId = segmentId;
            this.text = text;
            this.boundingBox = boundingBox;
            this.source = source;
        }

        public String getSegmentId() { return segmentId; }
        public String getText() { return text; }
        public String getBoundingBox() { return boundingBox; }
        public String getSource() { return source; }
    }

    ContextCapture capturePageContext(String pagePath) throws Exception;

    ContextCapture capturePageContext(String pagePath, String selector) throws Exception;

    ContextCapture captureFragmentContext(String fragmentPath) throws Exception;

    ContextCapture captureAssetContext(String assetPath) throws Exception;

    ContextCapture captureWithPreview(String contentPath, ViewportConfig viewport) throws Exception;

    String generateContextUrl(String contentPath);

    String generatePreviewUrl(String contentPath, ViewportConfig viewport);

    void cleanupOldCaptures(int maxAgeHours);

    List<ContextCapture> getCapturesForTranslation(String translationJobId);

    class ViewportConfig {
        private final String name;
        private final int width;
        private final int height;
        private final boolean isMobile;
        private final String userAgent;

        public ViewportConfig(String name, int width, int height) {
            this(name, width, height, false, null);
        }

        public ViewportConfig(String name, int width, int height, boolean isMobile, String userAgent) {
            this.name = name;
            this.width = width;
            this.height = height;
            this.isMobile = isMobile;
            this.userAgent = userAgent;
        }

        public String getName() { return name; }
        public int getWidth() { return width; }
        public int getHeight() { return height; }
        public boolean isMobile() { return isMobile; }
        public String getUserAgent() { return userAgent; }

        public static ViewportConfig desktop() {
            return new ViewportConfig("Desktop", 1920, 1080);
        }

        public static ViewportConfig tablet() {
            return new ViewportConfig("Tablet", 1024, 768, true, null);
        }

        public static ViewportConfig mobile() {
            return new ViewportConfig("Mobile", 375, 812, true, "Mozilla/5.0 (iPhone; CPU iPhone OS 14_0 like Mac OS X)");
        }
    }
}
