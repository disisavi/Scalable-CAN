package edu.s2019.asst1.implement;

import edu.s2019.asst1.Node;
import edu.s2019.asst1.Zone;

import java.awt.*;
import java.net.InetAddress;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.AbstractMap;
import java.util.ArrayList;


public interface NodeInterface extends Remote {
    boolean equals(Node node) throws RemoteException;

    Zone getZone() throws RemoteException;

    ArrayList<NodeInterface> getPeers() throws RemoteException;

    InetAddress getIP() throws RemoteException;

    String getName() throws RemoteException;

    boolean splitWithNode(AbstractMap.SimpleEntry<String, String> nodeID) throws RemoteException;

    AbstractMap.SimpleEntry<String, String> routeToNode(String nodeName, String nodeIP, Point point) throws RemoteException;

    Point getCordinateToBind() throws RemoteException;

    AbstractMap.SimpleEntry<String, String> findNodeToPoint(Point point) throws RemoteException;

    Node.Message splitNode() throws RemoteException;

    void shutdown() throws RemoteException;
}


