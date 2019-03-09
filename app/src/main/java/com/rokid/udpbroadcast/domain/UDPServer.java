package com.rokid.udpbroadcast.domain;

import com.rokid.udpbroadcast.utils.Logger;
import com.rokid.udpbroadcast.utils.Utils;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.Timer;
import java.util.TimerTask;

public class UDPServer extends Thread {
    // 组播UDP
    private MulticastSocket multicastSocket;

    // 线程运行控制flag
    private Boolean keepRunning;

    private ISocketCallback mCallback;

    private int mMode;

    private Timer timer = new Timer();
    private TimerTask task;


    public UDPServer(ISocketCallback callback, int mode) {
        super("UPDServer");
        this.mCallback = callback;
        this.mMode = mode;
        this.keepRunning = true;
        try {
            Logger.d("[UDPServer] start UDPServer now port:"+SocketManager.UDP_PORT);

            Enumeration<NetworkInterface> enumeration = NetworkInterface.getNetworkInterfaces();
            NetworkInterface eth0 = null;
            while (enumeration.hasMoreElements()) {
                eth0 = enumeration.nextElement();
                if (eth0.getName().equals("eth0")) {
                    //there is probably a better way to find ethernet interface
                    break;
                }
            }

            // 创建组播方式的UDP socket
            multicastSocket = new MulticastSocket(SocketManager.UDP_PORT);
            //设置本MulticastSocket发送的数据报会被回送到自身
            multicastSocket.setLoopbackMode(true);
            multicastSocket.setNetworkInterface(NetworkInterface.getByName("wlan0"));

            InetAddress address = InetAddress.getByName(SocketManager.UDP_IP);
            multicastSocket.joinGroup(new InetSocketAddress(address, SocketManager.UDP_PORT), eth0);

            // 如果是客户端，定时广播发送心跳数据
            if (mMode == SocketManager.MODE_CLIENT) {
                /*发送心跳数据*/
                sendClientPing();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Logger.e("[UDPServer] Exception creating socket: "+e);
            try {
                if(null!=multicastSocket && !multicastSocket.isClosed()){
                    multicastSocket.leaveGroup(InetAddress.getByName(SocketManager.UDP_IP));
                    multicastSocket.close();
                }
            } catch (Exception e1) {
                Logger.e("[UDPServer] Exception creating socket"+e1);
                e.printStackTrace();
            }
        }
    }

    @Override
    public void run() {
        try {
            byte[] buf = new byte[1024];

            while (keepRunning && multicastSocket != null && !multicastSocket.isClosed()) {
                Logger.d("[UDPServer] Inside while");

                // 这里会阻塞住，一直等待新的报文传递过来！！！
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                multicastSocket.receive(packet);
                Logger.d("[UDPServer] Datagram received");

                // 是谁发过来的?
                InetAddress remoteIP = packet.getAddress();

                // 判断消息是不是自己发送的
                if ((remoteIP.getHostAddress()).equals(Utils.getLocalAddress().getHostAddress())){
                    Logger.d("[UDPServer] 发给自己了....return");
                    continue;
                }

                // 如果不是自己发给自己的，则继续处理
                String content = new String(packet.getData(), 0, packet.getLength());
                Logger.d("[UDPServer] UDP Recv: Content: " + content + "， remoteIP="+remoteIP+", mCallback="+mCallback+", mode="+mMode);

                if (mCallback != null) {
                    // 如果是服务端
                    if (mMode == SocketManager.MODE_SERVER) {
                        // 服务端接收到PING消息
                        if (content.equals(Message.MEG_UDP_C2S_PING)) {
                            mCallback.onUDPClientPing(remoteIP);
                            Logger.e("[UDPServer] 服务端收到 content="+content+"，来之remoteIP="+remoteIP.getHostAddress());
                        }
                    }
                    // 如果是客户端
                    else if (mMode == SocketManager.MODE_CLIENT) {
                        String cmd = content.split("\\|")[0];
                        Logger.d("[UDPServer] UDP 客户端收到 Recv: cmd: " + cmd);
                        if (cmd.equals(Message.MSG_UDP_S2C_HOST_INFO)) {
                            if (timer != null) {
                                timer.purge();
                                timer.cancel();
                                timer = null;
                            }

                            int tcpPort = Integer.valueOf(content.split("\\|")[1]);
                            Logger.e("[UDPServer] 客户端收到 UDP Recv: tcpIP: " + remoteIP.getHostName() + ", tcpPort="+tcpPort);
                            // 已经收到服务器TCP地址了，不需要再发送心跳包了

                            mCallback.onUDPServerHostInfo(remoteIP.getHostName(), tcpPort);
                            break;
                        }
                    }
                }
            }
            Logger.e("[UDPServer] UDPFinished work.....Bye....");
            close();
        } catch (Exception e) {
            e.printStackTrace();
            Logger.d("Exception e:"+e);
        }
    }

    // 断开UDP线程， 离开组播UDP
    public void close() {
        Logger.d("[UDPServer] close udp server...bye...");
        this.keepRunning = false;

        if (timer != null) {
            timer.purge();
            timer.cancel();
            timer = null;
        }

        try {
            if(null!=multicastSocket && !multicastSocket.isClosed()){
                multicastSocket.leaveGroup(InetAddress.getByName(SocketManager.UDP_IP));
                multicastSocket.close();
            }
        } catch (Exception e1) {
            Logger.e("[UDPServer] Exception creating socket"+e1);
        }
    }

    /** 向组播的IP地址和端口， 发送消息， 这里由于是给组播IP发送，所以是广播的， 所有socket都能接受到 */
    public void sendBroadcast(final String data) {
        new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    InetAddress local = InetAddress.getByName(SocketManager.UDP_IP);
                    DatagramPacket packet = new DatagramPacket(data.getBytes(), data.length(), local, SocketManager.UDP_PORT);
                    multicastSocket.send(packet);
                    Logger.d("[UDPServer] sendBroadcast data="+data);
                } catch (Exception e) {
                    Logger.e("[UDPServer] Exception during sendBroadcast"+e);
                }
            }
        }.start();
    }

    //
    /** 给组播内某个具体的IP发送消息，发现在部分手机，如华为手机上接收不到数据 */
    public void sendMessage(final InetAddress ip, final String data) {
        new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    DatagramPacket packet = new DatagramPacket(data.getBytes(), data.length(), ip, SocketManager.UDP_PORT);
                    multicastSocket.send(packet);
                    Logger.d("[UDPServer] sendMessage data = "+data);
                } catch (Exception e) {
                    Logger.e("[UDPServer] Exception during sendIP"+e);
                }
            }
        }.start();
    }


    /**
     * 2s定时发送数据
     */
    private void sendClientPing() {
        if (timer == null) {
            timer = new Timer();
        }

        if (task == null) {
            task = new TimerTask() {
                @Override
                public void run() {
                    Logger.d("[UDPServer] 客户端发送心跳包....");
                    sendBroadcast(Message.MEG_UDP_C2S_PING);
                }
            };
        }

        timer.schedule(task, 0, 3000);
    }



}
