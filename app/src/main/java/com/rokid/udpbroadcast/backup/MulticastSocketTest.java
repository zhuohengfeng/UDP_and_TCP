package com.rokid.udpbroadcast.backup;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Scanner;

//让该类实现Runnable接口，该类的实例可以作为线程的target
public class MulticastSocketTest  implements   Runnable{

    //使用常量作为本程序多点广播的IP地址
    private   static  final  String  BROADCAST_IP="230.0.0.1";
    //使用常量作为本程序的多点广播的目的地端口
    public static final  int BROADCAST_PORT=3000;
    //定义每个数据报大小最大为4kb
    private  static final  int  DATA_LEN=4096;
    //定义本程序的MulticastSocket实例
    private MulticastSocket socket=null;
    private InetAddress broadcastAddress=null;
    private Scanner scan=null;

    //定义接收网络数据的字节数组
    byte[]  inBuff=new  byte[DATA_LEN];
    //以指定字节数组创建准备接收数据的MulticastSocket对象
    private DatagramPacket inPacket =new  DatagramPacket(inBuff, inBuff.length);

    //定义一个用于发送的DatagramPacket对象
    private DatagramPacket  outPacket=null;

    public void init() throws IOException{
        //创建键盘输入流
        Scanner scan = new Scanner(System.in);
        //创建用于发送、接收数据的MulticastSocket对象，由于该MulticastSocket需要接收数据，所以有指定端口
        socket=new  MulticastSocket(BROADCAST_PORT);
        broadcastAddress=InetAddress.getByName(BROADCAST_IP);

        //将该socket加入到指定的多点广播地址
        socket.joinGroup(broadcastAddress);
        //设置本MulticastSocket发送的数据报会被回送到自身
        socket.setLoopbackMode(false);

        //初始化发送用的DatagramSocket，它包含一个长度为0的字节数组
        outPacket =new DatagramPacket(new byte[0], 0, broadcastAddress, BROADCAST_PORT);

        //启动本实例的run()方法作为线程执行体的线程
        new  Thread(this).start();

        //不断的读取键盘输入
        while(scan.hasNextLine()){
            //将键盘输入的一行字符转换成字节数组
            byte [] buff=scan.nextLine().getBytes();
            //设置发送用的DatagramPacket里的字节数据
            outPacket.setData(buff);
            //发送数据报
            socket.send(outPacket);
        }
        socket.close();
    }





    public void run() {
        // TODO Auto-generated method stub

        while(true){
            //读取Socket中的数据，读到的数据放入inPacket所封装的字节组里
            try {
                socket.receive(inPacket);
                //打印从socket读取到的内容
                System.out.println("聊天信息："+new String(inBuff,0,inPacket.getLength()));
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            if(socket!=null){
                //让该socket离开多点IP广播地址
                try {
                    socket.leaveGroup(broadcastAddress);
                    //关闭socket对象
                    socket.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }

            System.exit(1);
        }
    }
    public static void main(String[] args) {
        try {
            new  MulticastSocketTest().init();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}

