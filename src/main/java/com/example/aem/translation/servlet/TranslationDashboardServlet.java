package com.example.aem.translation.servlet;

import com.example.aem.translation.job.TranslationJobManager;
import com.adobe.granite.translation.api.TranslationConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletPaths;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.*;

@Component(service = javax.servlet.Servlet.class)
@SlingServletPaths("/bin/translationgemma/dashboard")
public class TranslationDashboardServlet extends SlingAllMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(TranslationDashboardServlet.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Reference
    private TranslationJobManager jobManager;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String pathInfo = request.getPathInfo();

        try {
            if (pathInfo == null || pathInfo.equals("/") || pathInfo.equals("/stats")) {
                Map<String, Object> stats = jobManager.getDashboardStats();
                objectMapper.writeValue(response.getWriter(), stats);
                
            } else if (pathInfo.startsWith("/jobs")) {
                String jobId = request.getParameter("jobId");
                
                if (jobId != null) {
                    TranslationJobManager.JobInfo jobInfo = jobManager.getJobInfo(jobId);
                    if (jobInfo != null) {
                        objectMapper.writeValue(response.getWriter(), jobInfo);
                    } else {
                        response.setStatus(SlingHttpServletResponse.SC_NOT_FOUND);
                        objectMapper.writeValue(response.getWriter(), 
                            Map.of("error", "Job not found", "jobId", jobId));
                    }
                } else {
                    String statusParam = request.getParameter("status");
                    
                    if (statusParam != null) {
                        TranslationConstants.TranslationStatus status = 
                            TranslationConstants.TranslationStatus.valueOf(statusParam.toUpperCase());
                        TranslationJobManager.JobInfo[] jobs = jobManager.getJobsByStatus(status);
                        objectMapper.writeValue(response.getWriter(), jobs);
                    } else {
                        TranslationJobManager.JobInfo[] jobs = jobManager.getAllJobs();
                        objectMapper.writeValue(response.getWriter(), jobs);
                    }
                }
                
            } else {
                response.setStatus(SlingHttpServletResponse.SC_NOT_FOUND);
                objectMapper.writeValue(response.getWriter(), 
                    Map.of("error", "Unknown endpoint", "path", pathInfo));
            }
            
        } catch (Exception e) {
            LOG.error("Error processing dashboard request", e);
            response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            objectMapper.writeValue(response.getWriter(), 
                Map.of("error", e.getMessage()));
        }
    }

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String pathInfo = request.getPathInfo();

        try {
            if (pathInfo != null && pathInfo.startsWith("/jobs")) {
                String action = request.getParameter("action");
                
                if ("create".equals(action)) {
                    String name = request.getParameter("name");
                    String description = request.getParameter("description");
                    String sourceLanguage = request.getParameter("sourceLanguage");
                    String targetLanguage = request.getParameter("targetLanguage");
                    String dueDateStr = request.getParameter("dueDate");
                    
                    Date dueDate = null;
                    if (dueDateStr != null && !dueDateStr.isEmpty()) {
                        dueDate = objectMapper.readValue(dueDateStr, Date.class);
                    }
                    
                    String jobId = jobManager.createJob(
                        name, description, sourceLanguage, targetLanguage, dueDate, null
                    );
                    
                    objectMapper.writeValue(response.getWriter(), 
                        Map.of("jobId", jobId, "status", "created"));
                        
                } else if ("start".equals(action)) {
                    String jobId = request.getParameter("jobId");
                    jobManager.startJob(jobId);
                    objectMapper.writeValue(response.getWriter(), 
                        Map.of("jobId", jobId, "status", "started"));
                        
                } else if ("pause".equals(action)) {
                    String jobId = request.getParameter("jobId");
                    jobManager.pauseJob(jobId);
                    objectMapper.writeValue(response.getWriter(), 
                        Map.of("jobId", jobId, "status", "paused"));
                        
                } else if ("cancel".equals(action)) {
                    String jobId = request.getParameter("jobId");
                    jobManager.cancelJob(jobId);
                    objectMapper.writeValue(response.getWriter(), 
                        Map.of("jobId", jobId, "status", "cancelled"));
                        
                } else if ("addObjects".equals(action)) {
                    String jobId = request.getParameter("jobId");
                    String[] objects = request.getParameterValues("objects");
                    
                    if (objects != null) {
                        jobManager.addObjectsToJob(jobId, objects);
                        objectMapper.writeValue(response.getWriter(), 
                            Map.of("jobId", jobId, "addedCount", objects.length));
                    } else {
                        response.setStatus(SlingHttpServletResponse.SC_BAD_REQUEST);
                        objectMapper.writeValue(response.getWriter(), 
                            Map.of("error", "No objects provided"));
                    }
                } else {
                    response.setStatus(SlingHttpServletResponse.SC_BAD_REQUEST);
                    objectMapper.writeValue(response.getWriter(), 
                        Map.of("error", "Unknown action", "action", action));
                }
                
            } else {
                response.setStatus(SlingHttpServletResponse.SC_NOT_FOUND);
                objectMapper.writeValue(response.getWriter(), 
                    Map.of("error", "Unknown endpoint"));
            }
            
        } catch (Exception e) {
            LOG.error("Error processing dashboard request", e);
            response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            objectMapper.writeValue(response.getWriter(), 
                Map.of("error", e.getMessage()));
        }
    }

    @Override
    protected void doDelete(SlingHttpServletRequest request, SlingHttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try {
            String jobId = request.getParameter("jobId");
            
            if (jobId != null) {
                jobManager.deleteJob(jobId);
                objectMapper.writeValue(response.getWriter(), 
                    Map.of("jobId", jobId, "status", "deleted"));
            } else {
                response.setStatus(SlingHttpServletResponse.SC_BAD_REQUEST);
                objectMapper.writeValue(response.getWriter(), 
                    Map.of("error", "jobId parameter required"));
            }
            
        } catch (Exception e) {
            LOG.error("Error deleting job", e);
            response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            objectMapper.writeValue(response.getWriter(), 
                Map.of("error", e.getMessage()));
        }
    }
}
