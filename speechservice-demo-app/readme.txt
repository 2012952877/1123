
1. 编译  测试环境用-pDev 或 生产环境用-pProd
mvn clean compile package -Pdev -DskipTests
编译后的jar 文件位于当前工作目录 ./target下

2. 运行spring boot 程序:
java -jar ${workdir}/target/${jar} --spring.profiles.active=fat --server.port=8090
如shell命令行下 （指定了一个服务服务port）
java -jar target/<>-speechdemo-app-1.0.0-SNAPSHOT.jar --server.port=8090
或直接在idea 开发环境中运行

3.接口调用说明

3.1) 语音文件转文本接口:   api/speech/wav2text
post
http://localhost:8090/api/speech/wav2text
reqeust:
指定一个wav文件
{"filePath":"C:\\Users\\zhadeng\\deng\\msWork\\<>-speechservice-demo-app\\wavSample\\myVoiceIsMyPassportVerifyMe01.wav"}

3.2) 语音合成文本接口:   api/speech/recognitionWithLangAsync
post
http://localhost:8090/api/speech/recognitionWithLangAsync
reqeust:
{}
语音通过microphone说话输入

3.2) 把文本合成为语音输出:   api/speech/text2speech
post
http://localhost:8090/api/speech/text2speech
reqeust:
content:输入一串待合成的文本,如
{"content":"You don't have any recent projects yet"}


4. 关于配置文件
配置文件在工程的resource下,application.properties为公用的配置，现主要配置有spring acutuator相关的配置项，app.conf分开发环境(dev)和开发环境(prod),在编译时指定一个profile会启用哪个环境
app.conf为typesafe格式配置文件


#mode=1 按配置的权重选择，0--随机选择
choose.mode = 1

#endpoints配置speech service的azure endpoint,可多个, 并须配置weight权重系数
endpoints = [
{
name = zhaSpeechTest
subscriptionKey = 3b23296f40af4d5ea38facb428e584fd
serviceRegion = eastus
weight = 80
}
}

{
name = zhaSpeechTestEastUs2
subscriptionKey = e354a179c9514c09a25bc5fb52efe6e8
serviceRegion = eastus2
weight = 20
}
]

5.程序说明
1）sprint boot服务启动后，有个默认定时器检查可能的endpoint
SchedulerCheckTask, 暂设置为每1分钟检查一次，可视情况加以参数调整
  如  @Scheduled(cron = "0 */1 * * * *")
2）api调用时，会按权重随机或简单随机（视choose.mode）选择一个endpint
同时支持重试机制即endpoint 连接error时会重试， retryTime=2

3) 工程目录结构
├───src
│   ├───main
│   │   ├───java
│   │   │   └───com
│   │   │       └───microsoft
│   │   │           └───speech
│   │   │               └───<>
│   │   │                   ├───config
│   │   │                   ├───controller
│   │   │                   ├───dto
│   │   │                   ├───entity
│   │   │                   ├───service
│   │   │                   ├───stream
│   │   │                   ├───task
│   │   │                   └───utils
│   │   └───resources
│   │       ├───dev
│   │       └───prod
│   └───test
│       └───java
│           └───com
│               └───microsoft
│                   └───speech
│                       └───<>

wavSample  目录下含有两个测试用的wav文件.


主要模块说明:
程序主入口为 Application
config/AppConfig 加载speech service endpoints相关的配置文件 app.conf
controller为 接口控制类
service 下为接口实现调用的服务类，实现具体业务逻辑, service/SpeechMonitor类提供自定义Health监控以增加了各语音服务endpoint调用成功/失败的次数
task 下SchedulerCheckTask类为定时任务检查,每隔x(1)分钟检查语音endpoint的可用列表
utils/WeightRandom 实现按权重随机选择功能

5.健康监控
http://localhost:8090/actuator/health
{"status":"UP","components":{"diskSpace":{"status":"UP","details":{"total":1022870155264,"free":862126321664,"threshold":10485760,"exists":true}},"ping":{"status":"UP"},"speechMonitor":{"status":"UP","details":{"endpoints.success.counter":"{\"mi-asr1\":4}"}}}}

speechMonitor为相关的各speechserice endpoint的调用成功/失败次数
