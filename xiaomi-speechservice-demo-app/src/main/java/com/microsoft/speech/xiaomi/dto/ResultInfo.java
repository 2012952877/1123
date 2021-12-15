package com.microsoft.speech.xiaomi.dto;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

@Data
public class ResultInfo {
    private int code; //返回码
    private String info; //内容信息
    private String desc; //描述
    private String endpointName; //endpoint name


    public final static int SUCCESS_CODE = 0;
    public final static int FAIL_CODE = 1;
    public final static int CANCELED_ERROR_CODE = 2;

    public ResultInfo() {
        code = SUCCESS_CODE;
    }

    public ResultInfo(int code, String info) {
        this.code = code;
        this.info = info;
    }

    public ResultInfo(int code, String info, String endpointName) {
        this.code = code;
        this.info = info;
        this.endpointName = endpointName;
    }

    //code == 0: OK true; or is false
    public boolean success() {
        return  code == SUCCESS_CODE;
    }

    public boolean error() {
        return  code == CANCELED_ERROR_CODE;
    }

    public void setResult(int code, String info) {
        this.code = code;
        this.info = info;
    }

    @Override
    public String toString() {
        return JSONObject.toJSONString(this);
    }
}