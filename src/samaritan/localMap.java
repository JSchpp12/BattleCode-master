package samaritan;
import battlecode.common.*;

public class localMap {
    tile[][] map;
    int currentX, currentY;
    /*
    MapLocation currentLocation;
    ArrayList soupAmount = new ArrayList();
    ArrayList soupLocation = new ArrayList();
    */

    /**
     * Create a new localMap object -- used when robot is created
     * @param inInitLocation mapLocation object containing the location information to be written
     * @param locType object type that is being stored on the map, should be a robot or building at this point
     * @param sizeX size of the map along the x-axis
     * @param sizeY size of the map along the y-axis
     */
    public localMap(MapLocation inInitLocation, char locType, int sizeX, int sizeY) {
        int x = inInitLocation.x;
        int y = inInitLocation.y;
        this.map = new tile[sizeX][sizeY];
        this.map[inInitLocation.x][inInitLocation.y] = new tile(x, y, locType);
    }

    /**
     * Move the robot to a new location
     * @param inNewLocation location the robot is moving to
     */
    public void moveRobotLocation(MapLocation inNewLocation) {
        int elevation;
        char robotType;
        System.out.println("Moving location from : (" + this.currentX + " , " + this.currentY + ")" );
        System.out.println("To: ( " + inNewLocation.x + " , " + inNewLocation.y + ")");

        robotType = this.map[this.currentX][this.currentY].getLocationType(); //get the robot type
        clearLocation(this.currentX, this.currentY); //clear previous location

        //move current location to new location
        this.currentX = inNewLocation.x;
        this.currentY = inNewLocation.y;

        //check if new tile object exists in memory
        if (this.map[this.currentX][this.currentY] != null){
            System.out.println("Creating new tile object...");

            //new location tile exists, save location information
            this.map[this.currentX][this.currentY].setLocationType(robotType);
        }else{
            //create new map tile, as old does not exists
            this.map[this.currentX][this.currentY] = new tile(this.currentX, this.currentY, robotType );
        }
        System.out.println("---Move complete---");
    }

    /**
     * Add soup to the local map
     * @param inX x location of soup
     * @param inY y location of soup
     * @param inElevation the elevation of the tile
     */
    public void addTile(int inX, int inY, int inElevation) {
        //map[locX][locY] = elevation;
        map[inX][inY] = new tile(inX, inY, inElevation);
    }

    /**
     * add soup to the map
     * @param location location of the soup
     * @param soupAmt new amount of soup
     */
    public void addSoup(MapLocation location, int soupAmt) {
        this.map[location.x][location.y] = new tile(location.x, location.y, 'C', soupAmt);
    }

    /*
    public void removeSoup(MapLocation location) {
        for(int i = 0; i < soupLocation.size(); i++) {
            if(((MapLocation) soupLocation.get(i)).equals(location)) {
                soupLocation.remove(i);
                soupAmount.remove(i);
            }
        }
    }
    */

    /**
     * Remove an amount of soup from a location, and if necessary will clear the space
     * @param location location of soup to be updated
     * @param soupRemoved amount of soup that is being removed
     */
    public void removeSoup(MapLocation location, int soupRemoved){
        int amt;
        amt = this.map[location.x][location.y].getAmt();

        amt -= soupRemoved;
        if (amt <= 0){
            //no soup remains, clear spot
            clearLocation(location.x, location.y);
        }else{
            //update amount of soup at location
            this.map[location.x][location.y].setAmt(amt);
        }
    }

    /*
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
*/

    /**
     * clears location of its location data
     * @param xLoc x coordinate of target location
     * @param yLoc y coordinate of target location
     */
    public void clearLocation(int xLoc, int yLoc){this.map[xLoc][yLoc].setLocationType('A');}

    /**
     * update location data at a specific coordinate
     * @param inLocation MapLocation of target tile
     * @param newLocData new location data to be stored
     */
    public void updateLocationData(MapLocation inLocation, char newLocData){
        int x = inLocation.x;
        int y = inLocation.y;

        this.map[x][y].setLocationType(newLocData);
    }
}
