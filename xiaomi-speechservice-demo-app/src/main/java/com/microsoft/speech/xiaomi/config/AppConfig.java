package com.microsoft.speech.xiaomi.config;

import com.microsoft.speech.xiaomi.controller.SpeechController;
import com.typesafe.config.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.microsoft.speech.xiaomi.entity.CandidateEndpoints;
import com.microsoft.speech.xiaomi.entity.SpeechEndpointBean;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.Properties;

@Configuration
public class AppConfig {
    private static final Logger logger = LoggerFactory.getLogger(AppConfig.class);

    private Config config;
    private static final Properties properties = new Properties();

    //private CandidateEndpoints candidateEndpoints;

    //if not sepcified properties file, 默认加载classpath下的application.* : application.conf,application.json和application.properties文件
    public AppConfig() {
        config = ConfigFactory.load("app.conf");
    }


    //指定配置文件
    public AppConfig(Config config) {
        this.config = config;
    }

    @Bean
    public CandidateEndpoints getCandidateEndpoints() {
        logger.info("initCandidateEndpoints ......");
        CandidateEndpoints candidateEndpoints = new CandidateEndpoints();

        List<? extends ConfigObject> serviceslist =  config.getObjectList("endpoints");
        //config.getAnyRefList("endpoints");

        int i = 1;
        for(ConfigObject cfo : serviceslist) {
            SpeechEndpointBean endpointBean = new SpeechEndpointBean();
            endpointBean.setName(cfo.get("name").unwrapped().toString());
            endpointBean.setSubscriptionKey(cfo.get("subscriptionKey").unwrapped().toString());
            endpointBean.setRegion(cfo.get("serviceRegion").unwrapped().toString());
            endpointBean.setWeight(Integer.parseInt(cfo.get("weight").unwrapped().toString()));
            i++;

            logger.info("endpointBean info:{}", endpointBean.toString());
            candidateEndpoints.addCandidateBean(endpointBean);
        }

        return candidateEndpoints;
    }


    public Config getConfig() {
        return this.config;
    }
    public Config getConfig(String propName) {
        return config.getConfig(propName);
    }

    public String getString(String propName) {
        return config.getString(propName);
    }

    public int getInt(String propName) {
        return config.getInt(propName);
    }

    public long getLong(String propName) {
        return config.getLong(propName);
    }

    public boolean getBoolean(String propName) {
        return config.getBoolean(propName);
    }

    public  void printConfig(Config config) {
        config.entrySet().forEach(e -> logger.info(e.getKey() + "=" + e.getValue().render()));
    }

    public static void main(String[] args) {
        AppConfig appConfig = new AppConfig();
        //appConfig.printConfig(appConfig.config);
        logger.info("app.name:" + appConfig.getString("app.name"));


        CandidateEndpoints candidateEndpoints = appConfig.getCandidateEndpoints();
        candidateEndpoints.printInfo();
    }
}
