package com.example.notifi.api.security;

import com.example.notifi.api.data.entity.ClientEntity;
import com.example.notifi.api.data.repository.ClientRepository;
import com.example.notifi.api.web.error.ProblemDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import static com.example.notifi.api.web.error.Problems.unauthorized;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;

public class ApiKeyAuthFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-API-Key";

    private final ClientRepository clientRepository;
    private final ObjectMapper objectMapper;

    public ApiKeyAuthFilter(ClientRepository clientRepository, ObjectMapper objectMapper) {
        this.clientRepository = clientRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 1) Забираем API-ключ
        final String apiKey = request.getHeader(HEADER);
        if (apiKey == null || apiKey.isBlank()) {
            writeUnauthorized(request, response, "Missing or invalid API key");
            return;
        }

        // 2) Ищем клиента по ключу
        // ПРИМЕЧАНИЕ: если у репозитория другой метод — поправьте ниже.
        final Optional<ClientEntity> clientOpt = clientRepository.findByApiKey(apiKey);
        if (clientOpt.isEmpty()) {
            writeUnauthorized(request, response, "Missing or invalid API key");
            return;
        }

        // 3) Устанавливаем аутентификацию в контекст
        final ClientEntity client = clientOpt.get();
        final ClientPrincipal principal =
            new ClientPrincipal(client.getId(), client.getName(), client.getRateLimitPerMin());

        final ClientAuthenticationToken authentication = new ClientAuthenticationToken(principal);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        request.setAttribute(ClientPrincipal.class.getName(), principal);

        try {
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        final String path = request.getRequestURI();
        return path.startsWith("/v3/api-docs")
            || path.startsWith("/swagger-ui")
            || path.startsWith("/actuator");
    }

    private void writeUnauthorized(HttpServletRequest request,
                                   HttpServletResponse response,
                                   String detail) throws IOException {
        final String traceId = MDC.get("traceId");
        final ProblemDetails body = unauthorized(detail, request.getRequestURI(), traceId);
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), body);
    }
}
