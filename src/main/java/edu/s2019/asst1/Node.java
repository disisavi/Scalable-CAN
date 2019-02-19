package edu.s2019.asst1;

import edu.s2019.asst1.implement.DNSInterface;
import edu.s2019.asst1.implement.NodeInterface;
import edu.s2019.asst1.message.Message;

import java.awt.*;
import java.io.Serializable;
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

public class Node implements NodeInterface, Serializable {
    public static final int port = 1025;
    private static final long serialVersionUID = -6663545639371364731L;
    String name;
    InetAddress nodeaddress;
    private Zone zone;
    private ArrayList<NodeInterface> peers = new ArrayList<NodeInterface>();
    private DNSInterface dnsStub;


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

    /*
    TODO
    0. Make The server gracefully exit in case of failed bootstrap...
    .5 Check and revise conditions for which node will deny splitting
    .75 work on peer splitting algo
    1. Implement the Server which accepts commands
    2. Start work on other Modules as well...
    3. Implement the Hash Algo
    4. Make Bootstrap such that if its not able to contact any node, then consider all node dead and create new zone...
    5. Add a few comments so that i have some fucking idea what ive done...
    */
    public static void main(String[] args) {

        System.out.print("\033[H\033[2J");
        System.out.flush();
        Node node = new Node();
        try {

            NodeInterface nodeStub = (NodeInterface) UnicastRemoteObject.exportObject(node, Node.port);
            Registry registry = LocateRegistry.createRegistry(Node.port);
            registry.rebind(node.getName(), nodeStub);
            System.out.println("Client Server Startup Complete\nNode Name -- " + node.getName());
            System.out.println("ip -- " + node.getIP().getHostAddress());
        } catch (AccessException e) {
            System.out.println("Client server Startup Failure ...");
            node.shutdown(e);
        } catch (RemoteException e) {
            System.out.println("Client server Startup Failure ...");
            node.shutdown(e);
        }

        try {
            node.dnsStub = node.setDnsStub();
            if (node.bootstrap()) {
                node.dnsStub.registerNode(node.name, node.getIP().getHostAddress());
                System.out.println("Bootstrapping success... ");
                node.printNode();
            } else {

                System.out.println("####################\nBootstrap Error--- We will now repeat the proccess under assumption that we are the fist node and DNS was not updated till now.");

                node.zone = new Zone();
                node.dnsStub.registerNode(node.name, node.getIP().getHostAddress());
                System.out.println("Bootstrapping success... ");
                node.printNode();
            }
        } catch (RemoteException e) {
            node.shutdown(e);
        } catch (NotBoundException e) {
            node.shutdown(e);
        }
        node.run();
        node.shutdown(null);
    }

    public InetAddress getSelfIP() throws SocketException, UnknownHostException {

        final DatagramSocket socket = new DatagramSocket();
        socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
        InetAddress ip = InetAddress.getByName(socket.getLocalAddress().getHostAddress());

        return ip;
    }

    public boolean isPeer(Node node) {
        return this.zone.zoneShareWall(node.zone);
    }

    public boolean equals(Node node) {
        return this.nodeaddress.equals(node.nodeaddress) && this.name.equals(node.name);
    }

    public DNSInterface setDnsStub() throws RemoteException, NotBoundException {

        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter the DNS server ip ");
        String host = scanner.next();

        Registry dnsregistry = LocateRegistry.getRegistry(host, DNS.port);
        DNSInterface dns = (DNSInterface) dnsregistry.lookup("DNS");
        return dns;
    }

