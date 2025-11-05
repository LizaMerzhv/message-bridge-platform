package com.example.notificationapp.adminui.model;

import java.util.Map;

public record ApiProblemAlert(
        String title, String detail, String instance, int status, Map<String, Object> properties, Map<String, String> headers) {

    public boolean isUnauthorized() {
        return status == 401;
    }

    public boolean isRateLimited() {
        return status == 429;
    }
}
