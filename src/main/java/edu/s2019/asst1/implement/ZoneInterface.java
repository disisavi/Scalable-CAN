package edu.s2019.asst1.implement;

import edu.s2019.asst1.Zone;

import java.awt.*;

public interface ZoneInterface {

    Zone splitZone();

    boolean mergeZone(Zone zone);

    void printZone();

    boolean isPointInZone(Point point);

    boolean addFileToPoint(Point point, String fileName);

    boolean zoneShareWall(Zone zone);
}
