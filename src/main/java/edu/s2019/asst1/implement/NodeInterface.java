package edu.s2019.asst1.implement;

import edu.s2019.asst1.Zone;
import edu.s2019.asst1.helper.Message;

import java.awt.geom.Point2D;
import java.net.InetAddress;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.AbstractMap;
import java.util.HashMap;


public interface NodeInterface extends Remote {


    Zone getZone() throws RemoteException;

    void setPeers(String name, NodeInterface peer) throws RemoteException;

    void clearPeers() throws RemoteException;

    void clearZone() throws RemoteException;

    boolean leave() throws RemoteException;

    void clearIsBootstrap () throws RemoteException;

    InetAddress getIP() throws RemoteException;

    String getName() throws RemoteException;

    boolean splitWithNode(AbstractMap.SimpleEntry<String, String> nodeID) throws RemoteException;

    AbstractMap.SimpleEntry<String, String> routeToNode(NodeInterface node, Point2D.Float point) throws RemoteException;

    Point2D.Float getCordinateToBind() throws RemoteException;

    AbstractMap.SimpleEntry<String, String> findNodeToPoint(Point2D.Float point) throws RemoteException;

    Message splitNode(String name, String iP) throws RemoteException;

    String returnNodeStatus() throws RemoteException;

    boolean insertFile(String filename, Point2D.Float point) throws RemoteException;

    boolean insertFile(String hostname, String fileName) throws RemoteException;

    AbstractMap.SimpleEntry<String, Point2D.Float> searchFile(String hostname, String fileName) throws RemoteException;

    AbstractMap.SimpleEntry getFile(String filename) throws RemoteException;

    boolean join() throws RemoteException;

    boolean getIsBootstrapped() throws RemoteException;
}
