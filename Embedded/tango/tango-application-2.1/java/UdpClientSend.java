package ch.hefr.etu.zoutao_wen.tangoapplication;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * Helper for Udp data send
 */

public class UdpClientSend {
    private DatagramSocket udpSocket = null;
    private InetAddress serverAddr = null;
    private int port;

    public UdpClientSend(final String ip, final int port) {
        this.port = port;
        try {
            udpSocket = new DatagramSocket(port);
            serverAddr = InetAddress.getByName(ip);
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public void send(final String message) throws IOException {
        new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] buf = (message).getBytes();
                DatagramPacket packet = new DatagramPacket(buf, buf.length,serverAddr, port);
                try {
                    udpSocket.send(packet);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void close() {
        udpSocket.close();
    }

}
