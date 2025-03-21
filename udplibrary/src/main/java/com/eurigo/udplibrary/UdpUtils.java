package com.eurigo.udplibrary;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

/**
 * @author Eurigo
 * Created on 2021/7/1 14:43
 * desc   : UDP传输工具类
 */
public class UdpUtils {

    private static final String TAG = "UdpUtils";

    public UdpUtils() {
    }

    public static UdpUtils getInstance() {
        return SingletonHelper.INSTANCE;
    }

    /**
     * 打开UDP线程
     */
    public void startUdpSocket() {
        if (client != null) {
            return;
        }
        try {
            // Socket接收数据监听的端口，默认为9090
            client = new DatagramSocket(null);
            client.setReuseAddress(true);
            client.bind(new InetSocketAddress(getCurrentPort()));
            if (receivePacket == null) {
                receivePacket = new DatagramPacket(receiveByte, BUFFER_LENGTH);
            }
            startSocketThread();
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    /**
     * 停止UDP
     **/
    public void stopUdpSocket() {
        isThreadRunning = false;
        receivePacket = null;
        if (client != null) {
            client.close();
            client = null;
        }
        onUdpReceiveListener = null;
        try {
            executorService.shutdownNow();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 重启UDP服务
     **/
    public void restartUdpSocket() {
        stopUdpSocket();
        startUdpSocket();
    }

    /**
     * 有些路由器/Wi-Fi热点不支持255.255.255.255广播地址（例如：用Android手机做Wi-Fi热点的时候）
     */
    public static final String DEFAULT_SOCKET_HOST = "255.255.255.255";
    public static final int DEFAULT_SOCKET_UDP_PORT = 9090;

    private String udpHost = "";
    private int udpPort = 0;

    public void setUdpHost(String udpHost) {
        this.udpHost = udpHost;
    }

    public void setUdpPort(int udpPort) {
        this.udpPort = udpPort;
    }

    public String getCurrentHost() {
        return TextUtils.isEmpty(udpHost) ? DEFAULT_SOCKET_HOST : udpHost;
    }

    public int getCurrentPort() {
        return udpPort == 0 ? DEFAULT_SOCKET_UDP_PORT : udpPort;
    }

    /**
     * 字符长度
     */
    private static final int BUFFER_LENGTH = 1024;
    /**
     * 根据字符长度创建字节数据
     */
    private final byte[] receiveByte = new byte[BUFFER_LENGTH];

    /**
     * IP正则匹配
     */
    public static final String REGEX_IP = "((2[0-4]\\d|25[0-5]|[01]?\\d\\d?)\\.){3}(2[0-4]\\d|25[0-5]|[01]?\\d\\d?)";

    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private boolean isThreadRunning = false;
    private OnUdpReceiveListener onUdpReceiveListener;

    private DatagramPacket receivePacket;
    private DatagramSocket client;
    private ExecutorService executorService;
    private WifiManager mWifiManager;

    private WifiManager getWifiManger(Context context) {
        if (mWifiManager == null) {
            mWifiManager = (WifiManager) context.getApplicationContext()
                    .getSystemService(Context.WIFI_SERVICE);
        }
        return mWifiManager;
    }

    /**
     * 处理接受到的消息
     **/
    private void receiveMessage() {
        while (isThreadRunning) {
            if (client != null) {
                try {
                    client.receive(receivePacket);
                } catch (IOException e) {
                    Log.e(TAG, "UDP Thread is Stop!!");
                    stopUdpSocket();
                    e.printStackTrace();
                    return;
                }
            }
            if (receivePacket == null || receivePacket.getLength() == 0) {
                Log.e(TAG, "无法接收UDP数据或者接收到的UDP数据为空");
                continue;
            }
            String strReceive = new String(receivePacket.getData(), 0
                    , receivePacket.getLength());
            Log.e(TAG, strReceive + " from "
                    + receivePacket.getAddress().getHostAddress() + ":" + receivePacket.getPort());
            if (onUdpReceiveListener != null) {
                try {
                    onUdpReceiveListener.onReceived(strReceive);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            // 每次接收完UDP数据后，重置长度。否则可能会导致下次收到数据包被截断。
            if (receivePacket != null) {
                receivePacket.setLength(BUFFER_LENGTH);
            }
        }
    }

    /**
     * 开启接收数据的线程
     **/
    private void startSocketThread() {
        executorService = new ThreadPoolExecutor(CPU_COUNT
                , 2 * CPU_COUNT + 1
                , 30
                , TimeUnit.SECONDS
                , new LinkedBlockingQueue<>()
                , new UdpThreadFactory());
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                isThreadRunning = true;
                Log.e(TAG, "UDP Thread is running...");
                receiveMessage();
            }
        });
    }

    /**
     * 自动获取广播地址并发送广播消息
     *
     * @param message 消息文本
     */
    public void sendBroadcastMessage(Context context, String message) {
        setUdpHost(getBroadcastHost(context));
        sendMessage(message);
    }

    /**
     * 发送全局广播
     *
     * @param map 数据Map
     */
    public void sendBroadcastMessage(Context context, Map<String, Object> map) {
        setUdpHost(getBroadcastHost(context));
        sendMessage(map);
    }

    /**
     * 自定义广播地址并发送广播消息
     *
     * @param message 消息文本
     */
    public void sendBroadcastInCustomHost(String host, String message) {
        setUdpHost(host);
        sendMessage(message);
    }

    /**
     * 自定义广播地址并发送广播消息
     *
     * @param map 数据Map
     */
    public void sendBroadcastInCustomHost(String host, Map<String, Object> map) {
        setUdpHost(host);
        sendMessage(map);
    }

    /**
     * 发送字节数组消息,注意提前设置目标地址
     *
     * @param message 消息文本
     */
    public void sendMessage(byte[] message) {
        if (client == null) {
            startUdpSocket();
        }
        executorService.execute(() -> {
            try {
                InetAddress targetAddress = InetAddress.getByName(getCurrentHost());
                DatagramPacket packet = new DatagramPacket(message
                        , message.length
                        , targetAddress, getCurrentPort());
                client.send(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * 发送消息，注意提前设置目标地址
     *
     * @param message 消息文本
     */
    public void sendMessage(final String message) {
        if (client == null) {
            startUdpSocket();
        }
        executorService.execute(() -> {
            try {
                InetAddress targetAddress = InetAddress.getByName(getCurrentHost());
                DatagramPacket packet = new DatagramPacket(message.getBytes()
                        , message.getBytes().length
                        , targetAddress, getCurrentPort());
                client.send(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * 发送json数据，注意提前设置目标地址
     *
     * @param map 数据Map
     */
    public void sendMessage(Map<String, Object> map) {
        if (client == null) {
            startUdpSocket();
        }
        JsonObject object = new JsonObject();
        // 包装Json
        for (String key : map.keySet()) {
            object.addProperty(key, String.valueOf(map.get(key)));
        }
        executorService.execute(() -> {
            try {
                InetAddress targetAddress = InetAddress.getByName(getCurrentHost());
                DatagramPacket packet = new DatagramPacket(object.toString().getBytes()
                        , object.toString().getBytes().length
                        , targetAddress, getCurrentPort());
                client.send(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * 获取广播IP地址
     * 有些路由器/Wi-Fi热点不支持255.255.255.255广播地址（例如：用Android手机做Wi-Fi热点的时候）
     * 会出现“ENETUNREACH (Network is unreachable)”的异常，因此，为了保证程序成功发送广播包，建议使用组播广播地址
     *
     * @param context 上下文
     * @return 广播IP地址
     */
    public String getBroadcastHost(Context context) {
        DhcpInfo dhcp = getWifiManger(context).getDhcpInfo();
        // 如果没有ip信息使用组播
        if (dhcp == null) {
            return DEFAULT_SOCKET_HOST;
        }
        // 优先使用子网广播地址
        if (dhcp.ipAddress != 0) {
            return getSubnetBroadcastAddress();
        }
        return DEFAULT_SOCKET_HOST;
    }

    /**
     * 获取Wifi网络下的子网广播地址
     *
     * @return 子网广播地址
     */
    public String getSubnetBroadcastAddress() {
        try {
            Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
            while (nis.hasMoreElements()) {
                NetworkInterface ni = nis.nextElement();
                // 活跃接口 + 非环回 + 名称匹配（如 wlan0）
                if (!ni.isUp() || ni.isLoopback() || !ni.getName().startsWith("wlan")) {
                    continue;
                }
                List<InterfaceAddress> ias = ni.getInterfaceAddresses();
                for (int i = 0, size = ias.size(); i < size; i++) {
                    InterfaceAddress ia = ias.get(i);
                    InetAddress broadcast = ia.getBroadcast();
                    if (broadcast != null) {
                        return broadcast.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return DEFAULT_SOCKET_HOST;
    }

    /**
     * 是否为Ip地址
     *
     * @param regexString 匹配的字符串
     * @return 是否是IP
     */
    public boolean isIpAddress(String regexString) {
        return regexString != null && !regexString.isEmpty() && Pattern.matches(REGEX_IP, regexString);
    }

    private static class SingletonHelper {
        private final static UdpUtils INSTANCE = new UdpUtils();
    }

    public interface OnUdpReceiveListener {
        /**
         * 接收到数据包，
         * 在{@link #receiveMessage} 中回调，！！请勿直接在OnReceived中操作UI，
         * 【如果必要，请使用{@link android.app.Activity#runOnUiThread(Runnable)}切换到UI线程】
         *
         * @param data 数据包
         */
        void onReceived(String data);
    }

    public void setReceiveListener(OnUdpReceiveListener onUdpReceiveListener) {
        this.onUdpReceiveListener = onUdpReceiveListener;
    }

    private static final class UdpThreadFactory extends AtomicLong
            implements ThreadFactory {
        private static final AtomicInteger POOL_NUMBER = new AtomicInteger(1);
        private final String namePrefix;
        private final int priority;
        private final boolean isDaemon;

        UdpThreadFactory() {
            this(TAG, Thread.NORM_PRIORITY, false);
        }

        UdpThreadFactory(String prefix, int priority, boolean isDaemon) {
            namePrefix = prefix + "-pool-" +
                    POOL_NUMBER.getAndIncrement() +
                    "-thread-";
            this.priority = priority;
            this.isDaemon = isDaemon;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, namePrefix + getAndIncrement()) {
                @Override
                public void run() {
                    try {
                        super.run();
                    } catch (Throwable e) {
                        Log.e(TAG, "UDP Thread run threw throwable", e);
                    }
                }
            };
            t.setDaemon(isDaemon);
            t.setPriority(priority);
            t.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread t, Throwable e) {
                    Log.e(TAG, t.getName() + "Request threw uncaught throwable", e);
                }
            });
            return t;
        }
    }
}
