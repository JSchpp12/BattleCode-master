package examplefuncsplayer;
import battlecode.common.*;

/**
 * contains local form of map -- ONLY 32x32 right now
 * Robot initial location will be set to the middle of the 2d table
 * Map Legend:
 * 1 - current location
 * 2 - HQ
 * 3 -
 */

public class localMap {
    int[][] map;
    MapLocation currentLocation;

    public localMap(MapLocation inInitLocation) {
        this.map = new int[32][32];
        this.currentLocation = inInitLocation;
    }

    public void updateLocation(MapLocation inNewLocation) {
        this.currentLocation = inNewLocation;
    }
}
