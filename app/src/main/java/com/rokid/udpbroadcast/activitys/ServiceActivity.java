package com.rokid.udpbroadcast.activitys;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.rokid.udpbroadcast.R;
import com.rokid.udpbroadcast.domain.SocketManager;
import com.rokid.udpbroadcast.utils.Logger;
import com.rokid.udpbroadcast.view.DeviceListAdapter;

/**
 * 服务器---眼镜端---启动线程，等待手机端搜索到设备，并连接上
 */
public class ServiceActivity extends AppCompatActivity {

    private TextView mTvStatus;

    private ListView mDeviceListView;
    private DeviceListAdapter mDeviceListAdapter;

//    private Handler handler = new Handler() {
//
//        @Override
//        public void handleMessage(Message msg) {
//            super.handleMessage(msg);
//
//            Bundle b = msg.getData();
//            String type = b.getString("type");
//            Logger.d("ServiceActivity type="+type);
//            mTvStatus.setText(type);
////            if (type.equals("WAITINGROOM")) {
////                updateWaitingRoom(b); // 每次有新玩家加入，都会用一个WaitingRoom数据结构来通知所有玩家(包括服务器端的player)
////            } else if (type.equals("STARTGAME")) {
////                startGame(); // 用户选择开始游戏
////            } else if (type.equals("SHUTDOWN")) {
////                // 断开服务器
////                Toast.makeText(getBaseContext(), "The server was closed", Toast.LENGTH_SHORT).show();
////            }
//        }
//    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service);

        mTvStatus = findViewById(R.id.tv_status);
        mTvStatus.setText("手机端: 监听设备连接中...");

        mDeviceListView = findViewById(R.id.list_devices);
        mDeviceListAdapter = new DeviceListAdapter(this);
        mDeviceListView.setAdapter(mDeviceListAdapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }



    @Override
    protected void onStart() {
        super.onStart();
        Logger.d("ServiceActivity: [Service] start the TCP & UDP server ----");

        // 这里把这个界面的handler传入，当有状态改变时，通过这个handler来改变此界面的UI状态
        //SocketManager.getInstance().setHandlerUI(handler);

        try {
            // 服务端启动UDP线程
            SocketManager.getInstance().udpServerStart(this, SocketManager.MODE_SERVER);
            // 服务端启动TCP线程
            SocketManager.getInstance().serverTCPStart();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getBaseContext(), "Couldn't create the server", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Logger.d("ServiceActivity: [Service] stop the TCP & UDP server ----");
        SocketManager.getInstance().serverTCPStop();
        SocketManager.getInstance().udpServerStop();
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

