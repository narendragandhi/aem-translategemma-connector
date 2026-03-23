package com.example.aem.translation.continuous;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Component(service = ContinuousLocalizationService.class)
public class ContinuousLocalizationService {
    
    private static final Logger LOG = LoggerFactory.getLogger(ContinuousLocalizationService.class);
    
    private final Map<String, ContentWatch> watchedPaths = new ConcurrentHashMap<>();
    private final Map<String, TranslationJob> translationJobs = new ConcurrentHashMap<>();
    private final Map<String, WebhookConfig> webhooks = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    
    @Reference
    private com.example.aem.translation.service.TranslateGemmaTranslationService translationService;

    public ContinuousLocalizationService() {
        scheduler.scheduleAtFixedRate(this::checkForUpdates, 5, 5, TimeUnit.MINUTES);
        LOG.info("Continuous localization service started");
    }

    public void watchPath(String path, String sourceLanguage, String targetLanguage, WatchConfig config) {
        ContentWatch watch = new ContentWatch(path, sourceLanguage, targetLanguage, config);
        watchedPaths.put(path, watch);
        LOG.info("Watching path: {} for changes", path);
    }

    public void unwatchPath(String path) {
        watchedPaths.remove(path);
        LOG.info("Stopped watching path: {}", path);
    }

    public void registerWebhook(WebhookConfig webhook) {
        webhooks.put(webhook.getId(), webhook);
        LOG.info("Registered webhook: {} for event: {}", webhook.getId(), webhook.getEventType());
    }

    public void unregisterWebhook(String webhookId) {
        webhooks.remove(webhookId);
        LOG.info("Unregistered webhook: {}", webhookId);
    }

    public String triggerTranslation(ContentChange change) {
        String jobId = UUID.randomUUID().toString();
        
        TranslationJob job = new TranslationJob(jobId, change);
        translationJobs.put(jobId, job);
        
        processTranslationAsync(job);
        
        return jobId;
    }

    private void processTranslationAsync(TranslationJob job) {
        scheduler.submit(() -> {
            try {
                job.setStatus(JobStatus.IN_PROGRESS);
                triggerWebhooks(WebhookEvent.TRANSLATION_STARTED, job);
                
                String sourceText = extractContent(job.getChange().getPath());
                
                var result = translationService.translateString(
                    sourceText,
                    job.getChange().getSourceLanguage(),
                    job.getChange().getTargetLanguage(),
                    null,
                    "general"
                );
                
                applyTranslation(job.getChange().getPath(), result.getTranslation());
                
                job.setStatus(JobStatus.COMPLETED);
                job.setResult(result.getTranslation());
                triggerWebhooks(WebhookEvent.TRANSLATION_COMPLETED, job);
                
                LOG.info("Translation completed for path: {}", job.getChange().getPath());
                
            } catch (Exception e) {
                job.setStatus(JobStatus.FAILED);
                job.setError(e.getMessage());
                triggerWebhooks(WebhookEvent.TRANSLATION_FAILED, job);
                LOG.error("Translation failed for path: {}", job.getChange().getPath(), e);
            }
        });
    }

    private void checkForUpdates() {
        for (ContentWatch watch : watchedPaths.values()) {
            try {
                if (watch.hasChanges()) {
                    List<ContentChange> changes = watch.getChanges();
                    for (ContentChange change : changes) {
                        triggerTranslation(change);
                    }
                }
            } catch (Exception e) {
                LOG.error("Error checking for updates on path: {}", watch.getPath(), e);
            }
        }
    }

    private String extractContent(String path) {
        return "";
    }

    private void applyTranslation(String path, String translatedContent) {
        LOG.info("Applied translation to: {}", path);
    }

    private void triggerWebhooks(WebhookEvent event, TranslationJob job) {
        for (WebhookConfig webhook : webhooks.values()) {
            if (webhook.getEventType() == event) {
                try {
                    webhook.getCallback().accept(job);
                    LOG.debug("Triggered webhook: {} for event: {}", webhook.getId(), event);
                } catch (Exception e) {
                    LOG.error("Error triggering webhook: {}", webhook.getId(), e);
                }
            }
        }
    }

    public List<TranslationJob> getJobs(String path, JobStatus status) {
        return translationJobs.values().stream()
                .filter(job -> (path == null || job.getChange().getPath().equals(path)))
                .filter(job -> (status == null || job.getStatus() == status))
                .toList();
    }

    public TranslationJob getJob(String jobId) {
        return translationJobs.get(jobId);
    }

    public static class ContentWatch {
        private final String path;
        private final String sourceLanguage;
        private final String targetLanguage;
        private final WatchConfig config;
        private final List<ContentChange> changes = new ArrayList<>();
        private long lastCheck;

        public ContentWatch(String path, String sourceLanguage, String targetLanguage, WatchConfig config) {
            this.path = path;
            this.sourceLanguage = sourceLanguage;
            this.targetLanguage = targetLanguage;
            this.config = config;
            this.lastCheck = System.currentTimeMillis();
        }

