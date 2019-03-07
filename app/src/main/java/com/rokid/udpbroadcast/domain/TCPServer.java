package com.rokid.udpbroadcast.domain;

import com.rokid.udpbroadcast.utils.Logger;
import com.rokid.udpbroadcast.view.DeviceBean;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

// 这个是跑着服务端的
// 一直监听是否有客户端连接上
public class TCPServer extends Thread {

    ServerSocket serverSocket;

    private Vector<DeviceBean> players;

    private Integer numPlayerLastAssigned;

    private Boolean keepRunning;
    private Boolean listening;

    public TCPServer(Vector<DeviceBean> players) {
        super();

        setName("TCPServer");

        this.players = players;

        this.numPlayerLastAssigned = 0;

        this.keepRunning = true;
        this.listening = false;
    }


    @Override
    public void run() {
        try {
            Logger.d("first line");
            SocketManager.getInstance().portServer = SocketManager.TCP_PORT;
            do {
                try {
                    // 首先创建一个ServerSocket, 这里是循环端口创建
                    serverSocket = new ServerSocket(SocketManager.getInstance().portServer);
                } catch (Exception e) {
                    SocketManager.getInstance().portServer++;
                    Logger.d("Trying with " + SocketManager.getInstance().portServer);
                }
            } while (serverSocket == null || !serverSocket.isBound());

            Logger.d("TCPServer Started");

            this.listening = true;

            // 服务器等待客户端的tcp连接 ---- 终于等到TCP连接上了，这里就在服务端创建一个设备名称在APP端连接
            while (keepRunning) {
                Socket client = serverSocket.accept();
                // 创建一个角色player
                DeviceBean new_player = new DeviceBean(new TCPConnection(client), this.numPlayerLastAssigned);
                this.numPlayerLastAssigned++;
                players.add(new_player);
                Logger.d("有一个新的设备加入： new_player="+new_player);
                DomainUtils.getInstance().updatedPlayers();
            }
        } catch (Exception e) {
            Logger.e("Exception  in running TCPServer"+e);
        }
    }

    public void close() {
        try {
            this.keepRunning = false;
            this.serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Boolean isListening() {
        return this.listening;
    }

}
