package com.eurigo.udputils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.LinkProperties;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.NetworkUtils;
import com.blankj.utilcode.util.TimeUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.blankj.utilcode.util.Utils;
import com.eurigo.udplibrary.UdpUtils;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;

/**
 * @author Eurigo
 * Created on 2021/7/1 9:27
 * desc   :
 */
public class UdpActivity extends AppCompatActivity implements
        View.OnClickListener, UdpUtils.OnUdpReceiveListener {

    private LogAdapter mAdapter;
    private MaterialButton btnSend, btnReceive;
    private EditText etSendHost, etSendPort, etSendContent, etReceivePort;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.udp_receive_activity);
        initView();
        mAdapter.addDataAndScroll("本机WiFi地址:  " + NetworkUtils.getIpAddressByWifi());
        mAdapter.addDataAndScroll("本机Ipv4地址:  " + NetworkUtils.getIPAddress(true));
        mAdapter.addDataAndScroll("本机广播地址:  " + UdpUtils.getInstance().getBroadcastHost(this));
    }
    
    private void initView(){
        btnReceive = findViewById(R.id.btn_udp_receive);
        btnSend = findViewById(R.id.btn_udp_send);
        btnReceive.setOnClickListener(this);
        btnSend.setOnClickListener(this);
        etSendHost = findViewById(R.id.et_send_host);
        etSendPort = findViewById(R.id.et_send_port);
        etSendContent = findViewById(R.id.et_send_content);
        etReceivePort = findViewById(R.id.et_receive_port);
        
        RecyclerView mRecyclerView = findViewById(R.id.rcv_udp_receive);
        mAdapter = new LogAdapter(new ArrayList<>());
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(mAdapter);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_udp_send:
                // 重置目标IP
                String host = getEditText(etSendHost);
                if (!UdpUtils.getInstance().isIpAddress(host)) {
                    ToastUtils.showShort("请输入正确的IP地址！");
                    return;
                }
                if (!TextUtils.isEmpty(host) && !host.equals(UdpUtils.getInstance().getCurrentHost())) {
                    UdpUtils.getInstance().setUdpHost(host);
                }
                // 重置目标端口
                if (!TextUtils.isEmpty(getEditText(etSendPort))) {
                    int port = Integer.parseInt(getEditText(etSendPort));
                    if (port != UdpUtils.getInstance().getCurrentPort()) {
                        UdpUtils.getInstance().setUdpPort(port);
                    }
                }
                if (TextUtils.isEmpty(getEditText(etSendContent))) {
                    ToastUtils.showShort("发送内容不能为空");
                    return;
                }
                UdpUtils.getInstance().sendMessage(getEditText(etSendContent));
                mAdapter.addDataAndScroll(TimeUtils.getNowString()
                        + "\n已向目标【" + UdpUtils.getInstance().getCurrentHost()
                        + ":" + UdpUtils.getInstance().getCurrentPort() + "】发送数据: "
                        + getEditText(etSendContent));
                break;
            case R.id.btn_udp_receive:
                if ("开始接收".equals(btnReceive.getText().toString())) {
                    mAdapter.addDataAndScroll("开始接收"
                            + UdpUtils.getInstance().getCurrentPort() + "端口数据包...");
                    UdpUtils.getInstance().startUdpSocket();
                    UdpUtils.getInstance().setReceiveListener(this);
                    btnReceive.setText("停止接收");
                    // 重置接收的监听端口
                    if (!TextUtils.isEmpty(etReceivePort.getText())) {
                        UdpUtils.getInstance().setUdpPort(Integer.parseInt(getEditText(etReceivePort)));
                        mAdapter.addDataAndScroll("监听端口变更为：" + getEditText(etReceivePort));
                    }
                }else {
                    UdpUtils.getInstance().stopUdpSocket();
                    btnReceive.setText("开始接收");
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onReceived(String data) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAdapter.addDataAndScroll(TimeUtils.getNowString()
                        .concat("\n接收到数据>>>".concat(data)));
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        UdpUtils.getInstance().stopUdpSocket();
    }

    private String getEditText(EditText editText){
        return editText.getText().toString().trim();
    }
}
