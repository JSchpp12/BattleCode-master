package samaritan;
import battlecode.common.*;
import java.util.ArrayList;

/**
 * contains local form of map -- ONLY 32x32 right now
 * Robot initial location will be set to the middle of the 2d table
 * Map Legend:
 * -1 - current location
 * -2 - HQ
 * positive number - soup
 */

public class localMap {
    int[][] map;
    MapLocation currentLocation;
    ArrayList soupAmount = new ArrayList();
    ArrayList soupLocation = new ArrayList();

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
    public void addTile(int locX, int locY, int elevation) {
        map[locX][locY] = elevation;
    }

    public void addSoup(MapLocation location, int soup) {
        soupAmount.add(soup);
        soupLocation.add(location);
    }

    public void removeSoup(MapLocation location) {
        for(int i = 0; i < soupLocation.size(); i++) {
            if(((MapLocation) soupLocation.get(i)).equals(location)) {
                soupLocation.remove(i);
                soupAmount.remove(i);
            }
        }
    }

    public MapLocation closestSoup(MapLocation myLocation) {
        int distance;
        int closest = 100000;
        MapLocation location = null;
        for(int i = 0; i < soupLocation.size(); i++) {
            distance = myLocation.distanceSquaredTo((MapLocation) soupLocation.get(i));
            if(distance < closest) {
                location = (MapLocation) soupLocation.get(i);
                closest = distance;
            }
        }
        return location;
    }

    public int totalSoup(MapLocation myLocation, int radius) {
        int soup = 0;
        for(int i = 0; i < soupLocation.size(); i++) {
             if(myLocation.distanceSquaredTo((MapLocation) soupLocation.get(i)) <= radius)
                 soup += (int) soupAmount.get(i);
        }
        return soup;
    }

}
