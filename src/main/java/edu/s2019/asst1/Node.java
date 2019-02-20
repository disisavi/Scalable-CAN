package edu.s2019.asst1;

import edu.s2019.asst1.implement.DNSInterface;
import edu.s2019.asst1.implement.NodeInterface;
import edu.s2019.asst1.helper.Message;

import java.awt.geom.Point2D;
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
    public static final int port = 11233;
    private static final long serialVersionUID = -6663545639371364731L;
    String name;
    InetAddress nodeaddress;
    private Zone zone;
    private HashMap<String, NodeInterface> peers;
    private DNSInterface dnsStub;
    public boolean isBootstraped;


    public Node() {
        try {
            this.nodeaddress = getSelfIP();
            this.name = nodeaddress.getHostName();
            this.peers = new HashMap<>();
            this.isBootstraped = false;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
    TODO
    1. Im pretty sure there are some reduntant methods. Get rid of them
    1. Go through the code for consistent fault tolerance and relevant messages
    3. Implement  one more command
    5. Add a few comments so that i have some fucking idea what ive done...
    */
    public static void main(String[] args) {
        Node node = new Node();
        node.clearConsole();
        try {


            Registry registry;
            try {
                registry = LocateRegistry.createRegistry(Node.port);
            } catch (RemoteException e) {
                System.out.println("Unable to create registry.... Checking if registry already exist");
                registry = LocateRegistry.getRegistry(Node.port);
            }
            NodeInterface nodeStub = (NodeInterface) UnicastRemoteObject.exportObject(node, Node.port);
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
        // To connect to DNS and register the node, but not bootstrap

        try {
            node.dnsStub = node.setDnsStub();
            node.dnsStub.registerNode(node.name, node.nodeaddress.getHostAddress());
            System.out.println("Node registered to DNS Server");
        } catch (NotBoundException e) {
            System.out.println("Error contacting DNS.. Shutting down server. Please check DNS IP again");
            System.out.println("caused by " + e.getMessage());
            node.shutdown(e);
        } catch (RemoteException e) {
            System.out.println("Error contacting DNS.. Shutting down server. Please check DNS IP again");
            System.out.println("caused by " + e.getMessage());
            node.shutdown(e);
        }
        try {
            node.run();
        } catch (Exception e) {
            System.out.println("Unexpected error caused by " + e.getLocalizedMessage());
            e.printStackTrace();
            System.out.println("Restarting the Node Command server ");
            node.run();
        }

        node.shutdown(null);
    }

    public InetAddress getSelfIP() throws SocketException, UnknownHostException {

        final DatagramSocket socket = new DatagramSocket();
        socket.connect(InetAddress.getByName("8.8.8.8"), DNS.port);
        InetAddress ip = InetAddress.getByName(socket.getLocalAddress().getHostAddress());

        return ip;
    }

    public boolean equals(Node node) {
        return this.nodeaddress.equals(node.nodeaddress) && this.name.equals(node.name);
    }

    public boolean insertFile(String filename, Point2D.Float point) {
        if (isBootstraped) {
            if (this.zone.isPointInZone(point)) {
                this.zone.addFileToPoint(point, filename);
                System.out.println(" ----- Operation performed by remote object --- ");
                System.out.println("File " + filename + "added\n Please find revised Node structure printed now");
                this.printNode();
                return true;
            }
        }
        System.out.println("Couldn't Add file to the point " + point.toString());
        return false;
    }

    public boolean insertFile(String hostname, String fileName) {
        if (hostname.toUpperCase().equals("SELF")
                || hostname.toUpperCase().equals(this.name.toUpperCase())) {
            if (hostname.toUpperCase().equals(this.name.toUpperCase())) {
                System.out.println("Remote Request Starts");
            }
            System.out.println("**** File Will be inserted Locally from Node " + this.name);

            Point2D.Float filePoint = Zone.fileToPoint(fileName);

            if (this.isBootstraped) {
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
            } else {
                System.out.println("The node is not bootstrapped yet. Kindly JOIN the node and then try again");
                return false;
            }
            return true;


        } else {
            System.out.println("****File will be added on the server " + hostname);
            NodeInterface nodeStub = null;
            try {
                nodeStub = this.dnsStub.returnNode(hostname);
                nodeStub.insertFile(hostname, fileName);
            } catch (RemoteException e) {
                System.out.println("Unable to add File because. We are still on with business." + e.getMessage());
                e.printStackTrace();
                return false;
            } catch (NullPointerException e) {
                System.out.println("It seems like the Host doesn't exist on DNS. Kindly check the hostname.");
                return false;
            }

        }

        return false;
    }


    public AbstractMap.SimpleEntry<String, Point2D.Float> searchFile(String hostname, String fileName) {
        if (hostname.toUpperCase().equals("SELF")
                || hostname.toUpperCase().equals(this.name)) {
            if (hostname.toUpperCase().equals(this.name.toUpperCase())) {
                System.out.println("Remote Request Starts for File Search");
            }
            System.out.println("**** File Will be searched Locally from Node " + this.name);


            Point2D.Float fileInPoint = Zone.fileToPoint(fileName);
            System.out.println("File is in Point " + fileInPoint.toString());

            if (!this.zone.isPointInZone(fileInPoint)) {
                AbstractMap.SimpleEntry<String, String> nodeID;
                try {
                    nodeID = findNodeToPoint(fileInPoint);
                } catch (RemoteException e) {
                    System.out.println("Couldn't Find the Node... Aborting the mission for now. After StackTrace, Server will still be running for further operations");
                    System.out.println(e.getMessage());
                    e.printStackTrace();
                    return null;
                }
                try {
                    NodeInterface nodeStub = this.getNodeStub(nodeID.getKey(), nodeID.getValue());
                    AbstractMap.SimpleEntry<String, Point2D.Float> fileInfo = nodeStub.getFile(fileName);
                    this.printReturnedFileInfo(fileInfo, fileName);
                    return fileInfo;
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            } else {
                AbstractMap.SimpleEntry<String, Point2D.Float> fileInfo = null;

                fileInfo = this.getFile(fileName);
                this.printReturnedFileInfo(fileInfo, fileName);
                return fileInfo;
            }
        }

        System.out.println("****File will be searched by the server " + hostname);
        System.out.println("Contacting the server ... ");
        NodeInterface nodeStub;
        AbstractMap.SimpleEntry<String, Point2D.Float> fileInfo = null;
        try {
            nodeStub = this.dnsStub.returnNode(hostname);
            fileInfo = nodeStub.searchFile(hostname, fileName);
            this.printReturnedFileInfo(fileInfo, fileName);
        } catch (RemoteException e) {
            System.out.println("Unable to find File because we couldn't locate the hostname. We are still on with business." + e.getMessage());
            e.printStackTrace();
        } catch (NullPointerException e) {
            System.out.println("It seems like the Host doesn't exist on DNS. Kindly check the hostname.");
            return null;
        }
        return fileInfo;
    }

    void printReturnedFileInfo(AbstractMap.SimpleEntry<String, Point2D.Float> fileInfo, String fileName) {
        if (fileInfo != null && fileInfo.getValue().x != -1) {
            System.out.println("File Found in Node " + fileInfo.getKey());
            System.out.println("File Name -- " + fileName);
            System.out.println("Point Of FIle " + fileInfo.getValue().toString());
        } else {
            System.out.println("It seems like the file wasn't present... Please check file name before trying\nor Maybe we couldnt locate the node which has the file. In that case, relevant stacktrace would have been generated");
        }
    }

    public boolean getIsBootstrapped() {
        return this.isBootstraped;
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

    public void deRegisterNode(String name) {
        if (!(name == null || name.toUpperCase().equals("self"))) {
            if (this.peers.containsKey(name)) {
                this.peers.remove(name);
                try {
                    this.dnsStub.deregisterNode(name);
                } catch (RemoteException e) {
                    System.out.println("Couldn't remove Node from DNS level. Maybe connection to DNS is lost.");
                    e.printStackTrace();
                }
            }
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
        boolean isFirst = true;
        try {
            for (NodeInterface nodestub : response) {
                if (nodestub.getIsBootstrapped()) {
                    isFirst = false;
                    break;
                }
            }
        } catch (RemoteException e) {
            isFirst = true;
            e.printStackTrace();
        }

        if (response.size() == 0 && isFirst) {
            System.out.println("We are the first node to join CAN... ");
            this.zone = new Zone();
            return true;
        } else {
            AbstractMap.SimpleEntry<String, String> nodeID = null;
            do {
                Point2D.Float p2 = getCordinateToBind();
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

    public AbstractMap.SimpleEntry<String, Point2D.Float> getFile(String filename) {
        Point2D.Float filePoint = Zone.fileToPoint(filename);
        AbstractMap.SimpleEntry<String, Point2D.Float> returnMap = null;
        if (this.isBootstraped) {
            if (this.zone.isPointInZone(filePoint)) {
                if (this.zone.fileList.get(filePoint).contains(filename.toUpperCase())) {
                    returnMap = new AbstractMap.SimpleEntry<>(this.name, filePoint);
                } else {
                    returnMap = new AbstractMap.SimpleEntry<>(this.name, new Point2D.Float(-1, -1));
                }
            } else {
                returnMap = new AbstractMap.SimpleEntry<>(this.name, new Point2D.Float(-1, -1));
            }
        } else returnMap = new AbstractMap.SimpleEntry<>(this.name, new Point2D.Float(-1, -1));
        return returnMap;
    }

    public Point2D.Float getCordinateToBind() {
        Random random = new Random();
        float x = random.nextInt((int) Zone.maxWidth);
        float y = random.nextInt((int) Zone.maxHeight);
        return new Point2D.Float(x, y);
    }

    public AbstractMap.SimpleEntry<String, String> routeToNode(NodeInterface node, Point2D.Float point) {
        AbstractMap.SimpleEntry<String, String> response = null;

        if (isBootstraped) {
            try {

                response = node.findNodeToPoint(point);
            } catch (Exception e) {
                System.err.println("Client RMI failure couldnt execute Peer Method while Routing");
                System.out.println("Client exception: " + e.toString());
                e.printStackTrace();
            }
        }
        return response;
    }

    public AbstractMap.SimpleEntry<String, String> findNodeToPoint(Point2D.Float point) throws RemoteException {
        if (this.zone.isPointInZone(point)) {
            AbstractMap.SimpleEntry<String, String> returnEntry = new AbstractMap.SimpleEntry<>(this.name, this.nodeaddress.getHostAddress());
            return returnEntry;
        }

        float low = 11;
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
            response = node.splitNode(this.name, this.nodeaddress.getHostAddress());
            if (response == null) {
                return false;
            }

            this.zone = response.zone;
            if (response.peers != null) {
                this.peers = response.peers;
            } else {
                this.peers = new HashMap<>();
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
        HashMap<String, NodeInterface> tempMap = new HashMap<>();
        Zone tempZone = this.zone.splitZone();
        for (Map.Entry<String, NodeInterface> entry : this.peers.entrySet()) {
            NodeInterface peer = entry.getValue();
            try {
                System.out.println("DEBUG :: " + (peer.getZone().iszoneANeigbour(this.zone)));
                if (!peer.getZone().iszoneANeigbour(this.zone)) {
                    this.peers.remove(entry.getKey());
                }
                if (peer.getZone().iszoneANeigbour(tempZone)) {
                    tempMap.put(entry.getKey(), peer);
                }
            } catch (RemoteException e) {
                System.out.println("Error In contacting " + entry.getKey() + " -- " + e.getMessage()
                        + "\nSo, We are going to remove the node from peer list. Detailed Printstack will be printed once we have contacted all the Peers");
                exceptions.add(e);
                this.peers.remove(entry.getKey());
                try {
                    this.dnsStub.deregisterNode(entry.getKey());
                } catch (RemoteException f) {
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
            NodeInterface nodeStub = getNodeStub(name, iP);
            this.peers.put(name, nodeStub);
        } catch (NotBoundException e) {
            System.out.println("Couldnt contact Host Server. Hence, The host who called from SPlitting cant be added to neighbour");
            e.printStackTrace();
        } catch (RemoteException e) {
            System.out.println("Couldnt contact Host Server. Hence, The host who called from SPlitting cant be added to neighbour");
            e.printStackTrace();
        }
        returnMessage.zone = tempZone;
        returnMessage.peers = tempMap;
        System.out.println("***************\nEnd of request\n###########");
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


        System.exit(-1);

    }

    void view(String nodeName, Boolean showAllFlag) {
        try {
            if (showAllFlag) {
                ArrayList<NodeInterface> allNodesInCAN = this.dnsStub.returnAllNodes();
                for (NodeInterface nodestub : allNodesInCAN) {
                    System.out.println(nodestub.returnNodeStatus());
                }
            } else if ((nodeName.toUpperCase().equals(this.name.toUpperCase()))
                    || (nodeName.toUpperCase().equals("SELF"))) {
                System.out.println(this.returnNodeStatus());
            } else {
                NodeInterface nodeStub = dnsStub.returnNode(nodeName);
                System.out.println(nodeStub.returnNodeStatus());
            }
        } catch (Exception e) {
            System.out.println("Error in Printing Node. Printing StackTrace. After that, we will still be on with business.\nIF such a node exist in DNS,  we will now remove that node from DNS and Neighbour list");
            System.out.println(e.getMessage());
            e.printStackTrace();
            if (nodeName != null) {
                this.deRegisterNode(nodeName);
            }
        }
    }


    public boolean join() {
        if (!this.isBootstraped) {
            if (this.bootstrap()) {
                System.out.println("Bootstrapping success... ");
            } else {

                System.out.println("####################\nBootstrap Error--- We will now repeat the proccess under assumption that we are the fist node and DNS was not updated till now.");

                this.zone = new Zone();
                System.out.println("Bootstrapping success... ");
            }
            this.isBootstraped = true;
            return true;
        }
        System.out.println("This node is already bootstrapped...");
        return false;
    }

    public boolean join(String hostname, Boolean joinAll) {

        try {
            if (joinAll) {
                ArrayList<NodeInterface> nodeStubList;
                try {
                    nodeStubList = this.dnsStub.returnAllNodes();
                } catch (RemoteException e) {
                    System.out.println("Unable to contact DNS... ");
                    e.printStackTrace();
                    return false;
                }
                ArrayList<Exception> exceptions = new ArrayList<>();
                for (NodeInterface nodestub : nodeStubList) {
                    try {
                        if (nodestub.join()) {
                            System.out.println("Node " + nodestub.getName() + " Successfully Bootstrapped and is now part of CAN");
                            System.out.println("Printing Node that joined...");
                            System.out.println(nodestub.returnNodeStatus());
                            return true;
                        }
                    } catch (RemoteException e) {
                        exceptions.add(e);
                        System.out.println("Error In contacting node in CAN -- " + e.getMessage()
                                + "\nSDetailed stackTrace will be printed once we have joined remaining Nodes");
                    }
                    return false;
                }
            } else if (hostname.toUpperCase().equals(this.name.toUpperCase())
                    || hostname.toUpperCase().equals("SELF")) {
                if (this.join()) {
                    System.out.println("The local Node has Now Joined the CAN ");
                    System.out.println(this.returnNodeStatus());
                    return true;
                }
            } else {
                try {
                    NodeInterface nodestub = this.dnsStub.returnNode(hostname);
                    if (nodestub.join()) {
                        System.out.println("Node " + hostname + " Successfully Bootstrapped and is now part of CAN");
                        System.out.println("Printing Node that joined...");
                        System.out.println(nodestub.returnNodeStatus());
                    }
                } catch (RemoteException e) {
                    System.out.println("Couldnt Contact Node " + hostname);
                    e.printStackTrace();
                    return false;
                }

            }
        } catch (Exception e) {
            System.out.println("UnExpected Exception... ");
            System.out.println("Still in business ...");
            e.printStackTrace();
            return false;
        }
        return false;
    }

    public String returnNodeStatus() {
        StringBuilder returnBuilder = new StringBuilder();

        returnBuilder.append("\n*******************************\n");
        returnBuilder.append("IP -- " + this.getIP().getHostAddress());
        returnBuilder.append("\nName -- " + this.name);
        if (this.isBootstraped) {
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
        } else {
            returnBuilder.append("\n The node has not yet been bootstrapped\n");
        }
        return returnBuilder.toString();
    }

    void printNode() {
        System.out.println("\n*******************************");
        System.out.println("IP -- " + this.getIP().getHostAddress());
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
            String[] command = argumet.trim().split(" ", 2);

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
                        String[] insertParam = command[1].split(" ", 0);
                        if (insertParam.length == 1) {
                            System.out.println("Kindly follow the correct Format of Insert command");
                            break;
                        }
                        this.insertFile(insertParam[0], insertParam[1]);
                    }
                    break;
                case "SEARCH":
                    if (command.length == 1) {
                        System.out.println("Kindly fill all the parameters. The command List and its syntax is available by \"show\" command");
                        break;
                    } else {
                        String[] insertParam = command[1].split(" ", 0);
                        if (insertParam.length == 1) {
                            System.out.println("Kindly follow the correct Format of Search command");
                            break;
                        }
                        this.searchFile(insertParam[0], insertParam[1]);
                    }
                    break;
                case "JOIN":
                    boolean joinAll = false;
                    String hostname = null;
                    if (command.length == 1) {
                        joinAll = true;

                    } else {
                        hostname = command[1];
                    }
                    this.join(hostname, joinAll);
                    break;
                case "SHOW":
                    this.showAvailableComands();
                    break;
                case "EXIT":
                    System.out.println("************\nExiting");
                    runAlways = false;
                    break;
                case "CLEAR":
                    this.clearConsole();
                default:
                    System.out.println("Please enter one of the printed commands");
                    this.showAvailableComands();
            }
        }
    }

    void clearConsole() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    void showAvailableComands() {
        System.out.println("\n#####################");
        System.out.println("The following commands are available as now");
        System.out.println("1. View  --> Display the information of a specified peer peer where peer is a node identifier, not an IP address. The information includes the node identifier, the IP address, the coordinate, a list of neighbors, and the data items currently stored at the peer. If no peer is given, display the information of all currently active peers.");
        System.out.println("2. Insert --> Insert NodeName(\"Self\" for local Node) FileName");
        System.out.println("3. Show --> To show list of all available commands");
        System.out.println("4. Clear --> To Clear the console, so that we may better see the results.");
        System.out.println("5. Join --> So that the newly minted node can JOIN the CAN");
        System.out.println("Exit --> To exit\nPlease type in the command...");
    }
}