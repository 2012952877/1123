package com.microsoft.speech.xiaomi.service;


import com.microsoft.cognitiveservices.speech.*;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;
import com.microsoft.cognitiveservices.speech.audio.AudioOutputStream;
import com.microsoft.cognitiveservices.speech.audio.PullAudioOutputStream;
import com.microsoft.speech.xiaomi.config.AppConfig;
import com.microsoft.speech.xiaomi.entity.SpeechEndpointBean;
import com.microsoft.speech.xiaomi.utils.CommonUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import com.microsoft.speech.xiaomi.entity.CandidateEndpoints;

import java.util.*;
import java.util.concurrent.ExecutionException;

@Service
@Slf4j
public class CheckEndpointService  {
    private static final Logger logger = LoggerFactory.getLogger(CheckEndpointService.class);

    //当前有效的候选endpoints,可能动态在变化
    private CandidateEndpoints actualEndpoints;

    @Autowired
    private AppConfig appConfig;

    public CheckEndpointService() {
        actualEndpoints = new CandidateEndpoints();
        logger.info("{}: CheckEndpointService constructor..." , System.currentTimeMillis());
    }


    public CandidateEndpoints check() {
        CandidateEndpoints candidateEndpoints =  appConfig.getCandidateEndpoints();
        if(candidateEndpoints.isEmpty()) {
            actualEndpoints.clear();
            return actualEndpoints;
        }

        CandidateEndpoints checkingEndpoints = new CandidateEndpoints();
        for(SpeechEndpointBean endpointBean: candidateEndpoints.getCandidateEndpointList()) {
            boolean validFlag = false;
            //checkValid会重试两次
            for(int i=0; i<2; i++) {
                validFlag = checkValidEndpoint(endpointBean);
                if(validFlag) break;;
            }
            if(validFlag) {
                checkingEndpoints.addCandidateBean(endpointBean);
            }
        }

        actualEndpoints.copy(checkingEndpoints);
        logger.info("check valid endpoints......, size:{}", actualEndpoints.size());

        return actualEndpoints;
    }

    public CandidateEndpoints getActualEndpoints() {
        return actualEndpoints;
    }


    public int getCandidateSize() {
        return actualEndpoints.size();
    }


    //需过滤的endpoint name 列表
    public SpeechEndpointBean chooseOne(int mode, List<String> filteredEndpointList) {
        SpeechEndpointBean bean = null;
        ////同时从实际有效的列表中 移除需过滤的这部分列表
        //actualEndpoints.remove(filteredEndpointList);

        if(!actualEndpoints.isEmpty()) {
            //从实际可用列表中按权重选择一个
            String endpointName = mode > 0 ? CommonUtil.chooseEndpoint(actualEndpoints, filteredEndpointList)
                    : CommonUtil.chooseEndpointByRandom(actualEndpoints, filteredEndpointList);
            bean = actualEndpoints.selectEndpoint(endpointName);
        } else {
            //直接从配置列表的endpoint中按权重随机选择一个
            CandidateEndpoints candidateEndpoints = appConfig.getCandidateEndpoints();
            String endpointName = mode > 0 ? CommonUtil.chooseEndpoint(candidateEndpoints, filteredEndpointList)
                    : CommonUtil.chooseEndpointByRandom(candidateEndpoints, filteredEndpointList);
            bean = candidateEndpoints.selectEndpoint(endpointName);
        }

        if(bean != null) {
            logger.info("choosed endpoint:{}", bean.toString());
        }
        return bean;
    }

    //从输入的指定候选列表中选择一个
    public SpeechEndpointBean chooseOne(CandidateEndpoints inputEndpoints, List<String> filteredEndpointList) {
        SpeechEndpointBean bean = null;
        if(inputEndpoints != null && !inputEndpoints.isEmpty() ) {
            //从实际可用列表中按权重选择一个
            String endpointName = CommonUtil.chooseEndpoint(inputEndpoints, filteredEndpointList);
            bean = inputEndpoints.selectEndpoint(endpointName);
        }

        logger.info("choosed endpoint:{}", bean.toString());
        return bean;
    }

    //生成新的CandidateEndpoints
    public CandidateEndpoints genNewCandidates(CandidateEndpoints candidateEndpoints, List<String> filteredEndpointList) {
        if(filteredEndpointList == null || filteredEndpointList.size() == 0) {
            return candidateEndpoints;
        }

        CandidateEndpoints newCandidates = new CandidateEndpoints();
        for(SpeechEndpointBean endpointBean: candidateEndpoints.getCandidateEndpointList()) {
            if(!filteredEndpointList.contains(endpointBean.getName())) {
                newCandidates.addCandidateBean(endpointBean);
            }
        }
        return newCandidates;
    }

    public boolean checkValidEndpoint(SpeechEndpointBean endpointBean) {
        boolean ret = false;
        try {
            //调用文本转语音的语音合成来测试endpint是否可用可连接
            ret = synthesisToPullAudioOutputStreamAsync(endpointBean);
        } catch (Exception e) {
            e.printStackTrace();
            ret = false;
        }

        return ret;
    }

    // Speech synthesis to pull audio output stream.
    public boolean synthesisToPullAudioOutputStreamAsync(SpeechEndpointBean endpointBean) throws InterruptedException, ExecutionException
    {
        boolean ret = true;
        SpeechConfig config = SpeechConfig.fromSubscription(endpointBean.getSubscriptionKey(), endpointBean.getRegion());
        if(config == null) {
            return false;
        }
        // The default language is "en-us".
        config.setSpeechSynthesisLanguage("zh-CN");
        String exampleText =  "你好";

        // Creates an audio out stream.
        PullAudioOutputStream stream = AudioOutputStream.createPullStream();

        // Creates a speech synthesizer using audio stream output.
        AudioConfig streamAudioConfig = AudioConfig.fromStreamOutput(stream);
        SpeechSynthesizer synthesizer = new SpeechSynthesizer(config, streamAudioConfig);
        SpeechSynthesisResult result = synthesizer.SpeakTextAsync(exampleText).get();

        // Checks result.
        String msg = "";
        ResultReason resultReason = result.getReason();
        if (resultReason == ResultReason.SynthesizingAudioCompleted) {
            msg = String.format("*** synthesized OK [%s]", exampleText);
        } else if (resultReason == ResultReason.Canceled) {
            SpeechSynthesisCancellationDetails cancellation = SpeechSynthesisCancellationDetails.fromResult(result);
            msg = "*** CANCELED: Reason=" + cancellation.getReason();
            if (cancellation.getReason() == CancellationReason.Error) {
                msg = msg + String.format(";ErrorCode=%s; ErrorDetails=%s",  cancellation.getErrorCode().toString(), cancellation.getErrorDetails());
            }
            ret = false;
        } else {
            msg = "resultReason:" + resultReason;
        }
        result.close();
        logger.info("ret={}; msg:{}", ret, msg);

        synthesizer.close();
        streamAudioConfig.close();

        //statStreamSize(stream);
        stream.close();
        return ret;
    }

    private long statStreamSize(PullAudioOutputStream stream) {
        // Reads(pulls) data from the stream
        byte[] buffer = new byte[50000];
        long totalSize = 0;
        long filledSize = stream.read(buffer);
        while (filledSize > 0) {
            //logger.info(filledSize + " bytes received.");
            totalSize += filledSize;
            filledSize = stream.read(buffer);
        }

        logger.info("Totally " + totalSize + " bytes received.");
        return totalSize;
    }
}