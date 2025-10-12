package com.example.notifi.api.web.error;

import com.example.notifi.api.core.notification.exception.NotificationNotFoundException;
import com.example.notifi.api.core.notification.exception.SendAtWindowException;
import com.example.notifi.api.core.template.exception.TemplateCodeNotFoundException;
import com.example.notifi.api.core.template.exception.TemplateInactiveException;
import com.example.notifi.api.core.template.exception.TemplateNotFoundException;
import com.example.notifi.common.error.ProblemDetails;
import com.example.notifi.common.error.Problems;
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
        // Единый источник traceId во всех проблемах
        return MDC.get("traceId");
    }

    // 400: ошибки валидации тела/биндинга
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<ProblemDetails> handleValidationErrors(Exception ex, HttpServletRequest req) {
        String detail = aggregateErrors(ex);
        ProblemDetails body = Problems.badRequest(detail, req.getRequestURI(), traceId());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .contentType(APPLICATION_PROBLEM_JSON)
            .body(body);
    }

    // 400: ошибки ConstraintValidator
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetails> handleConstraintViolation(ConstraintViolationException ex,
                                                                    HttpServletRequest req) {
        ProblemDetails body = Problems.badRequest("Constraint violation: " + ex.getMessage(),
            req.getRequestURI(), traceId());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .contentType(APPLICATION_PROBLEM_JSON)
            .body(body);
    }

    // 400: неверный JSON/формат
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetails> handleUnreadable(HttpMessageNotReadableException ex,
                                                           HttpServletRequest req) {
        ProblemDetails body = Problems.badRequest("Malformed request", req.getRequestURI(), traceId());
        return ResponseEntity.badRequest()
            .contentType(APPLICATION_PROBLEM_JSON)
            .body(body);
    }

    // 400: отсутствует обязательный параметр
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ProblemDetails> handleMissingParam(MissingServletRequestParameterException ex,
                                                             HttpServletRequest req) {
        ProblemDetails body = Problems.badRequest("Missing parameter: " + ex.getParameterName(),
            req.getRequestURI(), traceId());
        return ResponseEntity.badRequest()
            .contentType(APPLICATION_PROBLEM_JSON)
            .body(body);
    }

    // 404: уведомление не найдено (специализированный type)
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

    // 404: шаблон не найден (специализированный type)
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

    // 422: код шаблона отсутствует
    @ExceptionHandler(TemplateCodeNotFoundException.class)
    public ResponseEntity<ProblemDetails> handleTemplateCodeNotFound(TemplateCodeNotFoundException ex,
                                                                     HttpServletRequest req) {
        ProblemDetails body = Problems.of(
            422,
            "/problems/template-not-found",
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

    // 422: шаблон INACTIVE
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

    // 422: окно sendAt нарушено
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

    // 409: БД-конфликты (не про идемпотентность)
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

    // 400: прочие бизнес-ошибки
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetails> handleBadRequest(IllegalArgumentException ex,
                                                           HttpServletRequest req) {
        ProblemDetails body = Problems.badRequest(ex.getMessage(), req.getRequestURI(), traceId());
        return ResponseEntity.badRequest()
            .contentType(APPLICATION_PROBLEM_JSON)
            .body(body);
    }

    // 500: fallback
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
