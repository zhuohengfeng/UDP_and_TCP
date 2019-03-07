package com.rokid.udpbroadcast.domain;

import com.rokid.udpbroadcast.utils.Logger;
import com.rokid.udpbroadcast.view.DeviceBean;

import java.util.Vector;

// 这个线程用于服务器给所有的player发送消息
public class TCPServerSend extends Thread {

    private Vector<DeviceBean> players;
    private String data;
    private Vector<String> data_vector;
    private Integer mode;

    public TCPServerSend(Vector<DeviceBean> players, String data) {
        super();

        setName("TCPServerSend");

        this.players = players;
        this.data = data;

        this.mode = 1;
    }

    public TCPServerSend(Vector<DeviceBean> players, Vector<String> data) {
        super();

        setName("TCPServerSend");

        this.players = players;
        this.data_vector = data;

        this.mode = 2;
    }

    public TCPServerSend(Vector<DeviceBean> players) {
        super();

        setName("TCPServerSend");

        this.players = players;

        this.mode = 3;
    }

    public void run() {

        if (mode == 1) {
            //Just send one thing

            for (int i = (this.players.size() - 1); i >= 0; i--) {
                try {
                    Logger.d("sending to player " + i + " " + data);
                    this.players.get(i).out(data);
                } catch (Exception e) {

                    // If the data we are sending is SHUTDOWN ignore the closed sockets and continue sending
                    if (!data.equals("SHUTDOWN")) {
                        DomainUtils.getInstance().disconnectionDetected(this.players.get(i).getConnection());
                    }
                }
            }

        } else if (mode == 2) {
            // Send more than one thing

            for (int d = 0; d < data_vector.size(); d++) {
                for (int i = (this.players.size() - 1); i >= 0; i--) {
                    try {
                        Logger.d("sending to player " + i + " " + data_vector.get(d));
                        this.players.get(i).out(data_vector.get(d));
                    } catch (Exception e) {
                        DomainUtils.getInstance().disconnectionDetected(this.players.get(i).getConnection());
                    }
                }
            }

        } else if (mode == 3) {
            // 开始游戏相关的命令发送， 这里是服务器端发送的

            // Send the startGame
            //String board = "UPDATEBOARD " + DomainUtils.getInstance().serialize(DomainUtils.getInstance().getBoardToSend());
            // 开始游戏，发送随机产生的4张牌
            String board = "PORKS " /*+ DomainUtils.getInstance().serialize(GameServer.getInstance().getPorks())*/;

            for (int i = 0; i < this.players.size(); i++) {
                try {
                    Logger.d("sending to player " + i + " " + board);
                    this.players.get(i).out(board);
                } catch (Exception e) {
                    DomainUtils.getInstance().disconnectionDetected(this.players.get(i).getConnection());
                }
            }

            // TODO: Ugly fix, wait between sending packets
            try {
                Thread.sleep(500);
            } catch (InterruptedException e1) {
            }


            String text_to_start = "STARTGAME";

            for (int i = 0; i < this.players.size(); i++) {
                try {
                    Logger.d("Sending to player " + i + " STARTGAME");
                    this.players.get(i).out(text_to_start);
                } catch (Exception e) {
                    DomainUtils.getInstance().disconnectionDetected(this.players.get(i).getConnection());
                }
            }

        }
    }
}