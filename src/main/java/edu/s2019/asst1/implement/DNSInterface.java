package edu.s2019.asst1.implement;

import edu.s2019.asst1.Node;

import java.net.InetAddress;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;

public interface DNSInterface extends Remote {
    HashMap<String, String> returnNodeList() throws RemoteException;
    void registerNode(Node node) throws RemoteException;
    void deregisterNode(Node node) throws RemoteException;
}
