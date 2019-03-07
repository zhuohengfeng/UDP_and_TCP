package com.rokid.udpbroadcast.view;

import com.rokid.udpbroadcast.domain.TCPConnection;
import com.rokid.udpbroadcast.utils.Logger;

/**
 * 设备Bean
 * 只要IP一样，则认为是同一个设备
 */
public class DeviceBean {
    String ip;      // IP地址
    int port;       // 端口
    String name;    // 设备名称  // SN
    String room;    // 设备所在房间 // typeID
//        String mac;
//        String sn;

    private TCPConnection connection;

    private int index;

    public DeviceBean(TCPConnection connection, int index) throws Exception {
        this.connection = connection;
        // The first thing we receive is the player name
        this.name = in();

        this.index = index;

        // 给客户端发送numPlayer和numTeam
        Logger.d("Now send the index:"+index);
        // The first thing we send is the player id and the team id assigned
        out(index+"");

        this.connection.setName("TCP server " + this.name);
        this.connection.start(); // 启动线程读取消息
    }


    public String in() throws Exception {
        return connection.in();
    }
    public void out(String content) throws Exception {
        connection.out(content);
    }
    public TCPConnection getConnection() {
        return connection;
    }

    public void close() {
        if (this.connection != null && this.connection.isAlive()) {
            this.connection.close();
            while (this.connection.isAlive()) {
                ;
            }
        }
        this.connection = null;
    }





    @Override
    public int hashCode() {
        return ip.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof DeviceBean) {
            return this.ip.equals(((DeviceBean)o).getIp());
        }
        return super.equals(o);
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }
}