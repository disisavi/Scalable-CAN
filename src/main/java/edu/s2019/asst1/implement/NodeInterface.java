package edu.s2019.asst1.implement;

import edu.s2019.asst1.Zone;
import edu.s2019.asst1.helper.Message;

import java.awt.*;
import java.net.InetAddress;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.AbstractMap;
import java.util.ArrayList;


public interface NodeInterface extends Remote {


    Zone getZone() throws RemoteException;

    ArrayList<NodeInterface> getPeers() throws RemoteException;

    InetAddress getIP() throws RemoteException;

    String getName() throws RemoteException;

    boolean splitWithNode(AbstractMap.SimpleEntry<String, String> nodeID) throws RemoteException;

    public AbstractMap.SimpleEntry<String, String> routeToNode(NodeInterface node, Point point) throws RemoteException;

    Point getCordinateToBind() throws RemoteException;

    AbstractMap.SimpleEntry<String, String> findNodeToPoint(Point point) throws RemoteException;

    Message splitNode() throws RemoteException;

    String returnNodeStatus() throws RemoteException;

    public boolean insertFile(String filename, Point point) throws RemoteException;
}


