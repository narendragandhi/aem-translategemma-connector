package com.example.aem.translation.sites.service.result;

import java.util.Date;

public class TranslationJobStatus {
    private final String jobId;
    private final String status;
    private final int totalPages;
    private final int completedPages;
    private final int totalAssets;
    private final int completedAssets;
    private final Date lastUpdated;

    public TranslationJobStatus(String jobId, String status, int totalPages, int completedPages,
                             int totalAssets, int completedAssets, Date lastUpdated) {
        this.jobId = jobId;
        this.status = status;
        this.totalPages = totalPages;
        this.completedPages = completedPages;
        this.totalAssets = totalAssets;
        this.completedAssets = completedAssets;
        this.lastUpdated = lastUpdated;
    }

    public String getJobId() { return jobId; }
    public String getStatus() { return status; }
    public int getTotalPages() { return totalPages; }
    public int getCompletedPages() { return completedPages; }
    public int getTotalAssets() { return totalAssets; }
    public int getCompletedAssets() { return completedAssets; }
    public Date getLastUpdated() { return lastUpdated; }

    public double getProgressPercentage() {
        int total = totalPages + totalAssets;
        int completed = completedPages + completedAssets;
        return total > 0 ? (double) completed / total * 100 : 0;
    }
}