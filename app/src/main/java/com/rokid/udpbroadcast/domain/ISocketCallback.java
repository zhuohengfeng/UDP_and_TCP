package com.rokid.udpbroadcast.domain;

import com.rokid.udpbroadcast.view.DeviceBean;

import java.net.InetAddress;

public interface ISocketCallback {

    void onUDPClientPing(InetAddress clientIp);

    void onUDPServerHostInfo(String hostTcpIp, int hostTcpPort);

    void onTCPServerAddDevice(DeviceBean device);


}
