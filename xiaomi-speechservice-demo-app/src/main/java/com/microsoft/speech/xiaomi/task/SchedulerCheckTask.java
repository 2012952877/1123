
package com.microsoft.speech.xiaomi.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.microsoft.speech.xiaomi.service.CheckEndpointService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

//定时任务
@Component
public class SchedulerCheckTask {

    private static final Logger logger = LoggerFactory.getLogger(SchedulerCheckTask.class);
    private int i;

    @Autowired
    private  CheckEndpointService  checkEndpointService;

    //每x分钟执行一次
    @Scheduled(cron = "0 */1 * * * *")
    public void checkValidSpeechEndpoints() {
        logger.info("thread id:{},SchedulerCheckTask execute times:{}", Thread.currentThread().getId(), ++i);
        checkEndpointService.check();
    }

    @PostConstruct
    public void init() {

    }

    @PreDestroy
    public void destroy() {
        //系统运行结束
    }

}