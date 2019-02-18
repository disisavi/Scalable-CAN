package edu.s2019.asst1.implement;

import edu.s2019.asst1.Node;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.HashMap;

public interface DNSInterface extends Remote {
    HashMap<String, String> returnNodeList() throws RemoteException;
    void registerNode(String name, String iP) throws RemoteException;
    void deregisterNode(Node node) throws RemoteException;
}
