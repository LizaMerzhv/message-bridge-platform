package com.example.notificationapp.adminui.api;

import com.example.notificationapp.adminui.model.ApiProblemAlert;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ProblemDetail;
import org.springframework.util.CollectionUtils;

public final class ApiProblemMapper {

    private ApiProblemMapper() {}

    public static ApiProblemAlert toAlert(ApiProblemException exception) {
        ProblemDetail problem = exception.problem();
        Map<String, Object> properties = new LinkedHashMap<>();
        if (problem != null && problem.getProperties() != null) {
            properties.putAll(problem.getProperties());
        }
        Map<String, String> headers = new LinkedHashMap<>();
        if (!CollectionUtils.isEmpty(exception.headerSnapshot())) {
            headers.putAll(exception.headerSnapshot());
        }
        return new ApiProblemAlert(
                problem != null ? problem.getTitle() : null,
                problem != null ? problem.getDetail() : null,
                problem != null && problem.getInstance() != null ? problem.getInstance().toString() : null,
                exception.statusCode() != null ? exception.statusCode().value() : 0,
                properties,
                headers);
    }
}
