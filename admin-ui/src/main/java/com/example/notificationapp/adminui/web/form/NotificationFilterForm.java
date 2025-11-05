package com.example.notificationapp.adminui.web.form;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

public class NotificationFilterForm {

    private static final DateTimeFormatter DATE_TIME_LOCAL_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private String status;
    private String to;
    private String externalRequestId;
    private String createdFrom;
    private String createdTo;
    private Integer page = 0;
    private Integer size = 20;
    private String sort = "createdAt,desc";

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getExternalRequestId() {
        return externalRequestId;
    }

    public void setExternalRequestId(String externalRequestId) {
        this.externalRequestId = externalRequestId;
    }

    public String getCreatedFrom() {
        return createdFrom;
    }

    public void setCreatedFrom(String createdFrom) {
        this.createdFrom = createdFrom;
    }

    public String getCreatedTo() {
        return createdTo;
    }

    public void setCreatedTo(String createdTo) {
        this.createdTo = createdTo;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }

    public MultiValueMap<String, String> toQueryParams() {
        LinkedMultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        add(params, "status", status);
        add(params, "to", to);
        add(params, "externalRequestId", externalRequestId);
        convertToUtc(createdFrom).ifPresent(value -> add(params, "createdFrom", value));
        convertToUtc(createdTo).ifPresent(value -> add(params, "createdTo", value));
        if (page != null) {
            params.add("page", String.valueOf(page));
        }
        if (size != null) {
            params.add("size", String.valueOf(size));
        }
        add(params, "sort", sort);
        return params;
    }

    public Optional<String> createdFromUtcPreview() {
        return convertToUtc(createdFrom);
    }

    public Optional<String> createdToUtcPreview() {
        return convertToUtc(createdTo);
    }

    private void add(LinkedMultiValueMap<String, String> params, String key, String value) {
        if (StringUtils.hasText(value)) {
            params.add(key, value.trim());
        }
    }

    private Optional<String> convertToUtc(String value) {
        if (!StringUtils.hasText(value)) {
            return Optional.empty();
        }
        String trimmed = value.trim();
        try {
            OffsetDateTime offsetDateTime = OffsetDateTime.parse(trimmed);
            return Optional.of(offsetDateTime.withOffsetSameInstant(ZoneOffset.UTC).toString());
        } catch (DateTimeParseException ignored) {
        }
        try {
            LocalDateTime localDateTime = LocalDateTime.parse(trimmed, DATE_TIME_LOCAL_FORMATTER);
            return Optional.of(localDateTime.atOffset(ZoneOffset.UTC).toString());
        } catch (DateTimeParseException ignored) {
            return Optional.empty();
        }
    }
}
