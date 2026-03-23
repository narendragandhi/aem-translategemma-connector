package com.example.aem.translation.job;

import com.adobe.granite.translation.api.TranslationConstants;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface TranslationJobManager {

    class JobInfo {
        private final String jobId;
        private final String name;
        private final String description;
        private final String sourceLanguage;
        private final String targetLanguage;
        private final TranslationConstants.TranslationStatus status;
        private final Date createdAt;
        private final Date updatedAt;
        private final Date dueDate;
        private final int totalObjects;
        private final int completedObjects;
        private final int failedObjects;
        private final List<String> objectPaths;
        private final Map<String, String> metadata;
        private final String createdBy;

        public JobInfo(String jobId, String name, String description, String sourceLanguage,
                     String targetLanguage, TranslationConstants.TranslationStatus status,
                     Date createdAt, Date updatedAt, Date dueDate, int totalObjects,
                     int completedObjects, int failedObjects, List<String> objectPaths,
                     Map<String, String> metadata, String createdBy) {
            this.jobId = jobId;
            this.name = name;
            this.description = description;
            this.sourceLanguage = sourceLanguage;
            this.targetLanguage = targetLanguage;
            this.status = status;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
            this.dueDate = dueDate;
            this.totalObjects = totalObjects;
            this.completedObjects = completedObjects;
            this.failedObjects = failedObjects;
            this.objectPaths = objectPaths;
            this.metadata = metadata;
            this.createdBy = createdBy;
        }

        public String getJobId() { return jobId; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public String getSourceLanguage() { return sourceLanguage; }
        public String getTargetLanguage() { return targetLanguage; }
        public TranslationConstants.TranslationStatus getStatus() { return status; }
        public Date getCreatedAt() { return createdAt; }
        public Date getUpdatedAt() { return updatedAt; }
        public Date getDueDate() { return dueDate; }
        public int getTotalObjects() { return totalObjects; }
        public int getCompletedObjects() { return completedObjects; }
        public int getFailedObjects() { return failedObjects; }
        public List<String> getObjectPaths() { return objectPaths; }
        public Map<String, String> getMetadata() { return metadata; }
        public String getCreatedBy() { return createdBy; }

        public double getProgressPercentage() {
            if (totalObjects == 0) return 0;
            return (double) completedObjects / totalObjects * 100;
        }
    }

    String createJob(String name, String description, String sourceLanguage, String targetLanguage,
                   Date dueDate, Map<String, String> metadata) throws Exception;

    void addObjectsToJob(String jobId, String[] objectPaths) throws Exception;

    void startJob(String jobId) throws Exception;

    void pauseJob(String jobId) throws Exception;

    void cancelJob(String jobId) throws Exception;

    JobInfo getJobInfo(String jobId) throws Exception;

    JobInfo[] getAllJobs() throws Exception;

    JobInfo[] getJobsByStatus(TranslationConstants.TranslationStatus status) throws Exception;

    void updateJobDueDate(String jobId, Date dueDate) throws Exception;

    void deleteJob(String jobId) throws Exception;

    Map<String, Object> getDashboardStats() throws Exception;
}
