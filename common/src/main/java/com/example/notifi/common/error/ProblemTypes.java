package com.example.notifi.common.error;

public final class ProblemTypes {
    private ProblemTypes() {}
    public static final String BAD_REQUEST = "/problems/bad-request";
    public static final String UNAUTHORIZED = "/problems/unauthorized";
    public static final String FORBIDDEN = "/problems/forbidden";
    public static final String NOT_FOUND = "/problems/not-found";
    public static final String CONFLICT = "/problems/conflict";
    public static final String UNPROCESSABLE = "/problems/unprocessable";
    public static final String TOO_MANY_REQUESTS = "/problems/too-many-requests";
    public static final String UNSUPPORTED_PARAMETER = "/problems/unsupported-parameter";
    public static final String INTERNAL = "/problems/internal";
}
