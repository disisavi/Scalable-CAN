package edu.s2019.asst1;


import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.*;


public class Zone implements Serializable {
    private static final long serialVersionUID = 3055231095479771380L;
    final static float maxHeight = 10, maxWidth = 10;
    float height, widht;
    Point2D.Float basePoint;
    HashMap<Point2D.Float, ArrayList<String>> fileList = new HashMap<>();

    public Zone() {
        height = Zone.maxHeight;
        widht = Zone.maxWidth;
        basePoint = new Point2D.Float(0, 0);
    }


    public Zone(float height, float width, Point2D.Float basePoint) {
        this.height = height;
        this.widht = width;
        this.basePoint = basePoint;

    }

    public static void main(String[] args) {

        Zone zone = new Zone();
        zone.printZone();
        Zone newZone = zone.splitZone();
        zone.printZone();
        newZone.printZone();
        newZone.splitZone();
        System.out.println(zone.iszoneANeigbour(newZone));
    }

    public Zone splitZone() {

        Point2D.Float newBasePoint = new Point2D.Float();
        float newHeight = -1;
        float newWidth = -1;
        if (widht >= height) {
            newBasePoint.x = basePoint.x + widht / 2;
            newBasePoint.y = basePoint.y;
            this.widht = widht / 2;
            newWidth = widht;
            newHeight = height;
        } else {
            newBasePoint.y = basePoint.y + height / 2;
            newBasePoint.x = basePoint.x;
            this.height = height / 2;
            newHeight = height;
            newWidth = widht;
        }

        Zone newZone = new Zone(newHeight, newWidth, newBasePoint);

        Iterator it = this.fileList.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Point2D.Float, ArrayList<String>> fileEntry = (Map.Entry) it.next();
            if (newZone.isPointInZone(fileEntry.getKey())) {
                newZone.fileList.put(fileEntry.getKey(), fileEntry.getValue());
                it.remove();
            }
        }