    public boolean bootstrap() {

        ArrayList<NodeInterface> response = new ArrayList<>();
        try {
            response = this.dnsStub.returnNodeList();
        } catch (Exception e) {
            System.err.println("Client Bootstrap Failure for " + this.name);
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
                for (NodeInterface bootNode : response) {
                    nodeID = routeToNode(bootNode, p2);
                    if (nodeID != null) {
                        break;
                    }
                }
            } while (nodeID != null && !splitWithNode(nodeID));

            if (nodeID == null) {
                return false;
            }
        }
        return true;
    }

    public Point getCordinateToBind() {
        Random random = new Random();
        int x = random.nextInt(Zone.maxWidth);
        int y = random.nextInt(Zone.maxHeight);
        return new Point(x, y);
    }

    public AbstractMap.SimpleEntry<String, String> routeToNode(NodeInterface node, Point point) {
        AbstractMap.SimpleEntry<String, String> response = null;
        try {

            response = node.findNodeToPoint(point);
        } catch (Exception e) {
            System.err.println("Client RMI failure couldnt execute Peer Method while Routing");
            System.out.println("Client exception: " + e.toString());
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


    public Message splitNode() {
        if (this.zone.height < 2 || this.zone.widht < 2) {
            return null;
        }

        Message returnMessage = new Message();
        returnMessage.zone = this.zone.splitZone();
        //To correct soon ===
        returnMessage.peers = this.peers;
        return returnMessage;
    }

    public void shutdown(Exception exception) {
        System.out.println("Shutting down RMI server");
        if (exception != null) {
            System.out.println("The following error lead to the shutdown");
            System.err.println(exception.getMessage());
            exception.printStackTrace();
        }
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

        System.exit(-1);

    }

    void view(String nodeName, Boolean showAllFlag) {
        try {
            if (showAllFlag) {
                ArrayList<NodeInterface> allNodesInCAN = this.dnsStub.returnAllNodes();
                for (NodeInterface nodestub : allNodesInCAN) {
                    System.out.println(nodestub.returnNodeStatus());
                }
            } else if (nodeName.toUpperCase().equals(this.name.toUpperCase())) {
                this.printNode();
            } else {
                NodeInterface nodeStub = dnsStub.returnNode(nodeName);
                System.out.println(nodeStub.returnNodeStatus());
            }
        } catch (Exception e) {
            System.out.println("Error in Printing Node. Printing StackTrace. After that, we will still be on with business");
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    public String returnNodeStatus() {
        StringBuilder returnBuilder = new StringBuilder();

        returnBuilder.append("\n*******************************\n");
        returnBuilder.append("IP -- " + this.getIP());
        returnBuilder.append("\nName -- " + this.name);
        returnBuilder.append("\nZone and file details ---\n");
        returnBuilder.append(this.zone.returnZoneStatus());
        returnBuilder.append("\nPeers --");
        try {
            for (NodeInterface peers : this.peers) {
                returnBuilder.append("\n"+peers.getName());

            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return returnBuilder.toString();
    }

    void printNode() {
        System.out.println("\n*******************************");
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

    void run() {
        Scanner scanner = new Scanner(System.in);
        boolean runAlways = true;
        while (runAlways) {
            String argumet = scanner.next();
            String[] command = argumet.split(" ", 0);
            this.showAvailableComands();
            switch (command[0].toUpperCase()) {
                case "VIEW":
                    boolean showALlFlag = false;
                    String param = null;
                    if (command.length == 1) {
                        showALlFlag = true;

                    } else {
                        param = command[1];
                    }
                    this.view(param, showALlFlag);
                    break;
                case "INSERT":
                    System.out.println("Under Construction");
                    break;
                case "EXIT":
                    System.out.println("************\nExiting");
                    runAlways = false;
                    break;
                default:
                    System.out.println("Please enter one of the printed commands");
                    this.showAvailableComands();
            }
        }
    }

    void showAvailableComands() {
        System.out.println("The following commands are available as now");
        System.out.println("1. View  --> Display the information of a specified peer peer where peer is a node identifier, not an IP address. The information includes the node identifier, the IP address, the coordinate, a list of neighbors, and the data items currently stored at the peer. If no peer is given, display the information of all currently active peers.");
        System.out.println("2. Insert --> UnderConstruction");
        System.out.println("Exit --> To exit");
    }
}