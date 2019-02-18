package edu.s2019.asst1;

import edu.s2019.asst1.implement.DNSInterface;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.rmi.AccessException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Random;

public class DNS implements DNSInterface {
    public HashMap<String, String> nodesInCAN = new HashMap<>();
    public InetAddress ip;
    public static final int port = 1024;

    public DNS() {
        try {
            final DatagramSocket socket = new DatagramSocket();
            socket.connect(InetAddress.getByName("8.8.8.8"), 80);
            this.ip = InetAddress.getByName(socket.getLocalAddress().getHostAddress());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (SocketException e) {
            e.printStackTrace();
        }

    }

    @Override
    public HashMap<String, String> returnNodeList() {
        Object[] keys = this.nodesInCAN.keySet().toArray();
        HashMap<String, String> returnMap = new HashMap<>();
        if (!nodesInCAN.isEmpty()) {
            for (int i = 0; i < 3; i++) {
                Random random = new Random();
                int n = random.nextInt(nodesInCAN.size());
                returnMap.put((String) keys[n], nodesInCAN.get(keys[i]));

            }
        }
        return returnMap;
    }

    public void registerNode(String name, String iP) {
        nodesInCAN.put(name, iP);
        System.out.println("Node Registered");
        System.out.println("Name -- " + name);
        System.out.println("IP -- " + iP);
    }

    public void deregisterNode(Node node) {
        nodesInCAN.remove(node.name);
        System.out.println("Node remove");
        System.out.println("Name -- " + node.name);
        System.out.println("IP -- " + node.nodeaddress.getHostAddress());
    }

    public static void main(String[] args) {

        try {
            String name = "DNS";
            DNS dns = new DNS();
            DNSInterface stub = (DNSInterface) UnicastRemoteObject.exportObject(dns, DNS.port);
            Registry registry = LocateRegistry.createRegistry(DNS.port);
            registry.rebind(name, stub);
            System.out.println("DNS Online");
            System.out.println("DNS information --\nip --> " + ((DNS) dns).ip.getHostAddress());
            System.out.println("Nodes stores --> " + ((DNS) dns).nodesInCAN.size());
        } catch (AccessException e) {
            System.out.println("DNS server Failure " + e.getMessage());
            e.printStackTrace();

        } catch (RemoteException e) {
            System.out.println("DNS server Failure" + e.getMessage());
            e.printStackTrace();
        }
    }
}