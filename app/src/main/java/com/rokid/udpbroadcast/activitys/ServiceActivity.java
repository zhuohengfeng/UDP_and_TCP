package com.rokid.udpbroadcast.activitys;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.widget.TextView;
import android.widget.Toast;

import com.rokid.udpbroadcast.R;
import com.rokid.udpbroadcast.domain.DomainUtils;
import com.rokid.udpbroadcast.utils.Logger;

/**
 * 服务器---眼镜端---启动线程，等待手机端搜索到设备，并连接上
 */
public class ServiceActivity extends AppCompatActivity {

    private TextView mTvStatus;

    private Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            Bundle b = msg.getData();
            String type = b.getString("type");
            Logger.d("ServiceActivity type="+type);
            mTvStatus.setText(type);
//            if (type.equals("WAITINGROOM")) {
//                updateWaitingRoom(b); // 每次有新玩家加入，都会用一个WaitingRoom数据结构来通知所有玩家(包括服务器端的player)
//            } else if (type.equals("STARTGAME")) {
//                startGame(); // 用户选择开始游戏
//            } else if (type.equals("SHUTDOWN")) {
//                // 断开服务器
//                Toast.makeText(getBaseContext(), "The server was closed", Toast.LENGTH_SHORT).show();
//            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service);

        mTvStatus = findViewById(R.id.tv_status);
        mTvStatus.setText("眼镜端广播信息中....");
    }


    @Override
    protected void onStart() {
        super.onStart();

        Logger.d("ServiceActivity: start");

        // 这里把这个界面的handler传入，当有状态改变时，通过这个handler来改变此界面的UI状态
        DomainUtils.getInstance().setHandlerUI(handler);

        try {
            // 用户启动TCP线程
            DomainUtils.getInstance().serverTCPStart();
            // 用户启动UDP线程
            DomainUtils.getInstance().serverUDPStart();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getBaseContext(), "Couldn't create the server", Toast.LENGTH_SHORT).show();
            finish();
        }

        Logger.d("ServiceActivity: end");
    }

    @Override
    protected void onStop() {
        super.onStop();
        DomainUtils.getInstance().serverUDPStop();
        DomainUtils.getInstance().serverTCPStop();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            // 用户按返回，就断开TCP连接
            DomainUtils.getInstance().serverTCPDisconnectClients();
        }

        return super.onKeyDown(keyCode, event);
    }










//    // APP调用，看可以搜索到多少台设备
//    private void searchDevices_broadcast() {
//        new DeviceSearcher() {
//            // 开始搜索设备的回调
//            @Override
//            public void onSearchStart() {
//                pushMsgToMain("开始搜索设备中.....");
//            }
//
//            // 搜索完成的回调
//            @Override
//            public void onSearchFinish(final Set deviceSet) {
//                pushMsgToMain("设备搜索完成");
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        mDeviceListAdapter.clear();
//                        mDeviceListAdapter.addAll(deviceSet);
//                    }
//                });
//            }
//        }.start();
//    }
//
//    private void pushMsgToMain(final String s) {
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                if (mTvStatus != null) {
//                    mTvStatus.setText(s);
//                }
//            }
//        });
//    }




}

