package edu.s2019.asst1.implement;

import edu.s2019.asst1.Node;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public interface NodeInterface {
    InetAddress getSelfIP() throws SocketException, UnknownHostException;
    Node splitNode();
    boolean mergeNode(Node node);
    boolean join();
    void leave();
    void printInfo();
    String search(String fileName);
    boolean isPeer(Node node);
}
