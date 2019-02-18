package edu.s2019.asst1;

import edu.s2019.asst1.implement.ZoneInterface;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


public class Zone implements ZoneInterface {
    final static int maxHeight = 10, maxWidth = 10;
    int height,widht;
    Point basePoint;
    HashMap<Point, ArrayList<String>> fileList = new HashMap<>();

    public Zone(){
        height = Zone.maxHeight;
        widht = Zone.maxWidth;
        basePoint = new Point(0,0);
    }


    public Zone(int height, int width,Point basePoint)
    {
        this.height = height;
        this.widht = width;
        this.basePoint= basePoint;

    }
    public Zone splitZone(){

        Point newBasePoint = new Point();
        int newHeight = -1;
        int newWidth = -1;
        if (widht >= height)
        {
            newBasePoint.x = basePoint.x+widht/2;
            newBasePoint.y = basePoint.y;
            this.widht = widht/2;
            newWidth = widht;
            newHeight = height;
        }
        else{
            newBasePoint.y = basePoint.y + height/2;
            newBasePoint.x = basePoint.x;
            this.height = height/2;
            newHeight = height;
            newWidth  = widht;
        }

        Zone newZone = new Zone(newHeight,newWidth,newBasePoint);

        Iterator it = this.fileList.entrySet().iterator();
        while (it.hasNext())
        {
            Map.Entry<Point,ArrayList<String>> fileEntry = (Map.Entry)it.next();
            if(newZone.isPointInZone(fileEntry.getKey()))
            {
                newZone.fileList.put(fileEntry.getKey(),fileEntry.getValue());
                it.remove();
            }
        }

        return  newZone;
    }

    public boolean mergeZone(Zone zone)
    {
        if(zoneShareXWall(zone)) {
            if (zone.basePoint.x <= basePoint.x) {
                basePoint.x = zone.basePoint.x;

            }
            this.widht+=zone.widht;
        }
        else if(zoneShareYWall(zone)) {
            if (zone.basePoint.y <= basePoint.y) {
                basePoint.y = zone.basePoint.y;

            }
            this.height += zone.height;
        }
        else{
            return false;
        }

        zone.basePoint = new Point(-1,-1);
        zone.height = -1;
        zone.widht = -1;
        return true;
    }

    public boolean zoneShareWall(Zone zone){
        if(zoneShareXWall(zone)) {
            return true;
        }
        else if(zoneShareYWall(zone)) {
            return true;
        }

        return false;
    }

    public boolean zoneShareXWall(Zone zone){
        boolean returnValue = false;
        if(zone.basePoint.x != basePoint.x && zone.basePoint.y == basePoint.y && height == zone.height) {

            if(zone.basePoint.x <  basePoint.x){
                if(zone.basePoint.x + zone.widht == basePoint.x)
                {
                    returnValue = true;
                }
            } else if (basePoint.x + widht == zone.basePoint.x){
                returnValue = true;
            }
        }
        return returnValue;
    }

    public boolean zoneShareYWall(Zone zone){
        boolean returnValue = false;
        if(zone.basePoint.y != basePoint.y && widht == zone.widht && zone.basePoint.x == basePoint.x){

            if(zone.basePoint.y <  basePoint.y){
                if(zone.basePoint.y + zone.height == basePoint.y)
                {
                    returnValue = true;
                }
            } else if (basePoint.y + height == zone.basePoint.y){
                returnValue = true;
            }

        }
        return returnValue;
    }

    public void printZone()
    {
        if(height == -1 )
        {
            System.out.println("The zone doesnt exist or hasent been initialised");
        }
        else {
            System.out.println("****************\n");
            System.out.println("The Zone's boundries are... \n\t[{" + this.basePoint.x + ", " + this.basePoint.y+"}, {"
            + this.basePoint.x + ", " + (this.basePoint.y + height)+"}, {" 
            + (this.basePoint.x + widht) + ", " + (this.basePoint.y + height)+"}, {"
            + (this.basePoint.x + widht) + ", " + this.basePoint.y+"}]");
            System.out.println("The File's are.. ");
            Iterator it = this.fileList.entrySet().iterator();
            while (it.hasNext())
            {
                Map.Entry<Point,ArrayList<String>> fileEntry = (Map.Entry)it.next();
                System.out.println("Point {"+fileEntry.getKey().x+", "+fileEntry.getKey().y+"} has the following files");
                for(int i = 0; i < fileEntry.getValue().size(); i++){
                    System.out.println("\t"+(i+1)+"--> "+fileEntry.getValue().get(i));
                }

            }
        }
    }

    public boolean isPointInZone(Point point){

        if(((this.basePoint.x+this.widht) > point.x)
        && (this.basePoint.y + this.height > point.y)
        && (point.x >= this.basePoint.x)
        &&( point.y >= this.basePoint.y)){
            return true;
        }
        return false;
    }

    public boolean addFileToPoint(Point point,String fileName){
        if(!isPointInZone(point)){
            return false;
        }

        if(fileList.get(point)!= null){
            fileList.get(point).add(fileName);
        }
        else {
            ArrayList<String> toAdd = new ArrayList<>();
            toAdd.add(fileName);
            fileList.put(point,toAdd);
        }

        return true;
    }
    public int distanceToPoint(Point point){
        int distance = (int) Math.sqrt(Math.pow((point.x - this.basePoint.x),2) + Math.pow((point.y - this.basePoint.y),2));
        return distance;
    }
    public static void main(String[] args){

        Zone zone = new Zone();
        zone.printZone();
        Zone newZone = zone.splitZone();
        zone.printZone();
        newZone.printZone();
        System.out.println(zone.zoneShareWall(newZone));
    }
}