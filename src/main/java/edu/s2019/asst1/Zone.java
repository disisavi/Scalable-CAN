package edu.s2019.asst1;

import edu.s2019.asst1.implement.ZoneInterface;
import java.awt.*;



public class Zone implements ZoneInterface {
    int height,widht;
    Point basePoint;

    public Zone(){
        height = 100;
        widht = 100;
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

        return  new Zone(newHeight,newWidth,newBasePoint);
    }

    public boolean mergeZone(Zone zone)
    {
        //Add conditions regarding the wall adjacency in case the current defined conditions are met, but they still dont share a wall.
        if(zone.basePoint.x != basePoint.x && zone.basePoint.y == basePoint.y && height == zone.height) {
            if (zone.basePoint.x <= basePoint.x) {
                basePoint.x = zone.basePoint.x;

            }
            this.widht+=zone.widht;
        }
        else if(zone.basePoint.y != basePoint.y && widht == zone.widht && zone.basePoint.x == basePoint.x) {
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

    public void printZone()
    {
        if(height == -1 )
        {
            System.out.println("The zone doesnt exist or hasent been initialised");
        }
        else {
            System.out.println("\n\n****************\n");
            System.out.println("The Zone's 4 corners are (in X Y format) widht and height are " + widht + " " + height);
            System.out.println("Bottom Left --> " + this.basePoint.x + " " + this.basePoint.y);
            System.out.println("Upper Left --> " + this.basePoint.x + " " + (this.basePoint.y + height));
            System.out.println("Upper Right --> " + (this.basePoint.x + widht) + " " + (this.basePoint.y + height));
            System.out.println("Bottom Right --> " + (this.basePoint.x + widht) + " " + this.basePoint.y);
            System.out.println("************************\n");
        }
    }

    public static void main(String[] args){

        Zone zone = new Zone();
        Zone newZone = zone.splitZone();
        Zone tempZone = zone.splitZone();
        tempZone = zone.splitZone();
        newZone = tempZone.splitZone();
        newZone.printZone();
        zone.printZone();
        tempZone.printZone();
        System.out.println(newZone.mergeZone(zone));
        System.out.println(newZone.mergeZone(tempZone));
        zone.printZone();
        newZone.printZone();
    }


}
