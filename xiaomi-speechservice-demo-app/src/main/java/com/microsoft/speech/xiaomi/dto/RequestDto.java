package com.microsoft.speech.xiaomi.dto;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

@Data
public class RequestDto {
    private String content;
    private String filePath;
    private String lang;


    @Override
    public String toString() {
        return JSONObject.toJSONString(this);
    }
}