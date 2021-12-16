package com.microsoft.speech.xiaomi.controller;

import com.microsoft.speech.xiaomi.dto.RequestDto;
import com.microsoft.speech.xiaomi.dto.ResultInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

import  com.microsoft.speech.xiaomi.dto.Response;
import  com.microsoft.speech.xiaomi.service.SpeechService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/speech")
public class SpeechController {
	private static final Logger logger = LoggerFactory.getLogger(SpeechController.class);

	@Autowired
	SpeechService speechService;


	//语音文件转文本
	//请求参数example: {"filePath":"C:\\Users\\zhadeng\\githubApps\\azure-samples\\cognitive-services-speech-sdk\\sampledata\\audiofiles\\myVoiceIsMyPassportVerifyMe03.wav"}
	@PostMapping("/wav2text")
	public ResponseEntity<?> handleWav2text(@RequestBody RequestDto requestDto) {
		logger.info("handling wavFile:" + requestDto.getFilePath());
		ResultInfo resultInfo = speechService.handleWav2Text(requestDto.getFilePath());
		if(resultInfo.success()) {
			return ResponseEntity.ok(Response.success("wav2text OK; " + resultInfo.getInfo()));
		} else  {
			return ResponseEntity.ok(Response.failure("wav2text fail; " + resultInfo.getInfo()));
		}
	}

	/* 处理按指定语言进行语音识别成text。输入audio来自microphone
	 *  请求参数example:{} 或 {“lang”:"zh-CN"}
	 */
	@PostMapping("/recognitionWithLangAsync")
	public ResponseEntity<?> recognitionWithLanguageAsync(@RequestBody RequestDto requestDto) {
		logger.info("handling recognitionWithLanguageAsync......" );
		ResultInfo resultInfo = speechService.recognitionWithLanguageAsync(requestDto.getLang());
		if(resultInfo.success()) {
			return ResponseEntity.ok(Response.success("recognitionWithLanguageAsync OK; info:" + resultInfo.getInfo()));
		} else  {
			return ResponseEntity.ok(Response.failure("recognitionWithLanguageAsync fail; info:" + resultInfo.getInfo()));
		}
	}


	/*
		把文本合成为语音输出
        requestDto.conent为输入的文本内容
	 */
	@PostMapping("/text2speech")
	public ResponseEntity<?> handleText2speech(@RequestBody RequestDto requestDto) {
		logger.info("recv text2speech req:{}", requestDto.toString());
		ResultInfo resultInfo = speechService.handleText2speech(requestDto.getContent());
		if(resultInfo.success()) {
			return ResponseEntity.ok(Response.success("text2speech OK; " + resultInfo.getInfo()));
		} else  {
			return ResponseEntity.ok(Response.failure("text2speech fail; " +resultInfo.getInfo()));
		}
	}


}
