package com.softenergy.leaderboard.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.softenergy.leaderboard.api.BusinessException;
import com.softenergy.leaderboard.config.SoftEnergyProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class WechatContentSecurityServiceTest {
    private static final String TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/token"
            + "?grant_type=client_credential&appid=test-app-id&secret=test-app-secret";

    @Test
    void refreshesRejectedAccessTokenOnceAndRetriesTextCheck() {
        TestContext context = testContext();
        context.server.expect(once(), requestTo(TOKEN_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(textResponse("{\"access_token\":\"expired-token\",\"expires_in\":7200}"));
        context.server.expect(once(), requestTo(
                        "https://api.weixin.qq.com/wxa/msg_sec_check?access_token=expired-token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {"content":"周涛","version":2,"scene":2,"openid":"openid-1"}
                        """))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.TEXT_PLAIN)
                        .body("{\"errcode\":40014,\"errmsg\":\"invalid access_token\"}"));
        context.server.expect(once(), requestTo(TOKEN_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(textResponse("{\"access_token\":\"fresh-token\",\"expires_in\":7200}"));
        context.server.expect(once(), requestTo(
                        "https://api.weixin.qq.com/wxa/msg_sec_check?access_token=fresh-token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(textResponse("""
                        {"errcode":0,"errmsg":"ok","result":{"suggest":"pass","label":100}}
                        """));

        context.service.checkText("openid-1", "周涛");

        context.server.verify();
    }

    @Test
    void doesNotRetryNonTokenWechatFailure() {
        TestContext context = testContext();
        context.server.expect(once(), requestTo(TOKEN_URL))
                .andRespond(textResponse("{\"access_token\":\"valid-token\",\"expires_in\":7200}"));
        context.server.expect(once(), requestTo(
                        "https://api.weixin.qq.com/wxa/msg_sec_check?access_token=valid-token"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andRespond(textResponse("{\"errcode\":48001,\"errmsg\":\"api unauthorized\"}"));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> context.service.checkText("openid-1", "周涛"));

        assertEquals("CONTENT_SECURITY_UNAVAILABLE", exception.getCode());
        context.server.verify();
    }

    private TestContext testContext() {
        SoftEnergyProperties properties = new SoftEnergyProperties();
        properties.getWechat().setContentSecurityEnabled(true);
        properties.getWechat().setMockEnabled(false);
        properties.getWechat().setAppId("test-app-id");
        properties.getWechat().setAppSecret("test-app-secret");

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        var parser = new WechatApiResponseParser(new ObjectMapper());
        var service = new WechatContentSecurityService(properties, parser, builder.build());
        return new TestContext(service, server);
    }

    private ResponseCreator textResponse(String body) {
        return withSuccess(body, MediaType.TEXT_PLAIN);
    }

    private record TestContext(
            WechatContentSecurityService service,
            MockRestServiceServer server) {}
}
