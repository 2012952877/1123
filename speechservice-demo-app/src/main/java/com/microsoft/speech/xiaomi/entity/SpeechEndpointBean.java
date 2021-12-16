package com.microsoft.speech.xiaomi.entity;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

import java.util.Objects;

@Data
public class SpeechEndpointBean {
    private String name;
    private String subscriptionKey;
    private String region;
    private Integer weight;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SpeechEndpointBean that = (SpeechEndpointBean) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return JSONObject.toJSONString(this);
    }
}