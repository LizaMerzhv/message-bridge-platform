package com.example.notifi.securityservice.web.internal;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.notifi.securityservice.data.entity.ClientEntity;
import com.example.notifi.securityservice.data.repository.ClientRepository;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ApiKeyResolveController.class)
class ApiKeyResolveControllerTest {

  @Autowired private MockMvc mvc;

  @MockBean private ClientRepository clientRepository;

  @Test
  void shouldReturnUnauthorizedWhenHeaderMissing() throws Exception {
    mvc.perform(get("/internal/security/resolve")).andExpect(status().isUnauthorized());
  }

  @Test
  void shouldReturnClientWhenApiKeyValid() throws Exception {
    UUID clientId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    ClientEntity client = new ClientEntity();
    setField(client, "id", clientId);
    setField(client, "name", "Demo client");
    setField(client, "apiKey", "demo-key");
    setField(client, "rateLimitPerMin", 60);
    setField(client, "createdAt", Instant.now());
    setField(client, "updatedAt", Instant.now());

    when(clientRepository.findByApiKey(anyString())).thenReturn(Optional.of(client));

    mvc.perform(get("/internal/security/resolve").header("X-API-Key", "demo-key"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.clientId", is(clientId.toString())))
        .andExpect(jsonPath("$.clientName", is("Demo client")))
        .andExpect(jsonPath("$.rateLimitPerMin", is(60)));
  }

  private static void setField(Object target, String fieldName, Object value) {
    try {
      Field field = target.getClass().getDeclaredField(fieldName);
      field.setAccessible(true);
      field.set(target, value);
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
  }
}
