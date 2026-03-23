package com.example.aem.translation.job.impl;

import com.example.aem.translation.job.TranslationJobManager;
import com.adobe.granite.translation.api.TranslationConstants;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component(
    service = TranslationJobManager.class,
    immediate = true
)
public class TranslationJobManagerImpl implements TranslationJobManager {

    private static final Logger LOG = LoggerFactory.getLogger(TranslationJobManagerImpl.class);

    private final Map<String, JobInfo> jobs = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public TranslationJobManagerImpl() {
        scheduler.scheduleAtFixedRate(this::cleanupOldJobs, 1, 1, TimeUnit.HOURS);
    }

    @Override
    public String createJob(String name, String description, String sourceLanguage, String targetLanguage,
                          Date dueDate, Map<String, String> metadata) throws Exception {
        String jobId = UUID.randomUUID().toString();
        
        JobInfo jobInfo = new JobInfo(
            jobId,
            name,
            description,
            sourceLanguage,
            targetLanguage,
            TranslationConstants.TranslationStatus.DRAFT,
            new Date(),
            new Date(),
            dueDate,
            0, 0, 0,
            new ArrayList<>(),
            metadata != null ? metadata : new HashMap<>(),
            "system"
        );

        jobs.put(jobId, jobInfo);

        LOG.info("Created translation job: {} ({})", name, jobId);
        
        return jobId;
    }

    @Override
    public void addObjectsToJob(String jobId, String[] objectPaths) throws Exception {
        JobInfo jobInfo = jobs.get(jobId);
        if (jobInfo == null) {
            throw new IllegalArgumentException("Job not found: " + jobId);
        }

        if (jobInfo.getStatus() != TranslationConstants.TranslationStatus.DRAFT) {
            throw new IllegalStateException("Cannot add objects to job in status: " + jobInfo.getStatus());
        }

        List<String> currentPaths = new ArrayList<>(jobInfo.getObjectPaths());
        currentPaths.addAll(Arrays.asList(objectPaths));

        JobInfo updatedJob = new JobInfo(
            jobInfo.getJobId(),
            jobInfo.getName(),
            jobInfo.getDescription(),
            jobInfo.getSourceLanguage(),
            jobInfo.getTargetLanguage(),
            jobInfo.getStatus(),
            jobInfo.getCreatedAt(),
            new Date(),
            jobInfo.getDueDate(),
            currentPaths.size(),
            0,
            0,
            currentPaths,
            jobInfo.getMetadata(),
            jobInfo.getCreatedBy()
        );

        jobs.put(jobId, updatedJob);
        
        LOG.info("Added {} objects to job: {}", objectPaths.length, jobId);
    }

    @Override
    public void startJob(String jobId) throws Exception {
        JobInfo jobInfo = jobs.get(jobId);
        if (jobInfo == null) {
            throw new IllegalArgumentException("Job not found: " + jobId);
        }

        if (jobInfo.getTotalObjects() == 0) {
            throw new IllegalStateException("Job has no objects to translate");
        }

        JobInfo updatedJob = new JobInfo(
            jobInfo.getJobId(),
            jobInfo.getName(),
            jobInfo.getDescription(),
            jobInfo.getSourceLanguage(),
            jobInfo.getTargetLanguage(),
            TranslationConstants.TranslationStatus.TRANSLATION_IN_PROGRESS,
            jobInfo.getCreatedAt(),
            new Date(),
            jobInfo.getDueDate(),
            jobInfo.getTotalObjects(),
            0,
            0,
            jobInfo.getObjectPaths(),
            jobInfo.getMetadata(),
            jobInfo.getCreatedBy()
        );

        jobs.put(jobId, updatedJob);
        
        LOG.info("Started translation job: {}", jobId);
    }

    @Override
    public void pauseJob(String jobId) throws Exception {
        JobInfo jobInfo = jobs.get(jobId);
        if (jobInfo == null) {
            throw new IllegalArgumentException("Job not found: " + jobId);
        }

        if (jobInfo.getStatus() != TranslationConstants.TranslationStatus.TRANSLATION_IN_PROGRESS) {
            throw new IllegalStateException("Cannot pause job in status: " + jobInfo.getStatus());
        }

        JobInfo updatedJob = new JobInfo(
            jobInfo.getJobId(),
            jobInfo.getName(),
            jobInfo.getDescription(),
            jobInfo.getSourceLanguage(),
            jobInfo.getTargetLanguage(),
            TranslationConstants.TranslationStatus.DRAFT,
            jobInfo.getCreatedAt(),
            new Date(),
            jobInfo.getDueDate(),
            jobInfo.getTotalObjects(),
            jobInfo.getCompletedObjects(),
            jobInfo.getFailedObjects(),
            jobInfo.getObjectPaths(),
            jobInfo.getMetadata(),
            jobInfo.getCreatedBy()
        );

        jobs.put(jobId, updatedJob);
        
        LOG.info("Paused translation job: {}", jobId);
    }

