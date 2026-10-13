package com.softenergy.leaderboard.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WechatApiResponseParserTest {
    private final WechatApiResponseParser parser = new WechatApiResponseParser(new ObjectMapper());

    @Test
    void parsesSuccessfulWechatResponseReadAsPlainText() {
        var response = parser.parse("{\"openid\":\"openid-1\",\"session_key\":\"session-1\"}");

        assertEquals("openid-1", response.get("openid"));
        assertEquals("session-1", response.get("session_key"));
    }

    @Test
    void preservesWechatErrorFieldsReadAsPlainText() {
        var response = parser.parse("{\"errcode\":40029,\"errmsg\":\"invalid code\"}");

        assertEquals(40029, response.get("errcode"));
        assertEquals("invalid code", response.get("errmsg"));
    }

    @Test
    void rejectsNonJsonResponseWithoutLeakingBody() {
        RestClientException exception = assertThrows(
                RestClientException.class,
                () -> parser.parse("not-json"));

        assertFalse(exception.getMessage().contains("not-json"));
    }
}
