package edu.s2019.asst1;

import edu.s2019.asst1.implement.NodeInterface;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class Node implements NodeInterface {
    private String name;
    private Zone zone;
    private ArrayList<Node> peers = new ArrayList<>();
    private InetAddress nodeaddress;

    public Node(){
        try {
            this.nodeaddress = getSelfIP();
            this.name = nodeaddress.getHostName();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    public Node(String name){
        this.name = name;
        try {
            this.nodeaddress = getSelfIP();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public InetAddress getSelfIP() throws SocketException, UnknownHostException {

        final DatagramSocket socket = new DatagramSocket();
        socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
        InetAddress ip = InetAddress.getByName(socket.getLocalAddress().getHostAddress());


        System.out.println(ip);
        return ip;
    }

    @Override
    public Node splitNode() {
        return null;
    }


    @Override
    public boolean mergeNode(Node node) {
        return false;
    }

    @Override
    public boolean join() {
        return false;
    }

    @Override
    public void leave() {

    }

    @Override
    public void printInfo() {

    }

    public  String search(String filename){
        return "avi";
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Zone getZone() {
        return zone;
    }


    public ArrayList<Node> getPeers() {
        return peers;
    }

    public void setPeers(Node peer) {
        this.peers.add(peer);
    }
    public boolean isPeer(Node node){
        if(this.zone.zoneShareWall(node.zone))
        {
            return true;
        }
        return false;
    }
}
