package com.softenergy.leaderboard.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.util.Map;

@Component
public class WechatApiResponseParser {
    private static final TypeReference<Map<String, Object>> RESPONSE_TYPE = new TypeReference<>() {};

    private final ObjectMapper objectMapper;

    public WechatApiResponseParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> parse(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(responseBody, RESPONSE_TYPE);
        } catch (JsonProcessingException exception) {
            throw new RestClientException("微信接口返回了无法解析的响应", exception);
        }
    }

    public byte[] serialize(Object requestBody) {
        try {
            return objectMapper.writeValueAsBytes(requestBody);
        } catch (JsonProcessingException exception) {
            throw new RestClientException("微信接口请求无法序列化", exception);
        }
    }
}
