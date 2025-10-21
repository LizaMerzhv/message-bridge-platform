package com.example.notifi.worker.webhook;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HmacSignerTest {
  private final HmacSigner signer = new HmacSigner();

  @Test
  void producesDeterministicSignature() {
    byte[] body = "{\"status\":\"SENT\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
    String signature = signer.sign(body, "super-secret");
      assertThat(signature).isEqualTo("b435de6027c068ce46ef84f6794a13a49da2ff88f9b71820e33f41f534a22d65");
  }
}
