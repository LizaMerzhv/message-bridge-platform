package com.example.notifi.api.security;

import com.example.notifi.api.data.entity.ClientEntity;
import com.example.notifi.api.data.repository.ClientRepository;
import com.example.notifi.api.test.WebTestBase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockFilterConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

class ApiKeyAuthFilterTest extends WebTestBase {

    private ClientRepository clientRepository;
    private ApiKeyAuthFilter filter;
    private MockMvc mvc;

    @RestController
    static class DummyController {
        @GetMapping("/api/v1/secure")
        public String secure() {
            return "ok";
        }
    }

    @BeforeEach
    void setUpMvc() throws Exception {
        this.clientRepository = Mockito.mock(ClientRepository.class);
        this.filter = new ApiKeyAuthFilter(clientRepository, new ObjectMapper().findAndRegisterModules());
        this.filter.init(new MockFilterConfig());

        this.mvc = MockMvcBuilders
            .standaloneSetup(new DummyController())
            .addFilters(new RequestIdFilter(), filter)
            .build();
    }

    @Test
    void should_return_401_when_missing_key() throws Exception {
        var result = mvc.perform(get("/api/v1/secure")).andReturn();

        assertThat(result.getResponse().getStatus())
            .isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(result.getResponse().getContentType())
            .isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    }

    @Test
    void should_pass_when_valid_key() throws Exception {
        ClientEntity client = new ClientEntity();
        client.setId(UUID.randomUUID());
        client.setName("Client");
        client.setApiKey("key");
        client.setRateLimitPerMin(60);
        client.setCreatedAt(Instant.now());
        client.setUpdatedAt(Instant.now());

        when(clientRepository.findByApiKey(anyString())).thenReturn(Optional.of(client));

        var result = mvc.perform(get("/api/v1/secure")
                .header(ApiKeyAuthFilter.HEADER, "key"))
            .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
    }
}
