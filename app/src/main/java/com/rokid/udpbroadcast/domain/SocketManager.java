package com.rokid.udpbroadcast.domain;

import android.content.Context;
import android.net.wifi.WifiManager;

import com.rokid.udpbroadcast.utils.Logger;
import com.rokid.udpbroadcast.view.DeviceBean;

import java.net.InetAddress;
import java.security.InvalidParameterException;
import java.util.Vector;

public class SocketManager implements ISocketCallback{

    // 保存当前UDP线程是服务端，还是客户端
    public static Integer MODE_SERVER = 1;
    public static Integer MODE_CLIENT = 2;
    private int mode;

    private Context mContext;

    private WifiManager.MulticastLock mMulticastLock;

    // 单实例
    private static SocketManager mInstance = null;
    // 组播的IP地址
    public final static String UDP_IP = "228.5.6.7";//"239.9.9.1";
    // 组播的Port
    public final static Integer UDP_PORT = 6789; //5761;//17375;
    // tcp的Port
    public final static Integer TCP_PORT = 6761;//17375;

    // 为了避免端口被占用，这里定义一个变量可以循环搜索可以试用的端口
    public Integer portServer = TCP_PORT;

    // UDP线程
    private UDPServer threadUDPServer;
    private TCPServer threadTCPServer;
    private TCPConnection threadTCPClient; // 存在客户端，可以给服务端发送消息

    // wifi管理器
//    private WifiManager wifiManager;

    // 保存设备
    private Vector<DeviceBean> mDevices;

    private SocketManager() {
        this.mDevices = new Vector<>();
    }

    public static SocketManager getInstance() {
        if (mInstance == null) {
            synchronized (SocketManager.class){
                mInstance = new SocketManager();
            }
        }
        return mInstance;
    }

//
//    public void init(Context context, int mode) {
//        this.mode = mode;
//        if (mode != MODE_SERVER && mode != MODE_CLIENT) {
//            throw new InvalidParameterException();
//        }
//        mContext = context;
////    }
//
//    public void exit() {
//
//    }


    @Override
    public void onUDPClientPing(InetAddress clientIp) {
        Logger.d("service收到登录请求，发送服务端TCP信息给client , clientIp="+clientIp.getHostAddress());
        threadUDPServer.sendMessage(clientIp, Message.MSG_UDP_S2C_HOST_INFO+"|"+this.portServer);
    }

    @Override
    public void onUDPServerHostInfo(String hostTcpIp, int hostTcpPort) {
        Logger.d("客户端收到服务端发过来的TCP端口信息， port="+hostTcpIp+", message="+hostTcpPort);
        // 这里的hostTcpIp 就是服务器的IP地址
        try {
            serverTCPConnect(hostTcpIp, hostTcpPort);
        } catch (Exception e) {
            e.printStackTrace();
            Logger.d("SocketManager eventbus onRecvEvent : MSG_UDP_S2C_HOST_INFO serverTCPConnect e="+e);
        }
    }

    @Override
    public void onTCPServerAddDevice(DeviceBean device) {
        mDevices.add(device);
        // 收到新设备，更新UI
        Logger.d("[TCPServer] 增加新设备 device="+device);
    }


    /************************************
     *          UDP 相关操作
     ************************************/
    public void udpServerStart(Context context, int mode) {
        this.mode = mode;
        if (mode != MODE_SERVER && mode != MODE_CLIENT) {
            throw new InvalidParameterException();
        }
        this.mContext = context;

        WifiManager wifiManager=(WifiManager)mContext.getSystemService(Context.WIFI_SERVICE);
        mMulticastLock = wifiManager.createMulticastLock("multicast.test");
        mMulticastLock.acquire();

        // 创建server端的UDP线程
        Logger.e("[server] UDP Start running--->multicastLock.isHeld="+mMulticastLock.isHeld());
        udpServerStop();

        this.threadUDPServer = new UDPServer(this, mode);
        this.threadUDPServer.start();
    }

    // 对于client端来说，启动客户端后，会发送一个PING请求信息
//    public void clientUDPFind() {
//        Logger.d("[client] UDP Start running--->");
//        udpServerStop();
//
//        this.threadUDPServer = new UDPServer(this, mode);
//        this.threadUDPServer.start();
//
//        Logger.d("[client] client Sent PING--->");
//        this.threadUDPServer.sendBroadcast(Message.MEG_UDP_C2S_PING);
//    }

