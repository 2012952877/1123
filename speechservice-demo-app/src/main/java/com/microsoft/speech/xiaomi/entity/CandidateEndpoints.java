package com.microsoft.speech.xiaomi.entity;

import java.util.ArrayList;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class CandidateEndpoints {
    private static final Logger logger = LoggerFactory.getLogger(CandidateEndpoints.class);
    private List<SpeechEndpointBean> candidateEndpointList;

    public CandidateEndpoints() {
        this.candidateEndpointList = new ArrayList<>();
    }

    public CandidateEndpoints(List<SpeechEndpointBean> candidateEndpointList) {
        this.candidateEndpointList = candidateEndpointList;
    }

    public void clear() {
        candidateEndpointList.clear();
    }

    public void copy(CandidateEndpoints other) {
        clear();
        if(other != null) {
            candidateEndpointList.addAll(other.getCandidateEndpointList());
        }
    }


    public void addCandidateBean(SpeechEndpointBean endpointBean) {
        if(endpointBean != null) {
            candidateEndpointList.add(endpointBean);
        }
    }

    public void remove(SpeechEndpointBean endpointBean) {
        candidateEndpointList.remove(endpointBean);
    }

    //remove by name
    public void remove(String name) {
        for(SpeechEndpointBean endpointBean: candidateEndpointList) {
            if(name.equals(endpointBean.getName())) {
                candidateEndpointList.remove(endpointBean);
                break;
            }
        }
    }


    //过滤或移除掉指定黑名单中的endpoints
    public void remove(List<String> filteredList) {
        if(filteredList == null || filteredList.size() == 0) {
            return;
        }

        for(SpeechEndpointBean endpointBean: candidateEndpointList) {
            if(filteredList.contains(endpointBean.getName())) {
                candidateEndpointList.remove(endpointBean);
                break;
            }
        }
    }

    public CandidateEndpoints filterGen(List<String> filteredList) {
        CandidateEndpoints endpoints = new CandidateEndpoints();
        endpoints.copy(this);
        endpoints.remove(filteredList);
        return endpoints;
    }

    public boolean isEmpty() {
        return candidateEndpointList.isEmpty();
    }

    public SpeechEndpointBean selectEndpoint(String name) {
        if(StringUtils.isNotBlank(name)) {
            for (SpeechEndpointBean endpointBean : candidateEndpointList) {
                if (name.equals(endpointBean.getName())) {
                    return endpointBean;
                }
            }
        }
        return null;
    }

    public List<SpeechEndpointBean> getCandidateEndpointList() {
        return candidateEndpointList;
    }

    public void setCandidateEndpointList(List<SpeechEndpointBean> candidateEndpointList) {
        this.candidateEndpointList = candidateEndpointList;
    }

    public int size() {
        return candidateEndpointList.size();
    }

    //取idx 索引的Bean
    public SpeechEndpointBean get(int idx) {
        if(idx < candidateEndpointList.size()) {
            return candidateEndpointList.get(idx);
        }

        return null;
    }

    public void printInfo() {
        logger.info("candSize={}", size());
        int i = 0;
        for(SpeechEndpointBean endpointBean: candidateEndpointList) {
            logger.info("#{}. endpoint info: {}", ++i, endpointBean.toString());
        }
    }

    @Override
    public String toString() {
        return JSONObject.toJSONString(this);
    }
}