package com.example.notifi.api.model;

import com.example.notifi.api.data.entity.Channel;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ChannelJsonTest {
    private final ObjectMapper om = new ObjectMapper();

    @Test
    void json_roundtrip_case_insensitive() throws JsonProcessingException {
        String json = om.writeValueAsString(Channel.EMAIL);
        assertThat(json).isEqualTo("\"email\"");
        Channel back1 = om.readValue("\"EMAIL\"", Channel.class);
        Channel back2 = om.readValue("\"eMaIl\"", Channel.class);
        assertThat(back1).isEqualTo(Channel.EMAIL);
        assertThat(back2).isEqualTo(Channel.EMAIL);
    }
}
