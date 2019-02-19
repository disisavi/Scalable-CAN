package edu.s2019.asst1.helper;

import edu.s2019.asst1.Zone;
import edu.s2019.asst1.implement.NodeInterface;

import java.io.Serializable;
import java.util.ArrayList;

public class Message implements Serializable {
    private static final long serialVersionUID = -8213236225808957524L;
    public Zone zone;
    public ArrayList<NodeInterface> peers = new ArrayList<>();
}

