package com.adobe.granite.comments;

import java.util.List;

public interface CommentCollection<C extends Comment> {
    List<C> getComments();
}
