package edu.s2019.asst1.helper;

import edu.s2019.asst1.Zone;
import edu.s2019.asst1.implement.NodeInterface;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Message implements Serializable {
    private static final long serialVersionUID = -8213236225808957524L;
    public Zone zone;
    public HashMap<String,NodeInterface> peers;

    public static void main(String[] args){
        System.out.println("Yellow");

        HashMap<String,String> hp = null;
        for(Map.Entry<String,String> entry: hp.entrySet()){
            String value = entry.getValue();
            System.out.println(entry.getKey());
        }
    }
}

