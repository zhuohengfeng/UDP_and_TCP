package com.rokid.udpbroadcast.domain;

import com.rokid.udpbroadcast.utils.Logger;
import com.rokid.udpbroadcast.view.DeviceBean;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

// 这个是跑着服务端的
// 一直监听是否有客户端连接上
public class TCPServer extends Thread {

    private ServerSocket serverSocket;

    private Boolean keepRunning;
    private Boolean listening;

    private ISocketCallback mCallback;

    private int numPlayerLastAssigned = 0;

    public TCPServer(ISocketCallback callback) {
        super("TCPServer");
        this.keepRunning = true;
        this.listening = false;
        this.mCallback = callback;
        this.numPlayerLastAssigned = 0;
    }


    @Override
    public void run() {
        try {
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

            Logger.d("[TCPClient] TCPServer 服务端启动，进入监听状态 portServer="+SocketManager.getInstance().portServer);

            this.listening = true;

            // 服务器等待客户端的tcp连接 ---- 终于等到TCP连接上了，这里就在服务端创建一个设备名称在APP端连接
            while (keepRunning) {
                Socket client = serverSocket.accept();

                // 创建一个设备
                DeviceBean new_device = new DeviceBean(new TCPConnection(client), this.numPlayerLastAssigned);
                this.numPlayerLastAssigned++;

                Logger.e("有一个新的设备加入： new_device="+new_device);

                if (mCallback != null) {
                    mCallback.onTCPServerAddDevice(new_device);
                }

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
