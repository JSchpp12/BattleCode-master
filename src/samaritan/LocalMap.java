package samaritan;
import battlecode.common.*;

public class LocalMap {
    Tile[][] map;
    int currentX, currentY;

    /**
     * Create a new localMap object -- used when robot is created
     * @param inInitLocation mapLocation object containing the location information to be written
     * @param robotType object type that is being stored on the map, should be a robot or building at this point
     * @param sizeX size of the map along the x-axis
     * @param sizeY size of the map along the y-axis
     */
    public LocalMap(MapLocation inInitLocation, char robotType, int sizeX, int sizeY) {
        int x = inInitLocation.x;
        int y = inInitLocation.y;
        this.map = new Tile[sizeX][sizeY];
        this.map[inInitLocation.x][inInitLocation.y] = new Tile(x, y); //create new tile
        this.map[inInitLocation.x][inInitLocation.y].setLocationType(robotType); //set robot at the new tile
    }

    /**
     * Move the robot to a new location
     * @param inNewLocation location the robot is moving to
     */
    public void moveRobotLocation(MapLocation inNewLocation, int inTurnCount) {
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
            this.map[this.currentX][this.currentY] = new Tile(this.currentX, this.currentY);

            //set location data to robot type for the current tile
            this.map[this.currentX][this.currentY].setLocationType(robotType);
        }
        System.out.println("---Move complete---");
    }

    /**
     * Create blank tile object at location
     * @param inX x location of soup
     * @param inY y location of soup
     */
    public void addTile(int inX, int inY, int inTurnCount) {
        map[inX][inY] = new Tile(inX, inY);
    }

    /**
     * add soup to the map
     * @param location location of the soup
     * @param soupAmt new amount of soup
     */
    public void addSoup(MapLocation location, int soupAmt, int inTurnCount) {
        int x, y;
        x = location.x;
        y = location.y;
        if (this.map[x][y] != null) {
            this.map[x][y].setSoup(true); //set soup
            this.map[x][y].setSoupAmt(soupAmt); //set soup amount
            System.out.println("Modifying Soup Tile");
        }else{
            this.map[location.x][location.y] = new Tile(location.x, location.y); //create new tile
            this.map[location.x][location.y].setSoup(true); //make tile contain soup
            this.map[location.x][location.y].setSoupAmt(soupAmt); //set soup amount
            System.out.println("Creating new soup tile");
        }
    }

    /**
     * Remove an amount of soup from a location, and if necessary will clear the space
     * @param location location of soup to be updated
     * @param soupRemoved amount of soup that is being removed
     */
    public void removeSoup(MapLocation location, int soupRemoved){
        int amt;
        amt = this.map[location.x][location.y].getSoupAmt();

        amt -= soupRemoved;
        if (amt <= 0){
            //no soup remains, clear spot
            clearLocation(location.x, location.y);
        }else{
            //update amount of soup at location
            this.map[location.x][location.y].setSoupAmt(amt);
        }
    }

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
    public void recordLocationData(MapLocation inLocation, char newLocData){
        int x = inLocation.x;
        int y = inLocation.y;

        this.map[x][y].setLocationType(newLocData);
    }

    /**
     * Store elevation data at a specific location
     * @param inLocation target location
     * @param inElevation elevation data to be stored
     */
    public void recordElevation(MapLocation inLocation, int inElevation){
        int x, y;
        x = inLocation.x;
        y = inLocation.y;

        if (this.map[x][y] != null){
            //tile object exists, update/record elevation data
            this.map[x][y].setElevation(inElevation);
        }else{
            //tile object does not exist, create a new one
            this.map[x][y] = new Tile(x,y);
            this.map[x][y].setElevation(inElevation);
        }
    }


    /**
     * Update the amount of pollution at a location -- will throw exception if tile does not exist
     * @param inLocation target location
    // * @param inPollution amount of pollution to be recorded at that location
     */
    /*
    public void recordPollution(MapLocation inLocation, int inPollution, int inTurnCount) throws NullPointerException {
        int x, y;
        x = inLocation.x;
        y = inLocation.y;
        if (this.map[x][y] != null){
            this.map[x][y].setPollution();
        }else{
            throw new NullPointerException("Tile Does Not Exist");
        }
    }
    */

    public char getLocationData(MapLocation inLocation){return this.map[inLocation.x][inLocation.y].getLocationType();}
    public int getLocationElevation(MapLocation inLocation){return this.map[inLocation.x][inLocation.y].getElevation();}
    public int getlocationAmtSoup(MapLocation inLocation){return this.map[inLocation.x][inLocation.y].getSoupAmt();}
    public int getUpdateTime(MapLocation inLocation) {return this.map[inLocation.x][inLocation.y].getUpdateTime(); }

    public Tile closestSoup(MapLocation location) {
        //System.out.println("Running Closest Soup");
        int closestDistance = 100000;
        Tile closestSoup = null;
        for(int i = 0; i < map.length; i++) {
            for(int j = 0; j < map[i].length; j++) {
                if(map[i][j] == null) continue;
                if('C' == map[i][j].getLocationType() && location.distanceSquaredTo(toMapLocation(map[i][j])) < closestDistance) {
                    System.out.println("Soup found at " + i + ", " + j);
                    closestSoup = map[i][j];
                    closestDistance = location.distanceSquaredTo(toMapLocation(map[i][j]));
                }
            }
        }
        return closestSoup;

    }

    //turns a tile into a MapLocation for easy processing
    public static MapLocation toMapLocation(Tile t) {
        if(t == null)
            return null;
        return new MapLocation(t.getX(), t.getY());
    }

    //Returns the distance squared between
    public static int distanceBetween(Tile t1, Tile t2) { return toMapLocation(t1).distanceSquaredTo(toMapLocation(t2)); }
}