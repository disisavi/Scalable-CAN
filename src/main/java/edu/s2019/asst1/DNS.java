package edu.s2019.asst1;

import edu.s2019.asst1.implement.DNSInterface;
import edu.s2019.asst1.implement.NodeInterface;

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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.Scanner;

public class DNS implements DNSInterface {
    public static final int port = 1024;
    public HashMap<String, NodeInterface> nodesInCAN = new HashMap<>();
    public InetAddress ip;

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

    public static void main(String[] args) {
        DNS dns = new DNS();
        dns.clearConsole();
        try {
            String name = "DNS";
            DNSInterface stub = (DNSInterface) UnicastRemoteObject.exportObject(dns, DNS.port);
            Registry registry = LocateRegistry.createRegistry(DNS.port);
            registry.rebind(name, stub);
            System.out.println("DNS Online");
            System.out.println("DNS information --\nip --> " + dns.ip.getHostAddress());
            System.out.println("On server "+dns.ip.getHostName());
            System.out.println("Nodes stores --> " + dns.nodesInCAN.size());
        } catch (AccessException e) {
            System.out.println("DNS server Failure ...");
            dns.shutdown(e);
        } catch (RemoteException e) {
            System.out.println("DNS server Failure...");
            dns.shutdown(e);
        }

        dns.run();
        dns.shutdown(null);
    }

    @Override
    public ArrayList<NodeInterface> returnNodeList() {
        Object[] values = this.nodesInCAN.values().toArray();
        ArrayList<NodeInterface> returnList = new ArrayList<>();
        if (!nodesInCAN.isEmpty()) {
//            for (int i = 0; i < nodesInCAN.size(); i++) {
//                Random random = new Random();
//                int n = random.nextInt(nodesInCAN.size());
//                returnList.add((NodeInterface) values[n]);
//            }

            return new ArrayList<>(nodesInCAN.values());
        }
        return returnList;
    }

    @Override
    public NodeInterface returnNode(String name) {
        NodeInterface nodeStub = this.nodesInCAN.get(name);
        return nodeStub;
    }

    public ArrayList<NodeInterface> returnAllNodes() {
        return new ArrayList<>(this.nodesInCAN.values());
    }

    public NodeInterface getRegisteryForNode(String name, String iP) throws RemoteException, NotBoundException {
        Registry nodeRegistry = LocateRegistry.getRegistry(iP, Node.port);
        NodeInterface nodeStub = (NodeInterface) nodeRegistry.lookup(name);
        return nodeStub;
    }

    public void registerNode(String name, String iP) {


        NodeInterface nodeStub = null;
        try {
            nodeStub = getRegisteryForNode(name, iP);
            nodesInCAN.put(name, nodeStub);
            System.out.println("\n******************");
            System.out.println("Node Registered");
            System.out.println("Name -- " + name);
            System.out.println("IP -- " + iP);
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (NotBoundException e) {
            e.printStackTrace();
        }
    }

    public void deregisterNode(String name) {
        System.out.println("\nNode Removal requested...");

        if (nodesInCAN.containsKey(name)) {
            nodesInCAN.remove(name);
            System.out.println("Node removed");
            System.out.println("Name -- " + name);
        } else {
            System.out.println("The Node " + name + " Doesn't exist");
        }
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
            registry.unbind("DNS");
            UnicastRemoteObject.unexportObject(this, true);
            Runtime.getRuntime().gc();
        } catch (AccessException e) {
        } catch (RemoteException e) {
        } catch (NotBoundException e) {
        }

        Runtime.getRuntime().gc();

        System.exit(-1);
    }

    void clearConsole() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    void showAvailableComands() {
        System.out.println("\n#####################");
        System.out.println("The following commands are available as now for DNS");
        System.out.println("1. View  --> Display the names of all the Nodes in the network. This list may not be most up to date");
        System.out.println("2. Show --> To show list of all available commands");
        System.out.println("3. Clear --> Clear the console");
        System.out.println("Exit --> To exit\nPlease type in the command...");
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
                    this.printDNS();
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

    public void printDNS() {
        System.out.println("************");
        System.out.println("DNS running on\nIP " + this.ip.getHostAddress());
        System.out.println("Hostname " + this.ip.getHostName());
        if (nodesInCAN.size() > 0) {
            System.out.println("Following nodes are registered");
            for (String key : nodesInCAN.keySet()) {
                System.out.println(key);
            }
        } else {
            System.out.println("No Nodes registered to DNS");
        }
    }
}

/* TODO 
 * 	1. code cleanup. For example i am creating stubs not really required while node splitting
 * 	2. refactor it into possibly better structure 
 * 	3. Add concurency support 
 * 	4. Add support for actual file being added to the CAN
 * 	5. Better console messages to alert the user of actions being taken
 * 	6. Fault tolerence is really required... 
 * 	7. Make a better and more usefull api... Like getall etc methods. 
 */
