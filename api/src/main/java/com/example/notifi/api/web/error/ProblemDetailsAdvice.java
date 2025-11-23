package com.example.notifi.api.web.error;

import com.example.notifi.api.core.notification.exceptions.NotificationNotFoundException;
import com.example.notifi.api.core.notification.exceptions.SendAtWindowException;
import com.example.notifi.api.core.template.exceptions.TemplateCodeNotFoundException;
import com.example.notifi.api.core.template.exceptions.TemplateInactiveException;
import com.example.notifi.api.core.template.exceptions.TemplateNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON;

@RestControllerAdvice
public class ProblemDetailsAdvice {

    private static String traceId() {
        return MDC.get("traceId");
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<ProblemDetails> handleValidationErrors(Exception ex, HttpServletRequest req) {
        String detail = aggregateErrors(ex);
        ProblemDetails body = Problems.badRequest(detail, req.getRequestURI(), traceId());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .contentType(APPLICATION_PROBLEM_JSON)
            .body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetails> handleConstraintViolation(ConstraintViolationException ex,
                                                                    HttpServletRequest req) {
        ProblemDetails body = Problems.badRequest("Constraint violation: " + ex.getMessage(),
            req.getRequestURI(), traceId());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .contentType(APPLICATION_PROBLEM_JSON)
            .body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetails> handleUnreadable(HttpMessageNotReadableException ex,
                                                           HttpServletRequest req) {
        ProblemDetails body = Problems.badRequest("Malformed request", req.getRequestURI(), traceId());
        return ResponseEntity.badRequest()
            .contentType(APPLICATION_PROBLEM_JSON)
            .body(body);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ProblemDetails> handleMissingParam(MissingServletRequestParameterException ex,
                                                             HttpServletRequest req) {
        ProblemDetails body = Problems.badRequest("Missing parameter: " + ex.getParameterName(),
            req.getRequestURI(), traceId());
        return ResponseEntity.badRequest()
            .contentType(APPLICATION_PROBLEM_JSON)
            .body(body);
    }

    @ExceptionHandler(NotificationNotFoundException.class)
    public ResponseEntity<ProblemDetails> handleNotificationNotFound(NotificationNotFoundException ex,
                                                                     HttpServletRequest req) {
        ProblemDetails body = Problems.of(
            404,
            "/problems/notification-not-found",
            "Not Found",
            ex.getMessage(),
            req.getRequestURI(),
            traceId(),
            null,
            null
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .contentType(APPLICATION_PROBLEM_JSON)
            .body(body);
    }

    @ExceptionHandler(TemplateNotFoundException.class)
    public ResponseEntity<ProblemDetails> handleTemplateNotFound(TemplateNotFoundException ex,
                                                                 HttpServletRequest req) {
        ProblemDetails body = Problems.of(
            404,
            "/problems/template-not-found",
            "Not Found",
            ex.getMessage(),
            req.getRequestURI(),
            traceId(),
            null,
            null
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .contentType(APPLICATION_PROBLEM_JSON)
            .body(body);
    }

    @ExceptionHandler(TemplateCodeNotFoundException.class)
    public ResponseEntity<ProblemDetails> handleTemplateCodeNotFound(
        TemplateCodeNotFoundException ex,
        HttpServletRequest req
    ) {
        ProblemDetails body = Problems.notFound(
            ex.getMessage(),
            req.getRequestURI(),
            traceId()
        );

        return ResponseEntity
            .status(404)
            .contentType(APPLICATION_PROBLEM_JSON)
            .body(body);
    }


    @ExceptionHandler(TemplateInactiveException.class)
    public ResponseEntity<ProblemDetails> handleTemplateInactive(TemplateInactiveException ex,
                                                                 HttpServletRequest req) {
        ProblemDetails body = Problems.of(
            422,
            "/problems/template-inactive",
            "Unprocessable Entity",
            ex.getMessage(),
            req.getRequestURI(),
            traceId(),
            null,
            null
        );
        return ResponseEntity.unprocessableEntity()
            .contentType(APPLICATION_PROBLEM_JSON)
            .body(body);
    }

    @ExceptionHandler(SendAtWindowException.class)
    public ResponseEntity<ProblemDetails> handleSendAt(SendAtWindowException ex,
                                                       HttpServletRequest req) {
        ProblemDetails body = Problems.of(
            422,
            "/problems/invalid-send-at",
            "Unprocessable Entity",
            ex.getMessage(),
            req.getRequestURI(),
            traceId(),
            null,
            null
        );
        return ResponseEntity.unprocessableEntity()
            .contentType(APPLICATION_PROBLEM_JSON)
            .body(body);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetails> handleConflict(DataIntegrityViolationException ex,
                                                         HttpServletRequest req) {
        String detail = ex.getMostSpecificCause() != null
            ? ex.getMostSpecificCause().getMessage()
            : "Conflict";
        ProblemDetails body = Problems.conflict(detail, req.getRequestURI(), traceId());
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .contentType(APPLICATION_PROBLEM_JSON)
            .body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetails> handleBadRequest(IllegalArgumentException ex,
                                                           HttpServletRequest req) {
        ProblemDetails body = Problems.badRequest(ex.getMessage(), req.getRequestURI(), traceId());
        return ResponseEntity.badRequest()
            .contentType(APPLICATION_PROBLEM_JSON)
            .body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetails> handleInternal(Exception ex, HttpServletRequest req) {
        ProblemDetails body = Problems.internal("Internal error", req.getRequestURI(), traceId());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .contentType(APPLICATION_PROBLEM_JSON)
            .body(body);
    }

    // ------- helpers -------

    private String aggregateErrors(Exception ex) {
        if (ex instanceof MethodArgumentNotValidException manve) {
            return manve.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.toList())
                .toString();
        }
        if (ex instanceof BindException bind) {
            return bind.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.toList())
                .toString();
        }
        return ex.getMessage();
    }
}
