package samaritan;
import battlecode.common.*;
import javafx.scene.control.cell.TextFieldListCell;

import java.util.ArrayList;

public class LocalMap {
    Tile[][] map;
    int currentX, currentY;
    ArrayList<Tile> soups = new ArrayList<>();

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
    public void addTile(int inX, int inY) {
        map[inX][inY] = new Tile(inX, inY);
    }

    /**
     * add soup to the map
     * @param location location of the soup
     * @param soupAmt new amount of soup
     */
    public void addSoup(MapLocation location, int soupAmt) {
        int x, y;
        x = location.x;
        y = location.y;
        if (this.map[x][y] != null) {
            if(!this.map[x][y].getSoup())
                soups.add(this.map[location.x][location.y]);
            this.map[x][y].setSoup(true); //set soup
            this.map[x][y].setSoupAmt(soupAmt); //set soup amount
        }else{
            addTile(location.x, location.y);
            this.map[location.x][location.y] = new Tile(location.x, location.y); //create new tile
            this.map[location.x][location.y].setSoup(true); //make tile contain soup
            this.map[location.x][location.y].setSoupAmt(soupAmt); //set soup amount
            soups.add(this.map[location.x][location.y]);
        }
    }

    /**
     * Remove an amount of soup from a location, and if necessary will clear the space
     * @param location location of soup to be updated
     */
    public void removeSoup(MapLocation location){
        this.map[location.x][location.y].setSoup(false);
        this.map[location.x][location.y].setSoupAmt(0);

        for(int i = 0; i < soups.size(); i++) {
            if(soups.get(i).getX() == location.x && soups.get(i).getY() == location.y) {
                soups.remove(i);
                return;
            }
        }
        System.out.println("ERR - removeSoup: no soups in ArrayList");
    }

    /**
     * clears location of its location data
     * @param xLoc x coordinate of target location
     * @param yLoc y coordinate of target location
     */
    public void clearLocation(int xLoc, int yLoc){this.map[xLoc][yLoc].setLocationType('-');}

    /**
     * update location data at a specific coordinate
     * @param inLocation MapLocation of target tile
     * @param newLocData new location data to be stored
     */
    public void recordLocationData(MapLocation inLocation, char newLocData){
        int x = inLocation.x;
        int y = inLocation.y;

        if (this.map[x][y] == null){
            addTile(x, y);
        }
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
            addTile(x, y);
            this.map[x][y].setElevation(inElevation);
        }
    }

    public void recordEnemy(RobotInfo r) {
        int x, y;
        MapLocation inLocation = r.getLocation();
        x = inLocation.x;
        y = inLocation.y;

        if (this.map[x][y] != null){
            //tile object exists, update/record enemy
        }else {
            //tile object does not exist, create a new one
            addTile(x, y);
        }
        this.map[x][y].setEnemy(true);
    }

    public void clearEnemies() {
        for(int i = 0; i < this.map.length; i++) {
            for(int j = 0; j < this.map[i].length; j++) {
                if(map[i][j] == null) continue;
                map[i][j].setEnemy(false);
            }
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

    public char getLocationData(MapLocation inLocation){
        if (this.map[inLocation.x][inLocation.y] != null){
            return this.map[inLocation.x][inLocation.y].getLocationType();
        }else{
            return '-';
        }

    }
    public int getLocationElevation(MapLocation inLocation){
        return this.map[inLocation.x][inLocation.y].getElevation();
    }
    public int getlocationAmtSoup(MapLocation inLocation){
        return this.map[inLocation.x][inLocation.y].getSoupAmt();
    }
    public int getUpdateTime(MapLocation inLocation) {
        return this.map[inLocation.x][inLocation.y].getUpdateTime();
    }

    public boolean hasBeenScanned(MapLocation inLocation){
        if (this.map[inLocation.x][inLocation.y] != null){
            return true;
        }else{
            return false;
        }
    }

    /*public Tile closestSoup(MapLocation location) {
        //System.out.println("Running Closest Soup");
        int closestDistance = 100000;
        Tile closestSoup = null;
        for(int i = 0; i < map.length; i++) {
            for(int j = 0; j < map[i].length; j++) {
                if(map[i][j] == null) continue;
                if(map[i][j].getSoup() && location.distanceSquaredTo(toMapLocation(map[i][j])) < closestDistance) {
                    //System.out.println("Soup found at " + i + ", " + j);
                    closestSoup = map[i][j];
                    closestDistance = location.distanceSquaredTo(toMapLocation(map[i][j]));
                }
            }
        }
        return closestSoup;

    }*/

    public Tile nextSoup() {
        if(soups.size() <= 0)
            return null;
        return soups.get(0);
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