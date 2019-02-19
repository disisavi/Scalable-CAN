package edu.s2019.asst1;

import edu.s2019.asst1.implement.DNSInterface;
import edu.s2019.asst1.implement.NodeInterface;
import edu.s2019.asst1.helper.Message;

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
    public static final int port = 1032;
    private static final long serialVersionUID = -6663545639371364731L;
    String name;
    InetAddress nodeaddress;
    private Zone zone;
    private HashMap<String, NodeInterface> peers;
    private DNSInterface dnsStub;


    public Node() {
        try {
            this.nodeaddress = getSelfIP();
            this.name = nodeaddress.getHostName();
            this.peers = new HashMap<>();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
    TODO
    1. work on peer splitting algo
    2. Convert all points from Point to Point2d.float.
    3. Implement  one more command
    5. Add a few comments so that i have some fucking idea what ive done...
    */
    public static void main(String[] args) {

        System.out.print("\033[H\033[2J");
        System.out.flush();
        Node node = new Node();
        try {

            NodeInterface nodeStub = (NodeInterface) UnicastRemoteObject.exportObject(node, Node.port);
            Registry registry;
            try {
                registry = LocateRegistry.createRegistry(Node.port);
            } catch (RemoteException e) {
                System.out.println("Unable to create registry.... Checking if registry already exist");
                registry = LocateRegistry.getRegistry(Node.port);
            }
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


    public boolean insertFile(String filename, Point point) {

        if (this.zone.isPointInZone(point)) {
            this.zone.addFileToPoint(point, filename);
            System.out.println(" ----- Operation performed by remote object --- ");
            System.out.println("File " + filename + "added\n Please find revised Node structure printed now");
            this.printNode();
            return true;
        }
        System.out.println("Couldnt Add file to the point " + point.toString());
        return false;
    }

    public boolean insertFile(String fileName) {
        Point filePoint = this.zone.fileToPoint(fileName);
        if (!this.zone.isPointInZone(filePoint)) {
            AbstractMap.SimpleEntry<String, String> nodeID = null;
            try {
                nodeID = this.findNodeToPoint(filePoint);
            } catch (RemoteException e) {
                System.out.println("Couldn't Find the Node... Aborting the mission for now. After StackTace, Server will still be running for further operations");
                System.out.println(e.getMessage());
                e.printStackTrace();
                return false;
            }
            try {
                NodeInterface nodeStub = this.getNodeStub(nodeID.getKey(), nodeID.getValue());
                if (!nodeStub.insertFile(fileName, filePoint)) {
                    return false;
                }
            } catch (NotBoundException e) {
                e.printStackTrace();
                return false;
            } catch (RemoteException e) {
                e.printStackTrace();
                return false;
            }

        } else {
            this.insertFile(fileName, filePoint);
        }
        return true;
    }

    public NodeInterface getNodeStub(String hostname, String iP) throws NotBoundException, RemoteException {
        try {
            Registry noderegistry = LocateRegistry.getRegistry(iP, Node.port);
            NodeInterface node = (NodeInterface) noderegistry.lookup(hostname);
            return node;
        } catch (RemoteException e) {
            System.out.println("Unable to contact the Node..." + hostname);
            throw e;
        } catch (NotBoundException e) {
            System.out.println("Unable to contact the Node..." + hostname);
            throw e;
        }

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

        ArrayList<NodeInterface> response;
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
            } while (nodeID != null && !this.splitWithNode(nodeID));

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
        for (Map.Entry<String, NodeInterface> entry : this.peers.entrySet()) {
            NodeInterface node = entry.getValue();

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
        Message response;
        try {
            NodeInterface node = this.getNodeStub(nodeID.getKey(), nodeID.getValue());
            response = node.splitNode(this.name,this.nodeaddress.getHostAddress());
            if (response == null) {
                return false;
            }

            this.zone = response.zone;
            if (response.peers != null){
                this.peers = response.peers;
            }
            this.peers.put(nodeID.getKey(), node);

        } catch (Exception e) {
            System.err.println("Client RMI failure couldnt Contact Node " + nodeID.getKey() + " while splitting");
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
            return false;
        }
        return true;
    }


    public Message splitNode(String name, String iP) {
        if (this.zone.height < 2 || this.zone.widht < 2) {
            return null;
        }
        System.out.println("***** \nContacted By remote server. We are now going to split this node.");
        ArrayList<Exception> exceptions = new ArrayList<>();
        Message returnMessage = new Message();
        returnMessage.zone = this.zone.splitZone();
        for (Map.Entry<String, NodeInterface> entry : this.peers.entrySet()) {
            NodeInterface peer = entry.getValue();
            try {
                if (!peer.getZone().zoneShareWall(this.zone)) {
                    returnMessage.peers.put(entry.getKey(), peer);
                }
            } catch (RemoteException e) {
                System.out.println("Error In contacting " + entry.getKey() + " -- " + e.getMessage()
                        + "\nSo, We are going to remove the node from peer list. Detailed Printstack will be printed once we have contacted all the Peers");
                exceptions.add(e);
                this.peers.remove(entry.getKey());
                try {
                    this.dnsStub.deregisterNode(entry.getKey());
                }catch (RemoteException f){
                    f.printStackTrace();
                }
            }
        }
        if (exceptions.size() > 0) {
            for (Exception e : exceptions) {
                e.printStackTrace();
            }
        }

        try {
            NodeInterface nodeStub = getNodeStub(name,iP);
            this.peers.put(name,nodeStub);
        } catch (NotBoundException e) {
            System.out.println("Couldnt contact Host Server. Hence, The host who called from SPlitting cant be added to neighbour");
            e.printStackTrace();
        } catch (RemoteException e) {
            System.out.println("Couldnt contact Host Server. Hence, The host who called from SPlitting cant be added to neighbour");
            e.printStackTrace();
        }

        return returnMessage;

    }


    public void shutdown(Exception exception) {
        //todo -- add it to serverServices...
        System.out.println("Shutting down Client RMI server");
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

        } catch (RemoteException e) {

        } catch (NotBoundException e) {

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
            for (String peername : this.peers.keySet()) {
                NodeInterface peer = this.peers.get(peername);
                returnBuilder.append("\n" + peer.getName());

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
            for (String peerName : this.peers.keySet()) {
                NodeInterface peer = this.peers.get(peerName);
                System.out.println(peer.getName());

            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Zone getZone() {
        return zone;
    }

    @Override
    public HashMap<String, NodeInterface> getPeers() {
        return peers;
    }

    public InetAddress getIP() {
        return nodeaddress;
    }

    void run() {
        Scanner scanner = new Scanner(System.in);
        boolean runAlways = true;
        this.showAvailableComands();
        while (runAlways) {
            String argumet = scanner.nextLine();
            String[] command = argumet.split(" ", 0);

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
                    if (command.length == 1) {
                        System.out.println("Kindly fill all the parameters. The comand List and its syntax is available by \"show\" command");
                        break;
                    } else {
                        this.insertFile(command[1]);
                    }
                    break;
                case "SHOW":
                    this.showAvailableComands();
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
        System.out.println("\n#####################");
        System.out.println("The following commands are available as now");
        System.out.println("1. View  --> Display the information of a specified peer peer where peer is a node identifier, not an IP address. The information includes the node identifier, the IP address, the coordinate, a list of neighbors, and the data items currently stored at the peer. If no peer is given, display the information of all currently active peers.");
        System.out.println("2. Insert --> UnderConstruction");
        System.out.println("3. Show --> To show list of all available commands");
        System.out.println("Exit --> To exit\nPlease type in the command...");
    }
}