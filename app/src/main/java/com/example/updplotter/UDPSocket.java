package com.example.updplotter;

import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.LinkedBlockingQueue;


public class UDPSocket {

    private UDPThread listener = null;
    private int port;
    private CustomRunnable r;

    UDPSocket(CustomRunnable r){
        this(8080, r);
    }

    UDPSocket(int port, CustomRunnable r){
        this.port = port;
        this.r = r;
    }

    void start(){
        if(listener != null)
            return;
        Log.i("Listener", "Start called");
        listener = new UDPThread(port, r);
        listener.start();
    }

    void stop(){
        if(listener == null)
            return;

        listener.stopListening();
        try {
            listener.interrupt();
            listener.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void send(String ip, int port, String content){
        start();
        listener.send(ip, port, content);
    }

    private class UDPThread extends Thread {
        private DatagramSocket mm_socket;
        private volatile boolean running = true;
        LinkedBlockingQueue<DatagramPacket> lbq = new LinkedBlockingQueue<DatagramPacket>();
        CustomRunnable r;

        UDPThread(int port, CustomRunnable r){
            this.r = r;
            try {
                mm_socket = new DatagramSocket(null);
                mm_socket.setReuseAddress(true);
                mm_socket.bind(new InetSocketAddress(port));
            } catch (SocketException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            Log.i("Receiver", "Run started");
            while (running) {
                sendFromQueue();
                ReceiveServerSocketData();
            }
        }

        void stopListening(){
            running = false;
        }

        void sendFromQueue(){
            if (!lbq.isEmpty()){
                DatagramPacket packet = lbq.poll();
                try {
                    mm_socket.send(packet);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        void send(String ip, int port, String content){
            byte[] data = content.getBytes();
            InetAddress serverAddress = null;
            try {
                serverAddress = InetAddress.getByName(ip);
                DatagramPacket packet = new DatagramPacket(data,data.length, serverAddress, port);
                lbq.add(packet);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }

        private void ReceiveServerSocketData() {
            try {
                //Log.i("Receiver", "listening");
                byte[] data =new byte[4*1024];
                DatagramPacket packet = new DatagramPacket(data,data.length);
                mm_socket.receive(packet);

                String readMessage = new String(packet.getData(), packet.getOffset(), packet.getLength());
                //Log.i("Receiver", "received");
                r.run(readMessage);
            } catch(IOException e){
                e.printStackTrace();
            }
        }
    }

}
