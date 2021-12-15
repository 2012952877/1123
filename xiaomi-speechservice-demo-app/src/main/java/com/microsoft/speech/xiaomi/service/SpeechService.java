package com.microsoft.speech.xiaomi.service;

import com.alibaba.fastjson.JSON;
import com.microsoft.cognitiveservices.speech.audio.AudioInputStream;
import com.microsoft.cognitiveservices.speech.audio.PushAudioInputStream;
import com.microsoft.speech.xiaomi.config.AppConfig;
import com.microsoft.speech.xiaomi.dto.ResultInfo;

import com.microsoft.cognitiveservices.speech.*;
import com.microsoft.cognitiveservices.speech.ResultReason;

import com.microsoft.cognitiveservices.speech.audio.PullAudioInputStreamCallback;
import com.microsoft.speech.xiaomi.entity.SpeechEndpointBean;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;

import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;
import com.microsoft.speech.xiaomi.stream.WavStream;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.io.FileInputStream;
import java.util.concurrent.Future;

@Endpoint(id = "speechService")
@Service
@Slf4j
public class SpeechService {
    private static final Logger logger = LoggerFactory.getLogger(SpeechService.class);
    private static int retryTimes = 2;


    @Autowired
    private AppConfig appConfig;

    @Autowired
    private CheckEndpointService checkEndpointService;

    // The Source to stop recognition.
    private static Semaphore stopRecognitionSemaphore;

    private Map<String, Long> endpointCounterMap = new HashMap<>();
    private Map<String, Long> endpointFailCounterMap = new HashMap<>();

