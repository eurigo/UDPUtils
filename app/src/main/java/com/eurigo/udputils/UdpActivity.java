package com.eurigo.udputils;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.blankj.utilcode.util.NetworkUtils;
import com.blankj.utilcode.util.TimeUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.eurigo.udplibrary.UDPUtils;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;

/**
 * @author Eurigo
 * Created on 2021/7/1 9:27
 * desc   :
 */
public class UdpActivity extends AppCompatActivity implements
        View.OnClickListener, UDPUtils.OnUdpReceiveListener {

    private LogAdapter mAdapter;
    private MaterialButton btnSend, btnReceive;
    private EditText etSendHost, etSendPort, etSendContent, etReceivePort;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.udp_receive_activity);
        initView();
        mAdapter.addDataAndScroll("本机WIFI地址:  "+ NetworkUtils.getIpAddressByWifi());
        mAdapter.addDataAndScroll("本机Ipv4地址:  "+ NetworkUtils.getIPAddress(true));
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
                if (!UDPUtils.getInstance().isIpAddress(host)){
                    ToastUtils.showShort("请输入正确的IP地址！");
                    return;
                }
                if (!TextUtils.isEmpty(host) && !host.equals(UDPUtils.getInstance().getCurrentHost())){
                    UDPUtils.getInstance().setUdpHost(host);
                }
                // 重置目标端口
                if (!TextUtils.isEmpty(getEditText(etSendPort))){
                    int port = Integer.parseInt(getEditText(etSendPort));
                    if (port != UDPUtils.getInstance().getCurrentPort()) {
                        UDPUtils.getInstance().setUdpPort(port);
                    }
                }
                if (TextUtils.isEmpty(getEditText(etSendContent))){
                    ToastUtils.showShort("发送内容不能为空");
                    return;
                }
                UDPUtils.getInstance().sendMessage(getEditText(etSendContent));
                mAdapter.addDataAndScroll(TimeUtils.getNowString()
                        +"\n已向目标【"+UDPUtils.getInstance().getCurrentHost()
                        +":"+UDPUtils.getInstance().getCurrentPort()+"】发送数据: "
                        + getEditText(etSendContent));
                break;
            case R.id.btn_udp_receive:
                if ("开始接收".equals(btnReceive.getText().toString())){
                    mAdapter.addDataAndScroll("开始接收"
                            + UDPUtils.getInstance().getCurrentPort() +"端口数据包...");
                    UDPUtils.getInstance().startUDPSocket();
                    UDPUtils.getInstance().setReceiveListener(this);
                    btnReceive.setText("停止接收");
                    // 重置接收的监听端口
                    if (!TextUtils.isEmpty(etReceivePort.getText())){
                        UDPUtils.getInstance().setUdpPort(Integer.parseInt(getEditText(etReceivePort)));
                        mAdapter.addDataAndScroll("监听端口变更为："+ getEditText(etReceivePort));
                    }
                }else {
                    UDPUtils.getInstance().stopUDPSocket();
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
        UDPUtils.getInstance().stopUDPSocket();
    }

    private String getEditText(EditText editText){
        return editText.getText().toString().trim();
    }
}
