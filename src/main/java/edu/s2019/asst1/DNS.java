package edu.s2019.asst1;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class DNS {
    public ArrayList<Node> nodesInCAN = new ArrayList<>();
    public InetAddress ip;

    public DNS() throws UnknownHostException, SocketException {

        final DatagramSocket socket = new DatagramSocket();
        socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
        this.ip = InetAddress.getByName(socket.getLocalAddress().getHostAddress());
    }

    public static void main(String args) throws SocketException, UnknownHostException {

        DNS dns = new DNS();




    }
}
