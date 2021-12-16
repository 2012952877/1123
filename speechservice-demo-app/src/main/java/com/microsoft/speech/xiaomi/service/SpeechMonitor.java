package com.microsoft.speech.xiaomi.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import  org.springframework.boot.actuate.health.HealthIndicator;
import  org.springframework.boot.actuate.health.Health;

@Component
public class SpeechMonitor implements HealthIndicator {
    /**
     * SpeechMonitor 访问: http://localhost:8090/actuator/health
     * @return 自定义Health监控
     */

    @Autowired
    private SpeechService speechService;


    @Override
    public Health health() {
        String statsInfo = speechService.collectCounterInfo();
        //自定义监控内容
        return new Health.Builder().withDetail("endpoints.success.counter", statsInfo)
                .up().build();
    }
}