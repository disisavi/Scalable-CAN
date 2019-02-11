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

    public void mergeZone(Zone zone)
    {
        //todo
    }

    public void printZone()
    {
        System.out.println("\n\n\n****************\n");
        System.out.println("The Zone's 4 corners are (in X Y format) widht and height are "+widht+" "+height);
        System.out.println("Bottom Left --> "+this.basePoint.x+" "+this.basePoint.y);
        System.out.println("Upper Left --> "+this.basePoint.x+" "+(this.basePoint.y+height));
        System.out.println("Upper Right --> "+(this.basePoint.x+widht)+" "+(this.basePoint.y+height));
        System.out.println("Bottom Right --> "+(this.basePoint.x+widht)+" "+this.basePoint.y);
    }

    public static void main(String[] args){
        // basic Checks

        Zone zone = new Zone();
        zone.printZone();
        Zone newZone = zone.splitZone();
        zone.printZone();
        newZone.printZone();
        zone.splitZone().printZone();
        zone.printZone();

    }


}
