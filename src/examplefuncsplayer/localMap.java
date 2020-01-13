package examplefuncsplayer;
import battlecode.common.*;

/**
 * contains local form of map -- ONLY 32x32 right now
 * Robot initial location will be set to the middle of the 2d table
 * Map Legend:
 * 1 - current location
 * 2 - HQ
 * 3 - soup
 */

public class localMap {
    int[][] map;
    MapLocation currentLocation;

    public localMap(MapLocation inInitLocation, int sizeX, int sizeY) {
        this.map = new int[sizeX][sizeY];
        this.currentLocation = inInitLocation;
    }

    public void updateLocation(MapLocation inNewLocation) {
        this.currentLocation = inNewLocation;
    }

    /**
     * Add soup to the local map
     * @param locX x location of soup
     * @param locY y location of soup
     */
    public void addSoup(int locX, int locY){
        map[locX][locY] = 3; 
    }

}
