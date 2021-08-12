# UDPUtils  [![](https://jitpack.io/v/eurigo/UDPUtils.svg)](https://jitpack.io/#eurigo/UDPUtils)

Android UDP通讯工具类，支持Json格式传输。

+ [Github](https://github.com/eurigo/UDPUtils)

+ [Gitee](https://gitee.com/Eurigo/UDPUtils)

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
    implementation 'com.github.eurigo:UDPUtils:1.1.0'
}
```

+ 在app模块下的AndroidManifest.xml添加权限
```
<manifest
    ...
    <uses-permission android:name="android.permission.INTERNET" />
/>
```

### API
+ #### 接收数据
```
// 设置监听端口，不设置默认为9090
UdpUtils.getInstance().setUdpPort(9090);

// 打开Socket
UdpUtils.getInstance().startUDPSocket();

// 注册接收回调
UdpUtils.getInstance().setReceiveListener(new OnUdpReceiveListener);
```
+ #### 发送数据
```
// 设置数据接收方的端口
UdpUtils.getInstance().setUdpPort(9090);

// 设置数据接收方的IP
UdpUtils.getInstance().setUdpHost(192.168.43.255);

// 不指定接收方IP，直接发送全局广播
UdpUtils.getInstance().sendBroadcastMessage(String s)

// 不指定接收方IP，直接发送全局广播
UdpUtils.getInstance().sendBroadcastMessage(Map map)

// 发送文本
UdpUtils.getInstance().sendMessage(String s)

// 发送Json
UdpUtils.getInstance().sendMessage(Map map)
```
+ #### 其他
```
// 获取广播地址
UdpUtils.getInstance().getBroadcastHost(Context context)

// 获取当前UDP发送地址
UdpUtils.getInstance().getCurrentHost(Context context)

// 获取当前UDP发送端口
UdpUtils.getInstance().getCurrentPort(Context context)
```
