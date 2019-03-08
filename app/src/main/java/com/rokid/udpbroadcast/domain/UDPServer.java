package com.rokid.udpbroadcast.domain;

import com.rokid.udpbroadcast.utils.Logger;
import com.rokid.udpbroadcast.utils.Utils;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class UDPServer extends Thread {
    // 组播UDP
    private MulticastSocket multicastSocket;

    // 线程运行控制flag
    private Boolean keepRunning;

    private ISocketCallback mCallback;

    public UDPServer(ISocketCallback callback) {
        super("UPDServer");
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
            multicastSocket.setLoopbackMode(false);
            InetAddress address = InetAddress.getByName(SocketManager.UDP_IP);
            multicastSocket.joinGroup(new InetSocketAddress(address, SocketManager.UDP_PORT), eth0);
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
                if ((remoteIP.getHostAddress()).equals(Utils.getLocalAddress().getHostAddress()))
                    continue;

                // 如果不是自己发给自己的，则继续处理
                String content = new String(packet.getData(), 0, packet.getLength());
                Logger.d("[UDPServer] UDP Recv: Content: " + content + "， remoteIP="+remoteIP);


                if (mCallback != null) {
                    if (content.equals(Message.MEG_UDP_C2S_PING)) {
                        mCallback.onUDPClientPing(remoteIP);
                    }
                    else {
                        String tcpIP = content.split("|")[0];
                        int tcpPort = Integer.valueOf(content.split("|")[0]);
                        Logger.d("[UDPServer] UDP Recv: tcpIP: " + tcpIP + ", tcpPort="+tcpPort);
                        mCallback.onUDPServerHostInfo(tcpIP, tcpPort);
                    }
                }

                Logger.d("[UDPServer] UDPFinished work");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Logger.d("Exception e:"+e);
        }
    }

    // 断开UDP线程， 离开组播UDP
    public void close() {
        Logger.d("[UDPServer] close udp server...bye...");
        this.keepRunning = false;
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

    /** 给组播内某个具体的IP发送消息， 这里就是服务端给刚刚加入group的客户端发送“新游戏”的消息 */
    public void sendMessage(final InetAddress ip, final String data) {
        new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    DatagramPacket packet = new DatagramPacket(data.getBytes(), data.length(), ip, SocketManager.UDP_PORT);
                    multicastSocket.send(packet);
                    Logger.d("[UDPServer] sendMessage data="+data);
                } catch (Exception e) {
                    Logger.e("[UDPServer] Exception during sendIP"+e);
                }
            }
        }.start();
    }

}