    private SpeechConfig intSpeechConfig(SpeechEndpointBean endpointBean) {
        SpeechConfig config = null;
        try {
            config = SpeechConfig.fromSubscription(endpointBean.getSubscriptionKey(), endpointBean.getRegion());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return config;
    }

    private int getChooseMode() {
        int chooseMode = appConfig.getInt("choose.mode");
        logger.info("mode={}", chooseMode);
        return chooseMode;
    }

    public String collectCounterInfo() {
        return JSON.toJSONString(endpointCounterMap);
    }

    public String collectFailCounterInfo() {
        return JSON.toJSONString(endpointFailCounterMap);
    }

    //处理wav语音文件转文本
    public ResultInfo handleWav2Text(String wavAudioFileName) {
        ResultInfo resultInfo = new ResultInfo();
        List<String> filteredList = new ArrayList<>();

        for (int i = 0; i < retryTimes; i++) {
            resultInfo = recognitionWithAudioStreamAsync(wavAudioFileName, filteredList);
            String endpointName = resultInfo.getEndpointName();

            //or use follow method
            //resultInfo = continuousRecognitionWithPushStream(wavAudioFileName, filteredList);
            if (resultInfo.success()) {
                Long cnt = 1L;
                if(endpointCounterMap.containsKey(endpointName)) {
                    cnt += endpointCounterMap.get(endpointName);
                }
                endpointCounterMap.put(endpointName, cnt);
                return resultInfo;
            } else {
                if (resultInfo.error()) {
                    filteredList.add(resultInfo.getEndpointName());
                    Long failCnt = 1L;
                    if(endpointFailCounterMap.containsKey(endpointName)) {
                        failCnt += endpointFailCounterMap.get(endpointName);
                    }
                    endpointFailCounterMap.put(endpointName, failCnt);
                } else {
                    break;
                }
            }
        } //-->for(...)

        return resultInfo;
    }

    // Speech recognition with events from a push stream
    // This sample takes and existing file and reads it by chunk into a local buffer and then pushes the
    // buffer into an PushAudioStream for speech recognition.

    private ResultInfo continuousRecognitionWithPushStream(String wavAudioFileName, List<String> filteredList) {
        ResultInfo resultInfo = new ResultInfo();

        SpeechEndpointBean endpointBean = checkEndpointService.chooseOne(getChooseMode(), filteredList);
        if (endpointBean == null) {
            resultInfo.setResult(ResultInfo.FAIL_CODE, "No valid endpoint to choose");
            return resultInfo;
        }

        SpeechConfig speechConfig = intSpeechConfig(endpointBean);
        if (speechConfig == null) {
            resultInfo.setResult(ResultInfo.FAIL_CODE, "speechConfig is null");
            return resultInfo;
        }

        try {
            InputStream inputStream = new FileInputStream(wavAudioFileName);

            // Create the push stream to push audio to.
            PushAudioInputStream pushStream = AudioInputStream.createPushStream();

            // Creates a speech recognizer using Push Stream as audio input.
            AudioConfig audioInput = AudioConfig.fromStreamInput(pushStream);
            SpeechRecognizer recognizer = new SpeechRecognizer(speechConfig, audioInput);

            resultInfo = recognitionWithPushStream(recognizer, inputStream, pushStream);
            resultInfo.setEndpointName(endpointBean.getName());

            speechConfig.close();
            audioInput.close();
            recognizer.close();

            logger.info("info={}", resultInfo.getInfo());
        } catch (Exception e) {
            resultInfo.setResult(ResultInfo.FAIL_CODE, "Exception:" + e.getMessage());
            resultInfo.setEndpointName(endpointBean.getName());
        }

        return resultInfo;
    }

    private ResultInfo recognitionWithPushStream(SpeechRecognizer recognizer, InputStream inputStream, PushAudioInputStream pushStream) {
        ResultInfo resultInfo = new ResultInfo();
        try {
            // Subscribes to events.
            recognizer.recognizing.addEventListener((s, e) -> {
                logger.info("RECOGNIZING: Text=" + e.getResult().getText());
            });

            recognizer.recognized.addEventListener((s, e) -> {
                String msg = "";
                if (e.getResult().getReason() == ResultReason.RecognizedSpeech) {
                    msg = "RECOGNIZED: Text=" + e.getResult().getText();
                    resultInfo.setResult(ResultInfo.SUCCESS_CODE, msg);
                    logger.info(msg);
                } else if (e.getResult().getReason() == ResultReason.NoMatch) {
                    msg = "NOMATCH: Speech could not be recognized.";
                    resultInfo.setResult(ResultInfo.SUCCESS_CODE, msg);
                    logger.info(msg);
                }
            });

            recognizer.canceled.addEventListener((s, e) -> {
                //正常的CANCELED: Reason=EndOfStream
                String msg = "CANCELED: Reason=" + e.getReason();

                //CANCELED: Reason=Error
                //CANCELED: ErrorCode=AuthenticationFailure
                //CANCELED: ErrorDetails=WebSocket upgrade failed: Authentication error (401). Please check subscription information and region name. SessionId: c86f2fd9e372458c8c30bd331d797e77
                if (e.getReason() == CancellationReason.Error) {
                    msg = msg + ";errorCode=" + e.getErrorCode() + ";errorDetails=" + e.getErrorDetails();
                    resultInfo.setResult(ResultInfo.CANCELED_ERROR_CODE, msg);
                }

                logger.info(msg);
            });

            recognizer.sessionStarted.addEventListener((s, e) -> {
                logger.info("\nSession started event.");
            });

            recognizer.sessionStopped.addEventListener((s, e) -> {
                // Stops translation when session stop is detected.
                logger.info("\nnSession Stop event --> Stop translation.");
            });

            // Starts continuous recognition. Uses stopContinuousRecognitionAsync() to stop recognition.
            recognizer.startContinuousRecognitionAsync().get();

            // Arbitrary buffer size.
            byte[] readBuffer = new byte[4096];

            // Push audio read from the file into the PushStream.
            // The audio can be pushed into the stream before, after, or during recognition
            // and recognition will continue as data becomes available.
            int bytesRead;
            while ((bytesRead = inputStream.read(readBuffer)) != -1)
            {
                if (bytesRead == readBuffer.length) {
                    pushStream.write(readBuffer);
                } else {
                    // Last buffer read from the WAV file is likely to have less bytes
                    pushStream.write(Arrays.copyOfRange(readBuffer, 0, bytesRead));
                }
            }

            pushStream.close();
            inputStream.close();

            // Stops recognition.
            recognizer.stopContinuousRecognitionAsync().get();
        } catch (Exception e) {
            resultInfo.setResult(ResultInfo.FAIL_CODE, "Exception:" + e.getMessage());
        }

        return resultInfo;
    }

    // Speech recognition with audio stream
    private ResultInfo recognitionWithAudioStreamAsync(String wavAudioFileName, List<String> filteredList) {
        ResultInfo resultInfo = new ResultInfo();

        SpeechEndpointBean endpointBean = checkEndpointService.chooseOne(getChooseMode(), filteredList);
        if (endpointBean == null) {
            resultInfo.setResult(ResultInfo.FAIL_CODE, "No valid endpoint to choose");
            return resultInfo;
        }

        SpeechConfig speechConfig = intSpeechConfig(endpointBean);
        if (speechConfig == null) {
            resultInfo.setResult(ResultInfo.FAIL_CODE, "speechConfig is null");
            return resultInfo;
        }

        try {
            stopRecognitionSemaphore = new Semaphore(0);

            // Create an audio stream from a wav file.
            PullAudioInputStreamCallback callback = new WavStream(new FileInputStream(wavAudioFileName));
            AudioConfig audioInput = AudioConfig.fromStreamInput(callback);
            // Creates a speech recognizer using audio stream input.
            SpeechRecognizer recognizer = new SpeechRecognizer(speechConfig, audioInput);

            resultInfo = recognitionWithAudio(recognizer);
            resultInfo.setEndpointName(endpointBean.getName());

            speechConfig.close();
            audioInput.close();
            recognizer.close();
            logger.info("info={}", resultInfo.getInfo());
        } catch (Exception e) {
            resultInfo.setResult(ResultInfo.FAIL_CODE, "Exception:" + e.getMessage());
            resultInfo.setEndpointName(endpointBean.getName());
        }

        return resultInfo;
    }

    private ResultInfo recognitionWithAudio(SpeechRecognizer recognizer) {
        ResultInfo resultInfo = new ResultInfo();
        try {
            // Subscribes to events.
            recognizer.recognizing.addEventListener((s, e) -> {
                //logger.info("1.RECOGNIZING: Text=" + e.getResult().getText());
            });

            recognizer.recognized.addEventListener((s, e) -> {
                String msg = "";
                if (e.getResult().getReason() == ResultReason.RecognizedSpeech) {
                    msg = "RECOGNIZED: Text=" + e.getResult().getText();
                    resultInfo.setResult(ResultInfo.SUCCESS_CODE, msg);
                    logger.info(msg);
                } else if (e.getResult().getReason() == ResultReason.NoMatch) {
                    msg = "NOMATCH: Speech could not be recognized.";
                    resultInfo.setResult(ResultInfo.SUCCESS_CODE, msg);
                    logger.info(msg);
                }
            });

            recognizer.canceled.addEventListener((s, e) -> {
                //正常的CANCELED: Reason=EndOfStream
                String msg = "CANCELED: Reason=" + e.getReason();

                //CANCELED: Reason=Error
                //CANCELED: ErrorCode=AuthenticationFailure
                //CANCELED: ErrorDetails=WebSocket upgrade failed: Authentication error (401). Please check subscription information and region name. SessionId: c86f2fd9e372458c8c30bd331d797e77
                if (e.getReason() == CancellationReason.Error) {
                    msg = msg + ";errorCode=" + e.getErrorCode() + ";errorDetails=" + e.getErrorDetails();
                    resultInfo.setResult(ResultInfo.CANCELED_ERROR_CODE, msg);
                }

                logger.info(msg);
                stopRecognitionSemaphore.release();
            });

            recognizer.sessionStarted.addEventListener((s, e) -> {
                logger.info("\nSession started event.");
            });

            recognizer.sessionStopped.addEventListener((s, e) -> {
                // Stops translation when session stop is detected.
                logger.info("\nnSession Stop event --> Stop translation.");
                stopRecognitionSemaphore.release();
            });

            // Starts continuous recognition. Uses stopContinuousRecognitionAsync() to stop recognition.
            recognizer.startContinuousRecognitionAsync().get();

            // Waits for completion.
            stopRecognitionSemaphore.acquire();

            // Stops recognition.
            recognizer.stopContinuousRecognitionAsync().get();
        } catch (Exception e) {
            resultInfo.setResult(ResultInfo.FAIL_CODE, "Exception:" + e.getMessage());
        }

        return resultInfo;
    }

    //处理按指定语言进行语音识别成text。输入audio来自microphone
    public ResultInfo recognitionWithLanguageAsync(String lang) {
        ResultInfo resultInfo = new ResultInfo();
        List<String> filteredList = new ArrayList<>();
        for (int i = 0; i < retryTimes; i++) {
            resultInfo = recognitionWithLanguageAsync(lang, filteredList);
            String endpointName = resultInfo.getEndpointName();

            if (resultInfo.success()) {
                Long cnt = 1L;
                if(endpointCounterMap.containsKey(endpointName)) {
                    cnt += endpointCounterMap.get(endpointName);
                }
                endpointCounterMap.put(endpointName, cnt);

                return resultInfo;
            } else {
                if (resultInfo.error()) {
                    filteredList.add(endpointName);

                    Long failCnt = 1L;
                    if(endpointFailCounterMap.containsKey(endpointName)) {
                        failCnt += endpointFailCounterMap.get(endpointName);
                    }
                    endpointFailCounterMap.put(endpointName, failCnt);
                } else {
                    break;
                }
            }
        } //-->for(...)

        return resultInfo;
    }


    //指定语言进行语音识别成text， Speech recognition in the specified language, using microphone as audio input.
    private ResultInfo recognitionWithLanguageAsync(String lang, List<String> filteredList) {
        ResultInfo resultInfo = new ResultInfo();
        SpeechEndpointBean endpointBean = checkEndpointService.chooseOne(getChooseMode(),filteredList);
        if (endpointBean == null) {
            resultInfo.setResult(ResultInfo.FAIL_CODE, "No valid endpoint to choose");
            return resultInfo;
        }

        SpeechConfig speechConfig = intSpeechConfig(endpointBean);
        if (speechConfig == null) {
            resultInfo.setResult(ResultInfo.FAIL_CODE, "speechConfig is null");
            return resultInfo;
        }

        //en-US, default is zh-CN
        String language = StringUtils.isEmpty(lang) ? "zh-CN" : lang;
        try {
            // Creates a speech recognizer for the specified language, using microphone as audio input.
            SpeechRecognizer recognizer = new SpeechRecognizer(speechConfig, language);
            resultInfo = recognizeOnceAsync(recognizer);
            resultInfo.setEndpointName(endpointBean.getName());

            speechConfig.close();
            recognizer.close();
        } catch (Exception e) {
            String msg = "recognitionWithLanguageAsync exception:" + e.getMessage();
            logger.info(msg);
            resultInfo = new ResultInfo();
            resultInfo.setResult(ResultInfo.FAIL_CODE, msg);
            resultInfo.setEndpointName(endpointBean.getName());
        }

        return resultInfo;
    }

//    private SpeechRecognizer buildSpeechRecognizer(SpeechConfig speechConfig, String lang) {
//        // Creates a speech recognizer for the specified language, using microphone as audio input.
//        SpeechRecognizer recognizer = new SpeechRecognizer(speechConfig, lang);
//        return recognizer;
//    }

    private ResultInfo recognizeOnceAsync(SpeechRecognizer recognizer) throws InterruptedException, ExecutionException {
        ResultInfo resultInfo = new ResultInfo();

        // Starts recognizing. Say something with microphone...
        // Starts recognition. It returns when the first utterance has been recognized.
        SpeechRecognitionResult result = recognizer.recognizeOnceAsync().get();
        // Checks result.
        ResultReason reason = result.getReason();
        if (reason == ResultReason.RecognizedSpeech) {
            resultInfo.setResult(ResultInfo.SUCCESS_CODE, "RECOGNIZED: Text=" + result.getText());
        } else if (reason == ResultReason.NoMatch) {
            resultInfo.setResult(ResultInfo.SUCCESS_CODE, "NOMATCH: Speech could not be recognized.");
        } else if (reason == ResultReason.Canceled) {
            CancellationDetails cancellation = CancellationDetails.fromResult(result);
            String msg = "CANCELED: Reason=" + cancellation.getReason();
            if (cancellation.getReason() == CancellationReason.Error) {
                msg = msg + ";errorCode=" + cancellation.getErrorCode() + ";errorDetails=" + cancellation.getErrorDetails();
            }

            resultInfo.setResult(ResultInfo.CANCELED_ERROR_CODE, msg);
        } else {
            resultInfo.setResult(ResultInfo.SUCCESS_CODE, "ResultReason=" + reason);
        }
        result.close();
        logger.info(resultInfo.getInfo());

        return resultInfo;
    }


    //处理转文本为语音输出
    public ResultInfo handleText2speech(String textContent) {
        ResultInfo resultInfo = new ResultInfo();
        List<String> filteredList = new ArrayList<>();

        for (int i = 0; i < retryTimes; i++) {
            resultInfo = handleText2speech(textContent, filteredList);
            String endpointName = resultInfo.getEndpointName();
            if (resultInfo.success()) {
                Long failCnt = 1L;
                if(endpointCounterMap.containsKey(endpointName)) {
                    failCnt += endpointCounterMap.get(endpointName);
                }
                endpointCounterMap.put(endpointName, failCnt);
                return resultInfo;
            } else {
                if (resultInfo.error()) {
                    filteredList.add(resultInfo.getEndpointName());
                    Long failCnt = 1L;
                    if(endpointFailCounterMap.containsKey(endpointName)) {
                        failCnt += endpointFailCounterMap.get(endpointName);
                    }
                    endpointFailCounterMap.put(endpointName, failCnt);
                } else {
                    break;
                }
            }
        } //-->for(...)

        return resultInfo;
    }

    private ResultInfo handleText2speech(String textContent, List<String> filteredList) {
        // Creates an instance of a speech synthesizer using speech configuration with specified
        // subscription key and service region and default speaker as audio output.
        ResultInfo resultInfo = new ResultInfo();
        SpeechEndpointBean endpointBean = checkEndpointService.chooseOne(getChooseMode(), filteredList);
        if (endpointBean == null) {
            resultInfo.setResult(ResultInfo.FAIL_CODE, "No valid endpoint to choose");
            return resultInfo;
        }
        resultInfo.setEndpointName(endpointBean.getName());

        SpeechConfig speechConfig = intSpeechConfig(endpointBean);
        if (speechConfig == null) {
            resultInfo.setResult(ResultInfo.FAIL_CODE, "speechConfig is null");
            return resultInfo;
        }
        //textContent = "pleae test any text that you want to speak here.now ready go....";

        try {
            SpeechSynthesizer synth = new SpeechSynthesizer(speechConfig);
            assert (synth != null);

            Future<SpeechSynthesisResult> task = synth.SpeakTextAsync(textContent);
            assert (task != null);

            SpeechSynthesisResult result = task.get();
            assert (result != null);

            // Checks result.
            String msg = "";
            ResultReason resultReason = result.getReason();
            if (resultReason == ResultReason.SynthesizingAudioCompleted) {
                msg = String.format("*** synthesized OK [%s]", textContent);
            } else if (resultReason == ResultReason.Canceled) {
                SpeechSynthesisCancellationDetails cancellation = SpeechSynthesisCancellationDetails.fromResult(result);
                msg = "*** CANCELED: Reason=" + cancellation.getReason();
                if (cancellation.getReason() == CancellationReason.Error) {
                    msg = msg + String.format(";ErrorCode=%s; ErrorDetails=%s",  cancellation.getErrorCode().toString(), cancellation.getErrorDetails());
                    resultInfo.setCode(ResultInfo.CANCELED_ERROR_CODE);
                }
            } else {
                msg = "resultReason:" + resultReason;
            }

            resultInfo.setInfo(msg);
            logger.info(msg);
        } catch (Exception ex) {
            ex.printStackTrace();
            resultInfo.setResult(ResultInfo.FAIL_CODE, "exception" + ex.getMessage());
        }

        return resultInfo;
    }


}
