package com.rokid.udpbroadcast.domain;

import android.os.Bundle;
import android.os.Message;

import com.rokid.udpbroadcast.utils.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

// 这个是线程是跑在客户端的
public class TCPConnection extends Thread {

    private Socket socket;

    private Boolean keepRunning = false;

    // 客户端连接
    public TCPConnection(final String ip, final int port) {
        super();

        Logger.d("TCPConnection start");
        new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    Logger.d("TCPConnection init the socket");
                    TCPConnection.this.socket = new Socket(ip, port);
                    TCPConnection.this.keepRunning = true;

                } catch (IOException e) {
                    // TODO: error when establishing a connection?
                    e.printStackTrace();
                }
            }
        };

        Logger.d("TCPConnection end");
    }

    // 这个是服务端保存的
    public TCPConnection(Socket socket) {
        this.socket = socket;

        this.keepRunning = true;
    }

    @Override
    public void run() {
        super.run();

        try {
            String received="";
            // 注意这里in()也是阻塞式的，所以不会一直循环跑，而是等待发送过来的命令消息
            while (keepRunning && socket != null && !socket.isClosed()) {
                // 通知UI 更新
                received = in();
                Logger.d("TCPConnection--received ="+received);
                sendMsg("MSG", received);
            }
            Logger.d("TCPConnection thread run exit---received="+received);
            // 这里是一个客户端断开后，服务端这边会in()为空
            if (keepRunning) {
                Logger.e("CONNECTION READ NULL");
                // 如果没有读到任何消息，则断开连接，并通知所有剩余的playui退出
                DomainUtils.getInstance().disconnectionDetected(this);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Logger.e("CONNECTION READ EXCEPTION"+e);
            DomainUtils.getInstance().disconnectionDetected(this);
        }

    }

    public String in() throws Exception {
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String result = in.readLine();
        return result;
    }

    public void out(final String content) throws Exception {
        new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    PrintWriter out;
                    out = new PrintWriter(socket.getOutputStream(), true);
                    out.println(content);

                } catch (IOException e) {
                    // TODO: error when establishing a connection?
                    e.printStackTrace();
                    Logger.e("TCPConnection out " + e);
                }
            }
        };
    }

    // 如果收到消息，则通过handler来通知domain
    private void sendMsg(String type, String content) {
        Logger.e("sendMsg type："+type+", content="+content);
        Message msg = new Message();
        Bundle data = new Bundle();
        data.putString(type, content);
        msg.setData(data);
        DomainUtils.getInstance().getHandlerDomain().sendMessage(msg);
    }

    public void close() {
        Logger.d("Closing TCPConnection");
        this.keepRunning = false;
        try {
            this.socket.shutdownInput();
        } catch (IOException e) {
            Logger.e("Closing shutdownInput TCPConnection" + e);
        }

        try {
            this.socket.close();
        } catch (IOException e) {
            Logger.e("Closing close TCPConnection" + e);
        }
    }


}

