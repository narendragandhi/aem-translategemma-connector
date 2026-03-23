package com.example.aem.translation.mock;

import com.adobe.granite.comments.Comment;
import com.adobe.granite.translation.api.TranslationConstants;
import com.adobe.granite.translation.api.TranslationState;

public class MockTranslationState implements TranslationState {

    private String name;
    private TranslationConstants.TranslationStatus status;
    private String description;
    private Comment comment;

    public MockTranslationState(TranslationConstants.TranslationStatus status) {
        this.status = status;
        this.name = status.name();
        this.description = "Mock state for " + status.name();
    }

    public MockTranslationState(TranslationConstants.TranslationStatus status, String name, String description) {
        this.status = status;
        this.name = name;
        this.description = description;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public TranslationConstants.TranslationStatus getStatus() {
        return status;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public Comment getComment() {
        return comment;
    }

    @Override
    public void setStatus(TranslationConstants.TranslationStatus status) {
        this.status = status;
        this.name = status.name();
        this.description = "Mock state for " + status.name();
    }

    @Override
    public void setComment(Comment c) {
        this.comment = c;
    }
}
