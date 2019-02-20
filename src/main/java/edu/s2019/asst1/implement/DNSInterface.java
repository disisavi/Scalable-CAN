package edu.s2019.asst1.implement;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;


public interface DNSInterface extends Remote {
    ArrayList<NodeInterface> returnNodeList() throws RemoteException;

    void registerNode(String name, String iP) throws RemoteException;

    void deregisterNode(String name) throws RemoteException;

    ArrayList<NodeInterface> returnAllNodes() throws RemoteException;

    NodeInterface returnNode(String name) throws RemoteException;
}
