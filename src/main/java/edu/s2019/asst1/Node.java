package edu.s2019.asst1;

import edu.s2019.asst1.implement.DNSInterface;
import edu.s2019.asst1.implement.NodeInterface;

import java.awt.*;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

public class Node implements NodeInterface {
    String name;
    private Zone zone;
    private ArrayList<NodeInterface> peers = new ArrayList<NodeInterface>();
    InetAddress nodeaddress;
    public static final int port = 1024;

    public class Message {
        public Zone zone;
        public ArrayList<NodeInterface> peers = new ArrayList<NodeInterface>();
    }

    public Node() {
        try {
            this.nodeaddress = getSelfIP();
            this.name = nodeaddress.getHostName();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Node(String name) {
        this.name = name;
        try {
            this.nodeaddress = getSelfIP();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public InetAddress getSelfIP() throws SocketException, UnknownHostException {

        final DatagramSocket socket = new DatagramSocket();
        socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
        InetAddress ip = InetAddress.getByName(socket.getLocalAddress().getHostAddress());

        return ip;
    }

    public boolean isPeer(Node node) {
        if (this.zone.zoneShareWall(node.zone)) {
            return true;
        }
        return false;
    }

    public boolean equals(Node node) {
        if (this.nodeaddress.equals(node.nodeaddress) && this.name.equals(node.name)) {
            return true;
        }
        return false;
    }

    public boolean bootstrap() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter the DNS server ip ");
        String host = scanner.next();
        HashMap<String, String> response = new HashMap<>();
        try {
            Registry dnsregistry = LocateRegistry.getRegistry(host, Node.port);
            DNSInterface dns = (DNSInterface) dnsregistry.lookup("DNS");
            response = dns.returnNodeList();
        } catch (Exception e) {
            System.err.println("Client Bootstrap Failed for " + this.name);
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
            return false;
        }

        if (response.size() == 0) {
            this.zone = new Zone();
            return true;
        } else {
            AbstractMap.SimpleEntry<String, String> nodeID = null;
            do {
                Point p2 = getCordinateToBind();
                for (String key : response.keySet()) {
                    nodeID = routeToNode(key, response.get(key), p2);
                    if (nodeID != null) {
                        break;
                    }
                }
            } while (!splitWithNode(nodeID));
        }
        return true;
    }

    public Point getCordinateToBind() {
        Random random = new Random();
        int x = random.nextInt(Zone.maxWidth);
        int y = random.nextInt(Zone.maxHeight);
        return new Point(x, y);
    }

    public AbstractMap.SimpleEntry<String, String> routeToNode(String nodeName, String nodeIP, Point point) {
        AbstractMap.SimpleEntry<String, String> response = null;
        try {
            Registry noderegistry = LocateRegistry.getRegistry(nodeIP, Node.port);
            NodeInterface node = (NodeInterface) noderegistry.lookup(nodeName);
            response = node.findNodeToPoint(point);
        } catch (Exception e) {
            System.err.println("Client RMI failure couldnt Contact Node " + nodeName + " while Routing");
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
        return response;
    }

    public AbstractMap.SimpleEntry<String, String> findNodeToPoint(Point point) throws RemoteException {
        if (this.zone.isPointInZone(point)) {
            AbstractMap.SimpleEntry<String, String> returnEntry = new AbstractMap.SimpleEntry<>(this.name, this.nodeaddress.getHostAddress());
            return returnEntry;
        }

        int low = 11;
        NodeInterface nodeLow = new Node();
        for (NodeInterface node : this.peers) {

            if (node.getZone().isPointInZone(point)) {
                return new AbstractMap.SimpleEntry<>(node.getName(), node.getIP().getHostAddress());
            }
            if (low > node.getZone().distanceToPoint(point)) {
                low = node.getZone().distanceToPoint(point);
                nodeLow = node;
            }
        }

        return nodeLow.findNodeToPoint(point);
    }

    public boolean splitWithNode(AbstractMap.SimpleEntry<String, String> nodeID) {
        Message response = new Message();
        try {
            Registry noderegistry = LocateRegistry.getRegistry(nodeID.getValue(), Node.port);
            NodeInterface node = (NodeInterface) noderegistry.lookup(nodeID.getKey());
            response = node.splitNode();
            if (response != null) {
                this.peers.add(node);
            }
        } catch (Exception e) {
            System.err.println("Client RMI failure couldnt Contact Node " + nodeID.getKey() + " while splitting");
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }

        if (response == null) {
            return false;
        }

        this.zone = response.zone;
        this.peers = response.peers;

        return true;
    }


    public Node.Message splitNode() {
        if (this.zone.height < 2 || this.zone.widht < 2) {
            return null;
        }

        Message returnMessage = new Message();
        returnMessage.zone = this.zone.splitZone();
        //To correct soon ===
        returnMessage.peers = this.peers;
        return returnMessage;
    }

    public void shutdown() {
        System.out.println("Shutting down RMI server");
        try {

            Registry registry = LocateRegistry.getRegistry();
            registry.unbind(this.getName());
            UnicastRemoteObject.unexportObject(this, true);
            Runtime.getRuntime().gc();
        } catch (AccessException e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (NotBoundException e) {
            e.printStackTrace();
        }
        // otherwise we wait 60seconds for references to be removed
        Runtime.getRuntime().gc();
        // tell main to finish

    }

    public static void main(String[] args) {

        Node node = new Node();
        try {
            NodeInterface nodeStub = (NodeInterface) UnicastRemoteObject.exportObject(node, Node.port);
            Registry registry = LocateRegistry.createRegistry(Node.port + 1);
            registry.rebind(node.getName(), nodeStub);
            System.out.println("Client Server Startup Complete\nNode Name -- " + node.getName());
            System.out.println("ip -- " + node.getIP().getHostAddress());
        } catch (AccessException e) {
            System.out.println("Client server Startup Failure " + e.getMessage());
            e.printStackTrace();
        } catch (RemoteException e) {
            System.out.println("Client server Startup Failure " + e.getMessage());
            e.printStackTrace();
        }


        if (node.bootstrap()) {
            System.out.println("Bootstrapping success... ");
            node.printNode();
        } else {
            System.out.println("Bootstrap Error--- exiting the system");
            node.shutdown();
        }

    }

    void printNode() {
        System.out.println("IP -- " + this.getIP());
        System.out.println("Name -- " + this.name);
        System.out.println("Zone and file details ---");
        this.zone.printZone();
        System.out.println("Peers --");
        try {
            for (NodeInterface peers : this.peers) {
                System.out.println(peers.getName());

            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Zone getZone() {
        return zone;
    }

    @Override
    public ArrayList<NodeInterface> getPeers() {
        return peers;
    }

    public InetAddress getIP() {
        return nodeaddress;
    }

}