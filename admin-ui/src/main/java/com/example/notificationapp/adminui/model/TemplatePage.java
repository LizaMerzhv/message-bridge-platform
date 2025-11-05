package com.example.notificationapp.adminui.model;

import java.util.List;

public record TemplatePage(
        List<TemplateSummary> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last) {

    public boolean hasContent() {
        return content != null && !content.isEmpty();
    }
}
