# UDPUtils [![](https://jitpack.io/v/eurigo/UDPUtils.svg)](https://jitpack.io/#eurigo/UDPUtils)
Android UDP通讯工具类，支持Json格式传输。

+ 码云地址：[Gitee](https://gitee.com/Eurigo/UDPUtils)

### 集成使用

+ 在项目级 `build.gradle`添加

```
allprojects {
   repositories {
      maven { url 'https://jitpack.io' }
	}
}
```
  
+ 在app模块下的`build.gradle`文件中加入
```
dependencies {
    implementation 'com.github.eurigo:UDPUtils:1.0.2'
}
```

### API
+ #### 接收数据
```
// 设置监听端口，不设置默认为9090
UDPUtils.getInstance().setPort(9090);

// 打开Socket
UDPUtils.getInstance().startUDPSocket();

// 注册接收回调
UDPUtils.getInstance().setReceiveListener(new OnUdpReceiveListener);
```
+ #### 发送数据
```
// 设置数据接收方的端口
UDPUtils.getInstance().setPort(9090);

// 设置数据接收方的IP，255为广播形式发送
UDPUtils.getInstance().setHost(192.168.0.255);

// 发送文本
UDPUtils.getInstance().sendMessage(String s)

// 发送Json
UDPUtils.getInstance().sendMessage(Map map)
```
