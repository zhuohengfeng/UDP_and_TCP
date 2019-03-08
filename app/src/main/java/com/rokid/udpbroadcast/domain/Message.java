package com.rokid.udpbroadcast.domain;

public class Message {

//    public static final int MEG_UDP_TYPE = 0x10;
//    public static final int MEG_TCP_TYPE = 0x11;


    /** udp Client -> Service */
    public static final String MEG_UDP_C2S_PING = "ping";
    public static final String MSG_UDP_S2C_HOST_INFO = "host_info";
//
//    public int type;
//
//    public String message;
//
//    public String param;
//
//    public InetAddress remoteIP;
//
//    public int port;
//
//    public Message(int type, String message, String param, InetAddress remoteIP) {
//        this.type = type;
//        this.message = message;
//        this.param = param;
//        this.remoteIP = remoteIP;
//    }
//
//    public Message(int type, InetAddress remoteIP) {
//        this.type = type;
//        this.remoteIP = remoteIP;
//    }
//
//    public void splitMessageParam(String msg) {
//        this.message = msg.split("\\|")[0];
//        this.param = msg.split("\\|")[1];
//    }
//
//    @Override
//    public String toString() {
//        return "Message{" +
//                "type=" + type +
//                ", message='" + message + '\'' +
//                ", param='" + param + '\'' +
//                ", remoteIP=" + remoteIP +
//                ", port=" + port +
//                '}';
//    }
}