        public String getPath() { return path; }
        public String getSourceLanguage() { return sourceLanguage; }
        public String getTargetLanguage() { return targetLanguage; }
        public WatchConfig getConfig() { return config; }
        
        public boolean hasChanges() {
            return !changes.isEmpty();
        }
        
        public List<ContentChange> getChanges() {
            List<ContentChange> result = new ArrayList<>(changes);
            changes.clear();
            return result;
        }
    }

    public static class ContentChange {
        private final String path;
        private final String sourceLanguage;
        private final String targetLanguage;
        private final ChangeType changeType;
        private final String changedBy;
        private final Date timestamp;

        public ContentChange(String path, String sourceLanguage, String targetLanguage, 
                ChangeType changeType, String changedBy) {
            this.path = path;
            this.sourceLanguage = sourceLanguage;
            this.targetLanguage = targetLanguage;
            this.changeType = changeType;
            this.changedBy = changedBy;
            this.timestamp = new Date();
        }

        public String getPath() { return path; }
        public String getSourceLanguage() { return sourceLanguage; }
        public String getTargetLanguage() { return targetLanguage; }
        public ChangeType getChangeType() { return changeType; }
        public String getChangedBy() { return changedBy; }
        public Date getTimestamp() { return timestamp; }
    }

    public static class WatchConfig {
        private final boolean watchSubtree;
        private final Set<String> includePatterns;
        private final Set<String> excludePatterns;
        private final boolean autoTranslate;

        public WatchConfig(boolean watchSubtree, Set<String> includePatterns, 
                Set<String> excludePatterns, boolean autoTranslate) {
            this.watchSubtree = watchSubtree;
            this.includePatterns = includePatterns;
            this.excludePatterns = excludePatterns;
            this.autoTranslate = autoTranslate;
        }

        public boolean isWatchSubtree() { return watchSubtree; }
        public Set<String> getIncludePatterns() { return includePatterns; }
        public Set<String> getExcludePatterns() { return excludePatterns; }
        public boolean isAutoTranslate() { return autoTranslate; }

        public static class Builder {
            private boolean watchSubtree = true;
            private Set<String> includePatterns = new HashSet<>(Arrays.asList("*"));
            private Set<String> excludePatterns = new HashSet<>();
            private boolean autoTranslate = true;

            public Builder watchSubtree(boolean watchSubtree) {
                this.watchSubtree = watchSubtree;
                return this;
            }

            public Builder includePatterns(Set<String> patterns) {
                this.includePatterns = patterns;
                return this;
            }

            public Builder excludePatterns(Set<String> patterns) {
                this.excludePatterns = patterns;
                return this;
            }

            public Builder autoTranslate(boolean autoTranslate) {
                this.autoTranslate = autoTranslate;
                return this;
            }

            public WatchConfig build() {
                return new WatchConfig(watchSubtree, includePatterns, excludePatterns, autoTranslate);
            }
        }
    }

    public static class WebhookConfig {
        private final String id;
        private final WebhookEvent eventType;
        private final String url;
        private final Map<String, String> headers;
        private final Consumer<TranslationJob> callback;
        private final boolean enabled;

        public WebhookConfig(String id, WebhookEvent eventType, String url, 
                Map<String, String> headers, Consumer<TranslationJob> callback) {
            this.id = id;
            this.eventType = eventType;
            this.url = url;
            this.headers = headers;
            this.callback = callback;
            this.enabled = true;
        }

        public String getId() { return id; }
        public WebhookEvent getEventType() { return eventType; }
        public String getUrl() { return url; }
        public Map<String, String> getHeaders() { return headers; }
        public Consumer<TranslationJob> getCallback() { return callback; }
        public boolean isEnabled() { return enabled; }
    }

    public static class TranslationJob {
        private final String jobId;
        private final ContentChange change;
        private volatile JobStatus status;
        private volatile String result;
        private volatile String error;
        private final Date createdAt;
        private volatile Date completedAt;

        public TranslationJob(String jobId, ContentChange change) {
            this.jobId = jobId;
            this.change = change;
            this.status = JobStatus.PENDING;
            this.createdAt = new Date();
        }

        public String getJobId() { return jobId; }
        public ContentChange getChange() { return change; }
        public JobStatus getStatus() { return status; }
        public void setStatus(JobStatus status) { this.status = status; }
        public String getResult() { return result; }
        public void setResult(String result) { this.result = result; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        public Date getCreatedAt() { return createdAt; }
        public Date getCompletedAt() { return completedAt; }
    }

    public enum ChangeType {
        CREATED, MODIFIED, DELETED, PUBLISHED
    }

    public enum WebhookEvent {
        TRANSLATION_STARTED,
        TRANSLATION_COMPLETED,
        TRANSLATION_FAILED,
        CONTENT_CHANGED
    }

    public enum JobStatus {
        PENDING, IN_PROGRESS, COMPLETED, FAILED, CANCELLED
    }
}
