package com.rokid.udpbroadcast.activitys;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.rokid.udpbroadcast.R;
import com.rokid.udpbroadcast.backup.DeviceWaitingSearch;
import com.rokid.udpbroadcast.domain.DomainUtils;
import com.rokid.udpbroadcast.view.DeviceListAdapter;

public class DeviceSearchActivity extends AppCompatActivity {

    private TextView mTvStatus;
    private ListView mDeviceListView;
    private DeviceListAdapter mDeviceListAdapter;

    private DeviceWaitingSearch mDeviceWaitingSearch;


    private Handler udpHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            // 当客户端搜索到一个新游戏后，列表显示游戏信息(游戏名称， 主机名， 主机端口)。
            // 在UDP线程中就是客户端收到服务端点对点发送的信息之后更新


            // 这里返回的是服务的信息--- 服务端的名称，IP， Port
            String serverName = msg.getData().getString("NAME");
            String serverIP = msg.getData().getString("IP");
            int serverPort = msg.getData().getInt("PORT");


            // 获取到服务端信息后，开始进入建立TCP socket连接
            try {
                // client点击某个具体游戏后才开始TCP连接
                DomainUtils.getInstance().setHandlerUI(udpHandler);
                DomainUtils.getInstance().serverTCPConnect(serverIP, serverPort);
            } catch (Exception e) {
                Toast.makeText(getBaseContext(), "Couldn't connect to the server "+serverName, Toast.LENGTH_SHORT).show();
                finish();
            }








//            DeviceBean device = null;
//            try {
//                device = new DeviceBean(null, 1);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//            device.setName(name);
//            device.setIp(ip);
//            device.setPort(port);
//            mDeviceListAdapter.add(device);
//            mDeviceListAdapter.notifyDataSetChanged();

//            child.setOnClickListener(new OnClickListener() {
//                @Override
//                public void onClick(View view) {
//                    view.setBackgroundColor(Color.YELLOW);
//                    Intent i = new Intent();
//                    i.setClass(view.getContext(), JoinGameWaitingActivity.class);
//                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                    i.putExtras( (Bundle)view.getTag() );
//                    startActivity(i);
//                    finish();
//                }
//            });

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_search);

        mDeviceListView = findViewById(R.id.list_devices);

        mDeviceListAdapter = new DeviceListAdapter(this);
        mDeviceListView.setAdapter(mDeviceListAdapter);

        mTvStatus = findViewById(R.id.tv_status);
//        mDeviceWaitingSearch = new DeviceWaitingSearch(this, "Glasses No.1", "Rokid"){
//            @Override
//            public void onDeviceSearched(InetSocketAddress socketAddr) {
//                pushMsgToMain("已上线，搜索主机：" + socketAddr.getAddress().getHostAddress() + ":" + socketAddr.getPort());
//            }

//        };
    }


    @Override
    protected void onStart() {
        super.onStart();
        mDeviceListAdapter.clear();
        // 通过UDP来搜索游戏
        DomainUtils.getInstance().serverUDPFind(udpHandler);
        // CtrlDomain.getInstance().createCleanBoard();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // 停止UDP搜索游戏
        DomainUtils.getInstance().serverUDPStop(); // TODO， 这个应该可以提前关闭

        DomainUtils.getInstance().serverTCPDisconnect();
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            DomainUtils.getInstance().serverTCPDisconnect();
        }

        return super.onKeyDown(keyCode, event);
    }




//    @Override
//    protected void onStart() {
//        super.onStart();
//        if (mDeviceWaitingSearch != null) {
//            mDeviceWaitingSearch.startRunning();
//        }
//    }
//
//    @Override
//    protected void onStop() {
//        super.onStop();
//        if (mDeviceWaitingSearch != null) {
//            mDeviceWaitingSearch.stopRunning();
//        }
//    }
//
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

