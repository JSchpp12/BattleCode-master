package samaritan;

import battlecode.common.RobotController;

import static samaritan.RobotPlayer.rc;

/**Location Types:
 * - - empty
 * B - water
 * D - HQ
 * E - COW
 * F - MINER
 * G - NET_GUN
 * H - REFINERY
 * I - VAPORATOR
 * J - LANDSCAPER
 * K - DESIGN_SCHOOL
 * L - DELIVERY_DRONE
 * M - FULFILLMENT_CENTER
 */

public class Tile {
    private int x, y, soupAmt, pollutionAmt, elevation, turnUpdated;
    private char locationType;
    private boolean isEnemy, hasSoup, isPollution;
    public Tile(int inX, int inY){
        this.x = inX;
        this.y = inY;
        this.locationType = '-'; //empty space
        this.turnUpdated = rc.getRoundNum();
    }

    public int getX(){return this.x;}
    public int getY(){return this.y;}
    public boolean getSoup(){return this.hasSoup;}
    public boolean hasEnemy(){return this.isEnemy;}
    public int getSoupAmt(){return this.soupAmt;}
    public int getElevation(){return this.elevation;}
    public int getPollution(){return this.pollutionAmt;}
    public char getLocationType(){return this.locationType;}
    public int getUpdateTime(){return this.turnUpdated;}

    public void setSoup(boolean inHasSoup){this.hasSoup = inHasSoup; this.turnUpdated = rc.getRoundNum(); }
    public void setEnemy(boolean inHasEnemy){this.hasSoup = inHasEnemy; this.turnUpdated = rc.getRoundNum(); }
    public void setSoupAmt(int inSoupCount){this.soupAmt = inSoupCount; this.turnUpdated = rc.getRoundNum();  }
    public void setElevation(int newElevation){this.elevation = newElevation; this.turnUpdated = rc.getRoundNum(); }
    public void setPollution(int inPollution){this.pollutionAmt = inPollution; this.turnUpdated = rc.getRoundNum(); }
    public void setLocationType(char inLocType){this.locationType = inLocType; this.turnUpdated = rc.getRoundNum(); }
}