    // 对于server & client 都是停止UDP服务, close之后一直等待线程 not alive!
    public void udpServerStop() {
        if (this.threadUDPServer != null && this.threadUDPServer.isAlive()) {
            this.threadUDPServer.close();
            while (this.threadUDPServer.isAlive()) {
                ;
            }
        }
        this.threadUDPServer = null;

        if (mMulticastLock != null) {
            try {
                mMulticastLock.release();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        Logger.d("All UPD Closed!!!!");
    }


    /************************************
     *          TCP 相关操作
     ************************************/
    // 服务端建立TCP server
    public void serverTCPStart() {
        // Clear previous connections (if any)
        Logger.d("[TCPServer] First stop the TCP connect--------");
        serverTCPStop();

        // 服务器断开所有玩家的TCP连接
        serverTCPDisconnectClients();

        this.mDevices = new Vector<>();

        // Creating server
        this.threadTCPServer = new TCPServer(this);
        this.threadTCPServer.start();

        Logger.d("thread started");

        // Waiting for the server to start
        while (!this.threadTCPServer.isAlive()) {
            ;
        }
        Logger.d("thread alive");
        while (!this.threadTCPServer.isListening()) {
            ;
        }
        Logger.d("thread listening");

        // Connecting like a normal client
        // 这里其实就是把服务器也当作一个玩家，加入。 这样在新游戏等待界面就会显示当前游戏的信息
        //serverTCPConnect("127.0.0.1", TCP_PORT); //TODO 不需要去连接自己服务端的TCP

        Logger.d("connected");
    }

    public void serverTCPStop() {
        Logger.d("TCPServer starting close");

        if (this.threadTCPServer != null && this.threadTCPServer.isAlive()) {
            this.threadTCPServer.close();
            while (this.threadTCPServer.isAlive()) {
                ;
            }
        }
        this.threadTCPServer = null;
        Logger.d("TCPServer Closed");
    }


    /** 服务端断开所有玩家连接 */
    public void serverTCPDisconnectClients() {

        Logger.d("Starting disconnection");
        // Close all the remaining connections
        for (DeviceBean p : mDevices) {
            Logger.d("inside for");
            p.close();
        }

        Logger.d("Closed all sockets, clearing...");

        mDevices.clear();

        Logger.d("Disconnection ended");
    }


    /**
     * Connects to the specified server.
     *
     * @param ip
     *            The IP of the server
     * @param port
     *            The port of the server
     * @return The player id and the team id assigned by the server as a string
     *         like 7|3
     * @throws Exception
     */
    public void serverTCPConnect(String ip, int port) throws Exception {
        Logger.d("[ClientTCP] 客户端去连接TCP服务器 ip="+ip+", port="+port);

        serverTCPDisconnect();
        // 每个客户端都保存有一个TCP Connection的线程
        this.threadTCPClient = new TCPConnection(ip, port);
        this.threadTCPClient.start();
        while (!this.threadTCPClient.isAlive()) {
            ;
        }

        // 线程名称， 这里是客户端，所以知道自己玩家的名称
        //this.threadTCPClient.setName("TCP client " + SocketManager.getInstance().getPlayerName());
        // The first thing we send is the player name
        // 连接后，向主机服务器发送玩家名称

        //this.threadTCPClient.out(SocketManager.getInstance().getPlayerName()); // 给服务器发送
        // The first thing we receive is the player id --- 阻塞读取id
        //Logger.d("read the player_assigned_info start");
        //String player_assigned_info = this.threadTCPClient.in(); // TODO 这里不需要读取
        //Logger.d("read the player_assigned_info OK  player_assigned_info="+player_assigned_info);
    }


    /** 客户端某个具体玩家去断开连接 */
    public void serverTCPDisconnect() {
        Logger.d("Disconnecting TCP");
        if (this.threadTCPClient != null && this.threadTCPClient.isAlive()) {
            // 调用客户端的close函数
            this.threadTCPClient.close();
            while (this.threadTCPClient.isAlive()) {
                ;
            }
        }
        this.threadTCPClient = null;
    }


    public void disconnectionDetected(TCPConnection conn) {
        if (mode == MODE_CLIENT) {
            // If in client mode, notify the UI about the shutdown
            //shutdownUI(); // 一个用户端断开，首先自己的TCPConnection会异常，然后跑到这里去更新UI，就是退出游戏界面
        } else if (mode == MODE_SERVER) {
            // This happens when there is a disconnection in the WaitingRoom
            //NetServer.getInstance().removePlayer(conn);
            //updatedPlayers();
        }
    }



    public int getDevicesCount() {
        return mDevices.size();
    }

    public void removePlayer(TCPConnection c) {
        for (DeviceBean p : mDevices) {
            if (p.getConnection().equals(c)) {
                this.mDevices.remove(p);
            }
        }
    }


    public void sendToServer(String string) throws Exception {
        Logger.d("sending to server " + string);
        threadTCPClient.out(string);
    }

    public void sendToClient(Integer client, String string) throws Exception {
        Logger.d("sending to player " + client + " " + string);
        mDevices.get(client).out(string);
    }

}
