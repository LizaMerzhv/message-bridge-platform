package com.example.notifi.worker.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.Map;

@Converter
public class JsonAttributesConverter implements AttributeConverter<Map<String, Object>, String> {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Override
  public String convertToDatabaseColumn(Map<String, Object> attribute) {
    if (attribute == null || attribute.isEmpty()) {
      return null;
    }
    try {
      return OBJECT_MAPPER.writeValueAsString(attribute);
    } catch (JsonProcessingException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  @Override
  public Map<String, Object> convertToEntityAttribute(String dbData) {
    if (dbData == null || dbData.isBlank()) {
      return Collections.emptyMap();
    }
    try {
      return OBJECT_MAPPER.readValue(
          dbData,
          OBJECT_MAPPER.getTypeFactory().constructMapType(Map.class, String.class, Object.class));
    } catch (JsonProcessingException ex) {
      throw new UncheckedIOException(ex);
    }
  }
}
