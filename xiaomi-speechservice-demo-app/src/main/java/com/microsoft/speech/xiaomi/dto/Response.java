package com.microsoft.speech.xiaomi.dto;

import java.io.Serializable;

public class Response<T> implements Serializable {
	private Integer code;

	private String message;

	private T data;

	private Response(){}

	public static <T> Response<T> success(T data) {
		Response response = new Response();
		response.setCode(ResponseCode.success);
		response.setMessage(ResponseCode.success_str);
		response.setData(data);
		return response;
	}

	public static <T> Response<T> auth(T data) {
		Response response = new Response();
		response.setCode(ResponseCode.auth);
		response.setMessage(ResponseCode.auth_str);
		response.setData(data);
		return response;
	}

	public static <T extends Serializable> Response<T> failure(String message) {
		Response response = new Response();
		response.setCode(ResponseCode.failure);
		response.setMessage(message);
		return response;
	}

	public static <T extends Serializable> Response<T> exception(Throwable throwable) {
		Response response = new Response();
		response.setCode(ResponseCode.exception);
		response.setMessage(throwable.getMessage());
		return response;
	}

	public Integer getCode() {
		return code;
	}

	public void setCode(Integer code) {
		this.code = code;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public T getData() {
		return data;
	}

	public void setData(T data) {
		this.data = data;
	}
}