    @Override
    public void cancelJob(String jobId) throws Exception {
        JobInfo jobInfo = jobs.get(jobId);
        if (jobInfo == null) {
            throw new IllegalArgumentException("Job not found: " + jobId);
        }

        JobInfo updatedJob = new JobInfo(
            jobInfo.getJobId(),
            jobInfo.getName(),
            jobInfo.getDescription(),
            jobInfo.getSourceLanguage(),
            jobInfo.getTargetLanguage(),
            TranslationConstants.TranslationStatus.CANCEL,
            jobInfo.getCreatedAt(),
            new Date(),
            jobInfo.getDueDate(),
            jobInfo.getTotalObjects(),
            jobInfo.getCompletedObjects(),
            jobInfo.getFailedObjects(),
            jobInfo.getObjectPaths(),
            jobInfo.getMetadata(),
            jobInfo.getCreatedBy()
        );

        jobs.put(jobId, updatedJob);
        
        LOG.info("Cancelled translation job: {}", jobId);
    }

    @Override
    public JobInfo getJobInfo(String jobId) throws Exception {
        return jobs.get(jobId);
    }

    @Override
    public JobInfo[] getAllJobs() throws Exception {
        return jobs.values().toArray(new JobInfo[0]);
    }

    @Override
    public JobInfo[] getJobsByStatus(TranslationConstants.TranslationStatus status) throws Exception {
        return jobs.values().stream()
            .filter(job -> job.getStatus() == status)
            .collect(Collectors.toList())
            .toArray(new JobInfo[0]);
    }

    @Override
    public void updateJobDueDate(String jobId, Date dueDate) throws Exception {
        JobInfo jobInfo = jobs.get(jobId);
        if (jobInfo == null) {
            throw new IllegalArgumentException("Job not found: " + jobId);
        }

        JobInfo updatedJob = new JobInfo(
            jobInfo.getJobId(),
            jobInfo.getName(),
            jobInfo.getDescription(),
            jobInfo.getSourceLanguage(),
            jobInfo.getTargetLanguage(),
            jobInfo.getStatus(),
            jobInfo.getCreatedAt(),
            new Date(),
            dueDate,
            jobInfo.getTotalObjects(),
            jobInfo.getCompletedObjects(),
            jobInfo.getFailedObjects(),
            jobInfo.getObjectPaths(),
            jobInfo.getMetadata(),
            jobInfo.getCreatedBy()
        );

        jobs.put(jobId, updatedJob);
        
        LOG.info("Updated due date for job: {} to {}", jobId, dueDate);
    }

    @Override
    public void deleteJob(String jobId) throws Exception {
        if (jobs.remove(jobId) != null) {
            LOG.info("Deleted translation job: {}", jobId);
        }
    }

    @Override
    public Map<String, Object> getDashboardStats() throws Exception {
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalJobs", jobs.size());
        
        long draftCount = jobs.values().stream()
            .filter(j -> j.getStatus() == TranslationConstants.TranslationStatus.DRAFT)
            .count();
        stats.put("draftJobs", draftCount);

        long inProgressCount = jobs.values().stream()
            .filter(j -> j.getStatus() == TranslationConstants.TranslationStatus.TRANSLATION_IN_PROGRESS)
            .count();
        stats.put("inProgressJobs", inProgressCount);

        long completedCount = jobs.values().stream()
            .filter(j -> j.getStatus() == TranslationConstants.TranslationStatus.TRANSLATED)
            .count();
        stats.put("completedJobs", completedCount);

        long failedCount = jobs.values().stream()
            .filter(j -> j.getStatus() == TranslationConstants.TranslationStatus.ERROR_UPDATE)
            .count();
        stats.put("failedJobs", failedCount);

        int totalObjects = jobs.values().stream()
            .mapToInt(JobInfo::getTotalObjects)
            .sum();
        stats.put("totalObjectsToTranslate", totalObjects);

        int completedObjects = jobs.values().stream()
            .mapToInt(JobInfo::getCompletedObjects)
            .sum();
        stats.put("totalObjectsCompleted", completedObjects);

        List<Map<String, Object>> recentJobs = jobs.values().stream()
            .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
            .limit(5)
            .map(job -> {
                Map<String, Object> jobMap = new HashMap<>();
                jobMap.put("jobId", job.getJobId());
                jobMap.put("name", job.getName());
                jobMap.put("status", job.getStatus().name());
                jobMap.put("progress", job.getProgressPercentage());
                jobMap.put("createdAt", job.getCreatedAt());
                return jobMap;
            })
            .collect(Collectors.toList());
        stats.put("recentJobs", recentJobs);

        stats.put("timestamp", new Date());

        return stats;
    }

    private void cleanupOldJobs() {
        long cutoffTime = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000);
        
        jobs.entrySet().removeIf(entry -> {
            JobInfo job = entry.getValue();
            return job.getCreatedAt().getTime() < cutoffTime && 
                   (job.getStatus() == TranslationConstants.TranslationStatus.TRANSLATED ||
                    job.getStatus() == TranslationConstants.TranslationStatus.CANCEL);
        });
        
        LOG.info("Cleaned up old translation jobs");
    }
}
