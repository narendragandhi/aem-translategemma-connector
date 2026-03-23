package com.adobe.granite.translation.api;

import com.adobe.granite.comments.Comment;

public interface TranslationState {
    String getName();
    TranslationConstants.TranslationStatus getStatus();
    String getDescription();
    Comment getComment();
    void setStatus(TranslationConstants.TranslationStatus status);
    void setComment(Comment c);
}
