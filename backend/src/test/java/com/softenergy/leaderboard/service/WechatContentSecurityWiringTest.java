package com.softenergy.leaderboard.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.softenergy.leaderboard.config.SoftEnergyProperties;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class WechatContentSecurityWiringTest {

    @Test
    void createsServiceThroughSpringConstructorInjection() {
        try (var context = new AnnotationConfigApplicationContext()) {
            context.registerBean(SoftEnergyProperties.class);
            context.registerBean(ObjectMapper.class);
            context.register(WechatApiResponseParser.class, WechatContentSecurityService.class);
            context.refresh();

            assertNotNull(context.getBean(WechatContentSecurityService.class));
        }
    }
}
