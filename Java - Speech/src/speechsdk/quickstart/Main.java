//
// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See LICENSE.md file in the project root for full license information.
//
// <code>
package speechsdk.quickstart;

import java.util.*;
import java.text.SimpleDateFormat;
import java.util.concurrent.*;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;


import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import com.microsoft.cognitiveservices.speech.*;
import com.microsoft.cognitiveservices.speech.SpeechConfig;
import org.apache.http.util.EntityUtils;


/**
 * Quickstart: recognize speech using the Speech SDK for Java.
 */
public class Main {
    public static String speechSubscriptionKey = "c8e92314c7824ce08f12f098e6138b37";
    public static String serviceRegion = "southeastasia";
    public static boolean isCancel = false;
    public static SourceLanguageConfig sourceLanguageConfig = SourceLanguageConfig.fromLanguage("zh-CN");
    public static String auth = getToken();
    public static SpeechConfig config = SpeechConfig.fromAuthorizationToken(auth, serviceRegion);

    //Get new token from Speech service with 'speechSubscriptionKey'
    public static String getToken() {
        try {
            String url = "https://southeastasia.api.cognitive.microsoft.com/sts/v1.0/issuetoken";

            CloseableHttpClient httpClient = HttpClients.createDefault();
            HttpPost httpPost = new HttpPost(url);
            System.out.println(speechSubscriptionKey);
            httpPost.addHeader("Ocp-Apim-Subscription-Key", speechSubscriptionKey);

            CloseableHttpResponse response = httpClient.execute(httpPost);
            HttpEntity httpEntity = response.getEntity();
            String resStr = null;
            if (httpEntity != null) {
                resStr = EntityUtils.toString(httpEntity, "UTF-8");
                System.out.println("New token: " + resStr);
            }
            httpClient.close();
            response.close();
            return resStr;
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    //Schedule a task to renew token & speechRecognizer
    public static void startRenewTask(SpeechRecognizer speechRecognizer){

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        ScheduledFuture<?> renewToken =
                scheduler.scheduleWithFixedDelay(new Runnable() {
                    public void run() {
                        String newToken = getToken();
                        speechRecognizer.setAuthorizationToken(newToken);
                        System.out.println("Renew token : " + speechRecognizer.getAuthorizationToken());
                    };
                }, 10, 30, TimeUnit.SECONDS);

        //Stop task
        scheduler.schedule(new Runnable() {
            public void run() {
                if (isCancel){
                    renewToken.cancel(true);
                    System.out.println("Cancel Renew! -------------");
                }
            };
        }, 30, TimeUnit.SECONDS);
    }

    public static void startRecognize(SpeechRecognizer sr) throws ExecutionException, InterruptedException {
        // Subscribes to events.
        sr.recognizing.addEventListener((s, e) -> {
            System.out.println(new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss:SSS").format(new Date())
                    + " RECOGNIZING: Text=" + e.getResult().getText());
        });

        sr.recognized.addEventListener((s, e) -> {
            if (e.getResult().getReason() == ResultReason.RecognizedSpeech) {
                System.out.println(new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss:SSS").format(new Date())
                        + "RECOGNIZED: Text=" + e.getResult().getText());
            } else if (e.getResult().getReason() == ResultReason.NoMatch) {
                System.out.println(new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss:SSS").format(new Date())
                        + "NOMATCH: Speech could not be recognized.");
            }
        });

        sr.canceled.addEventListener((s, e) -> {
            System.out.println("CANCELED: Reason=" + e.getReason());

            if (e.getReason() == CancellationReason.Error) {
                System.out.println("CANCELED: ErrorCode=" + e.getErrorCode());
                System.out.println("CANCELED: ErrorDetails=" + e.getErrorDetails());
                System.out.println("CANCELED: Did you update the subscription info?");
            }
        });

        sr.sessionStarted.addEventListener((s, e) -> {
            System.out.println(new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss:SSS").format(new Date())
                    + "\n    Session started event.");
        });

        sr.sessionStopped.addEventListener((s, e) -> {
            System.out.println(new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss:SSS").format(new Date())
                    + "\n    Session stopped event.");
        });

        //Start recognition
        sr.startContinuousRecognitionAsync().get();

        System.out.println("Press any key to stop");
        new Scanner(System.in).nextLine();

        //Stop recognition
        sr.stopContinuousRecognitionAsync().get();
        sr.close();
    }
    /**
     * @param args Arguments are ignored in this sample.
     */
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        //start & stop recognition 3 times
        for (int n=0; n<3; n++){
            System.out.println("Please input 2 to start a recognition ...");
            Scanner scannerRe = new Scanner(System.in);
            int textRe = scannerRe.nextInt();

            if(textRe == 2){
                SpeechRecognizer spReco = new SpeechRecognizer(config, sourceLanguageConfig);
                startRenewTask(spReco);
                startRecognize(spReco);
            }
        }

        System.out.println("Press 3 to stop renew task...");
        Scanner sc3 = new Scanner(System.in);
        int text3 = sc3.nextInt();

        if(text3 == 3){
            isCancel = true;
        }
        /*int exitCode = 1;
        System.exit(exitCode);*/
    }
}
