package com.example.notifi.worker.webhook;

import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class HmacSigner {
  public String sign(byte[] body, String secret) {
    if (secret == null || secret.isBlank()) {
      throw new IllegalArgumentException("Secret must be provided");
    }
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      byte[] result = mac.doFinal(body);
      StringBuilder builder = new StringBuilder(result.length * 2);
      for (byte b : result) {
        builder.append(String.format("%02x", b));
      }
      return builder.toString();
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to sign payload", ex);
    }
  }
}
