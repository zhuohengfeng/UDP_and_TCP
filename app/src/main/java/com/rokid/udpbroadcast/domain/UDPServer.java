package com.rokid.udpbroadcast.domain;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.rokid.udpbroadcast.utils.Logger;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.security.InvalidParameterException;
import java.util.Enumeration;

public class UDPServer extends Thread {

    // 保存当前UDP线程是服务端，还是客户端
    private int mode;
    public static Integer MODE_SERVER = 1;
    public static Integer MODE_CLIENT = 2;

    // 组播UDP
    private MulticastSocket multicastSocket;

    // 线程运行控制flag
    private Boolean keepRunning;

    // 这个handler主要用于给client端更新UI
    private Handler handler;

    /** 这里mode=1表示服务端； mode=2表示客户端； handler表示。。。 */
    public UDPServer(int mode, Handler handler) {
        super();

        // 设置线程名称
        setName("UPDServer");

        if (mode != MODE_SERVER && mode != MODE_CLIENT) {
            throw new InvalidParameterException();
        }

        this.handler = handler;
        this.keepRunning = true;
        this.mode = mode;

        // Special socket (catches broadcast) on specified port
        try {
            Logger.d("UDPServer now port:"+SocketManager.UDP_PORT+", mode:"+mode);

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
            //MulticastSocket类时实现多点广播的关键，当MulticastSocket把一个DaragramPocket发送到多点广播的IP地址时，该数据报将会自动广播到加入该地址的所有MulticastSocket。MulticastSocket既可以将数据报发送到多点广播地址，也可以接收其他主机的广播信息。
            multicastSocket = new MulticastSocket(SocketManager.UDP_PORT);
            multicastSocket.setLoopbackMode(false); ////设置本MulticastSocket发送的数据报会被回送到自身
            InetAddress address = InetAddress.getByName(SocketManager.UDP_IP);
            multicastSocket.joinGroup(new InetSocketAddress(address, SocketManager.UDP_PORT), eth0);
        } catch (Exception e) {
            e.printStackTrace();
            Logger.e("Exception creating socket: "+e);
            try {
                if(null!=multicastSocket && !multicastSocket.isClosed()){
                    multicastSocket.leaveGroup(InetAddress.getByName(SocketManager.UDP_IP));
                    multicastSocket.close();
                }
            } catch (Exception e1) {
                Logger.e("Exception creating socket"+e1);
                e.printStackTrace();
            }
        }
    }

    public void run() {
        try {
            byte[] buf = new byte[1024];

            //Listen on socket to receive messages
            while (keepRunning && multicastSocket != null && !multicastSocket.isClosed()) {

                Logger.d("Inside while");

                // Wait for a new packet
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                // 这里会阻塞住，一直等待新的报文传递过来！！！
                multicastSocket.receive(packet);

                Logger.d("Datagram received");

                InetAddress remoteIP = packet.getAddress(); // 是谁发过来的

                // 这里PING广播是client发送的，但是他也会发送给自己，所以这里要加一个判断
                if ((remoteIP.getHostAddress().toString()).equals(SocketManager.getInstance().getLocalAddress().getHostAddress().toString()))
                    continue;

                // 如果不是自己发给自己的，则继续处理
                String content = new String(packet.getData(), 0, packet.getLength());
                Logger.d("UDP Recv: Content: " + content + "， remoteIP="+remoteIP);

                if (mode == MODE_SERVER) {
                    // SERVER---服务端在创建UDP线程之后，会一直等待客户端发送"PING"消息过来

                    Logger.d("Mode server entered");

                    // Send an answer to the client
                    // 如果收到了，就给接收的客户端发送 ： 当前游戏的名称 | Tcp端口号
                    sendIP(remoteIP, DomainUtils.getInstance().getServerName() + "|" + SocketManager.TCP_PORT);

                    Logger.d("Sent answer to client");

                } else if (mode == MODE_CLIENT) {
                    // CLIENT---如果是客户端，不会接收PING消息，所以这里接收的是服务端发送过来的： 当前游戏的名称 | 端口号 ，并获取服务端的IP地址
                    // 可以把这些更新到JoinGameWaiting这个界面
                    Logger.d("Mode client entered");
                    // 这里的remoteIP 就是服务器的IP地址
                    sendUIServer(content.split("\\|")[0], remoteIP, content.split("\\|")[1]);
                }

                Logger.d("Finished work");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Logger.d("Exception e:"+e);
        }

    }

    // 断开UDP线程， 离开组播UDP
    public void close() {
        this.keepRunning = false;
        //socket.close();
        try {
            if(null!=multicastSocket && !multicastSocket.isClosed()){
                multicastSocket.leaveGroup(InetAddress.getByName(SocketManager.UDP_IP));
                multicastSocket.close();
            }
        } catch (Exception e1) {
            Logger.e("Exception creating socket"+e1);
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
                    Logger.d("Datagram sent  data="+data);
                } catch (Exception e) {
                    Logger.e("Exception during sendBroadcast"+e);
                }
            }
        }.start();
    }

    /** 给组播内某个具体的IP发送消息， 这里就是服务端给刚刚加入group的客户端发送“新游戏”的消息 */
    public void sendIP(final InetAddress ip, final String data) {
        new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    DatagramPacket packet = new DatagramPacket(data.getBytes(), data.length(), ip, SocketManager.UDP_PORT);
                    multicastSocket.send(packet);
                    Logger.d("Datagram sent");
                } catch (Exception e) {
                    Logger.e("Exception during sendIP"+e);
                }
            }
        }.start();
    }

    // 客户端收到服务端发过来的 游戏名称， 服务端IP， 服务端port 之后更新joinGameWaiting界面
    private void sendUIServer(String name, InetAddress remoteIP, String port) {
        Logger.d("sendUIServer name="+name+", getHostAddress="+remoteIP.getHostAddress()+", port="+port);
        Message msg = new Message();
        Bundle data = new Bundle();
        data.putString("NAME", name);
        data.putString("IP", remoteIP.getHostAddress());
        data.putInt("PORT", Integer.valueOf(port));
        msg.setData(data);
        handler.sendMessage(msg);
    }




}