        return newZone;
    }

    public boolean iszoneANeigbour(Zone zone) {

        boolean returnValue = false;
        if ((this.basePoint.x == zone.basePoint.x)
                && (this.basePoint.y != zone.basePoint.y)) {
            if ((this.basePoint.y < zone.basePoint.y)
                    && ((this.height + this.basePoint.y) == zone.basePoint.y)) {
                returnValue = true;
            } else if ((zone.basePoint.y < this.basePoint.y)
                    && ((zone.basePoint.y + zone.height) == this.basePoint.y)) {
                returnValue = true;
            }
        } else if ((this.basePoint.y == zone.basePoint.y)
                && (this.basePoint.x != zone.basePoint.x)) {
            if ((this.basePoint.x < zone.basePoint.x)
                    && ((this.widht + this.basePoint.x) == zone.basePoint.x)) {
                returnValue = true;
            } else if ((zone.basePoint.x < this.basePoint.x)
                    && ((zone.basePoint.x + zone.widht) == this.basePoint.x)) {
                returnValue = true;
            }
        }

        return returnValue;
    }

    public boolean mergeZone(Zone zone) {
        if (zoneShareXWall(zone)) {
            if (zone.basePoint.x <= basePoint.x) {
                basePoint.x = zone.basePoint.x;

            }
            this.widht += zone.widht;
        } else if (zoneShareYWall(zone)) {
            if (zone.basePoint.y <= basePoint.y) {
                basePoint.y = zone.basePoint.y;

            }
            this.height += zone.height;
        } else {
            return false;
        }

        zone.basePoint = new Point2D.Float(-1, -1);
        zone.height = -1;
        zone.widht = -1;
        return true;
    }

    public boolean zoneShareWall(Zone zone) {

        return (zoneShareYWall(zone) || zoneShareXWall(zone));

    }

    public boolean zoneShareXWall(Zone zone) {
        boolean returnValue = false;
        if (zone.basePoint.x != basePoint.x && zone.basePoint.y == basePoint.y && height == zone.height) {

            if (zone.basePoint.x < basePoint.x) {
                if (zone.basePoint.x + zone.widht == basePoint.x) {
                    returnValue = true;
                }
            } else if (basePoint.x + widht == zone.basePoint.x) {
                returnValue = true;
            }
        }
        return returnValue;
    }

    public boolean zoneShareYWall(Zone zone) {
        boolean returnValue = false;
        if (zone.basePoint.y != basePoint.y && widht == zone.widht && zone.basePoint.x == basePoint.x) {

            if (zone.basePoint.y < basePoint.y) {
                if (zone.basePoint.y + zone.height == basePoint.y) {
                    returnValue = true;
                }
            } else if (basePoint.y + height == zone.basePoint.y) {
                returnValue = true;
            }

        }
        return returnValue;
    }

    public StringBuilder returnZoneStatus() {
        StringBuilder returnBuilder = new StringBuilder();
        if (height == -1) {
            returnBuilder.append("The zone doesnt exist or hasent been initialised");
        } else {
            returnBuilder.append("****************\n");
            returnBuilder.append("The Zone's boundries are... \n\t[{" + this.basePoint.x + ", " + this.basePoint.y + "}, {"
                    + this.basePoint.x + ", " + (this.basePoint.y + height) + "}, {"
                    + (this.basePoint.x + widht) + ", " + (this.basePoint.y + height) + "}, {"
                    + (this.basePoint.x + widht) + ", " + this.basePoint.y + "}]");
            returnBuilder.append("\nThe File's are.. ");
            Iterator it = this.fileList.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Point2D.Float, ArrayList<String>> fileEntry = (Map.Entry) it.next();
                returnBuilder.append("\nPoint {" + fileEntry.getKey().x + ", " + fileEntry.getKey().y + "} has the following files");
                for (int i = 0; i < fileEntry.getValue().size(); i++) {
                    returnBuilder.append("\n\t" + (i + 1) + "--> " + fileEntry.getValue().get(i));
                }

            }
        }
        return returnBuilder;
    }

    public void printZone() {
        if (height == -1) {
            System.out.println("The zone doesnt exist or hasent been initialised");
        } else {
            System.out.println("****************\n");
            System.out.println("The Zone's boundries are... \n\t[{" + this.basePoint.x + ", " + this.basePoint.y + "}, {"
                    + this.basePoint.x + ", " + (this.basePoint.y + height) + "}, {"
                    + (this.basePoint.x + widht) + ", " + (this.basePoint.y + height) + "}, {"
                    + (this.basePoint.x + widht) + ", " + this.basePoint.y + "}]");
            System.out.println("The File's are.. ");
            Iterator it = this.fileList.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Point2D.Float, ArrayList<String>> fileEntry = (Map.Entry) it.next();
                System.out.println("Point {" + fileEntry.getKey().x + ", " + fileEntry.getKey().y + "} has the following files");
                for (int i = 0; i < fileEntry.getValue().size(); i++) {
                    System.out.println("\t" + (i + 1) + "--> " + fileEntry.getValue().get(i));
                }

            }
        }
    }

    public boolean isPointInZone(Point2D.Float point) {

        return ((this.basePoint.x + this.widht) > point.x)
                && (this.basePoint.y + this.height > point.y)
                && (point.x >= this.basePoint.x)
                && (point.y >= this.basePoint.y);
    }

    //The hash Function
    public Point2D.Float fileToPoint(String fileName) {
        Point2D.Float returnPoint = new Point2D.Float();
        int charAtOdd = 0, charAtEven = 0;

        //string counted from 1
        for (int i = 0; i < fileName.length(); i++) {
            if ((i % 2) == 0) {
                charAtOdd += fileName.charAt(i);
            } else {
                charAtEven += fileName.charAt(i);
            }
        }
        returnPoint.x = charAtOdd % 10;
        returnPoint.y = charAtEven % 10;
//        System.out.println("charAtOdd " + charAtOdd);
//        System.out.println("Charateven " + charAtEven);
        return returnPoint;
    }


    public boolean addFileToPoint(Point2D.Float point, String fileName) {
        if (!isPointInZone(point)) {
            return false;
        }

        if (fileList.get(point) != null) {
            fileList.get(point).add(fileName);
        } else {
            ArrayList<String> toAdd = new ArrayList<>();
            toAdd.add(fileName);
            fileList.put(point, toAdd);
        }

        return true;
    }

    public float distanceToPoint(Point2D.Float point) {
        float distance = (float) Math.sqrt(Math.pow((point.x - this.basePoint.x), 2) + Math.pow((point.y - this.basePoint.y), 2));
        return distance;
    }
}