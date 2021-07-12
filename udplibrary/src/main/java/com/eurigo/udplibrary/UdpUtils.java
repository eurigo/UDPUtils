package com.eurigo.udplibrary;

import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
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

    public void startUdpSocket() {
        if (client != null) {
            return;
        }
        try {
            // Socket接收数据监听的端口，默认为9090
            client = new DatagramSocket(getCurrentPort());
            if (receivePacket == null) {
                receivePacket = new DatagramPacket(receiveByte, BUFFER_LENGTH);
            }
            startSocketThread();
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public static final String DEFAULT_SOCKET_HOST = "192.168.42.129";
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
        return TextUtils.isEmpty(DEFAULT_SOCKET_HOST)? DEFAULT_SOCKET_HOST : udpHost;
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

    /**
     * 处理接受到的消息
     **/
    private void receiveMessage() {
        while (isThreadRunning) {
            if (client != null) {
                try {
                    client.receive(receivePacket);
                } catch (IOException e) {
                    Log.e(TAG, "UDP数据包接收失败！线程停止");
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
                }catch (Exception e){
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
        executorService = new ThreadPoolExecutor(2 * CPU_COUNT + 1
                , 2 * CPU_COUNT + 1
                , 30
                , TimeUnit.SECONDS
                , new LinkedBlockingQueue<>()
                , new UdpThreadFactory("io", Thread.NORM_PRIORITY));
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                isThreadRunning = true;
                Log.e(TAG, "UDP clientThread is running...");
                receiveMessage();
            }
        });
    }

    /**
     * 发送消息
     *
     * @param message 消息文本
     */
    public void sendMessage(final String message) {
        if (client == null) {
            startUdpSocket();
        }
        Log.e(TAG, "发送的消息：" + message);
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    InetAddress targetAddress = InetAddress.getByName(getCurrentHost());
                    DatagramPacket packet = new DatagramPacket(message.getBytes()
                            , message.getBytes().length
                            , targetAddress, getCurrentPort());
                    client.send(packet);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * 发送json数据
     * @param map 数据Map
     */
    public void sendMessage(Map<String, String> map){
        if (client == null) {
            startUdpSocket();
        }
        JsonObject object = new JsonObject();
        // 包装Json
        for (String key : map.keySet()){
            object.addProperty(key, map.get(key));
        }
        Log.e(TAG, "发送的消息："+object.toString());
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    InetAddress targetAddress = InetAddress.getByName(getCurrentHost());
                    DatagramPacket packet = new DatagramPacket(object.toString().getBytes()
                            , object.toString().getBytes().length
                            , targetAddress, getCurrentPort());
                    client.send(packet);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
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
    }

    /**
     * 是否为Ip地址
     *
     * @param regexString 匹配的字符串
     * @return 是否是IP
     */
    public boolean isIpAddress(String regexString) {
        return regexString != null && regexString.length() > 0 && Pattern.matches(REGEX_IP, regexString);
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
        private static final long serialVersionUID = -9209200509960368598L;
        private final String namePrefix;
        private final int priority;
        private final boolean isDaemon;

        UdpThreadFactory(String prefix, int priority) {
            this(prefix, priority, false);
        }

        UdpThreadFactory(String prefix, int priority, boolean isDaemon) {
            namePrefix = prefix + "-pool-" +
                    POOL_NUMBER.getAndIncrement() +
                    "-thread-";
            this.priority = priority;
            this.isDaemon = isDaemon;
        }

        @Override
        public Thread newThread(@NonNull Runnable r) {
            Thread t = new Thread(r, namePrefix + getAndIncrement()) {
                @Override
                public void run() {
                    try {
                        super.run();
                    } catch (Throwable t) {
                        Log.e("ThreadUtils", "Request threw uncaught throwable", t);
                    }
                }
            };
            t.setDaemon(isDaemon);
            t.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread t, Throwable e) {
                    System.out.println(e);
                }
            });
            t.setPriority(priority);
            return t;
        }
    }
}
