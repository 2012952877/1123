package com.microsoft.speech.xiaomi.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.microsoft.speech.xiaomi.entity.CandidateEndpoints;
import com.microsoft.speech.xiaomi.entity.SpeechEndpointBean;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CommonUtil {
    private static final Logger logger = LoggerFactory.getLogger(CommonUtil.class);

    //返回按权重选中的endpoint key name
    public static String chooseEndpoint(CandidateEndpoints candidateEndpoints, List<String> filteredEndpointList) {
        if (candidateEndpoints.isEmpty()) {
            return null;
        }

        List<WeightRandom.WeightItem<String>> itemList = new ArrayList<>();
        for (SpeechEndpointBean bean : candidateEndpoints.getCandidateEndpointList()) {
            String name = bean.getName();
            if (filteredEndpointList == null || (!filteredEndpointList.contains(name))) {
                itemList.add(new WeightRandom.WeightItem<>(name, bean.getWeight()));
            }
        }

        if (itemList.size() == 0) {
            return null;
        }

        WeightRandom<String> weightRandom = new WeightRandom<>(itemList);
        return weightRandom.choose();
    }


    //随机从candidateEndpoints取一个endpoint,同时要求不在需过滤的列表中
    public static String chooseEndpointByRandom(CandidateEndpoints candidateEndpoints, List<String> filteredEndpointList) {
        if (candidateEndpoints.isEmpty()) {
            return null;
        }

        CandidateEndpoints newCandidates = new CandidateEndpoints();
        newCandidates.copy(candidateEndpoints);
        newCandidates.remove(filteredEndpointList);

        int nIndex = genRandomNum(newCandidates.size());
        SpeechEndpointBean bean = newCandidates.get(nIndex);
        if(bean != null) {
            return  bean.getName();
        }

        return null;
    }

    //生成[0, maxRandom)间的随机数
    public static int genRandomNum(int maxRandom) {
        Random rand = new Random();
        return rand.nextInt(maxRandom);
    }
}