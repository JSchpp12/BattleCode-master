package samaritan;

/**Location Types:
 * A - empty
 * B - water
 * C - soup
 * D - HQ
 * E -
 */

public class tile {
    private int x, y, amt, elevation;
    private char locationType;
    public tile(int inX, int inY){
        this.x = inX;
        this.y = inY;
        this.locationType = 'A'; //empty space
    }
    public tile(int inX, int inY, int inElevation){
        this.x = inX;
        this.y = inY;
        this.elevation = inElevation;
        this.locationType = 'A'; //empty space
    }
    public tile(int inX, int inY, char inLocType){
        this.x = inX;
        this.y = inY;
        this.locationType = inLocType;
    }
    public tile(int inX, int inY, char inLocType, int inAmt){
        this.x = inX;
        this.y = inY;
        this.locationType = inLocType;
        this.amt = inAmt;
    }
    public tile(int inX, int inY, char inLocType, int inAmt, int inElevation){
        this.x = inX;
        this.y = inY;
        this.locationType = inLocType;
        this.amt = inAmt;
        this.elevation = inElevation;
    }

    public int getX(){return this.x;}
    public int getY(){return this.y;}
    public int getAmt(){return this.amt;}
    public int getElevation(){return this.elevation;}
    public char getLocationType(){return this.locationType;}

    public void setAmt(int inAmt){this.amt = inAmt;}
    public void setElevation(int newElevation){this.elevation = newElevation;}
    public void setLocationType(char inLocType){this.locationType = inLocType;}



}