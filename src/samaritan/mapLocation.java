package samaritan;

/**Location Types:
 * A - empty
 * B - Soup
 * C -
 * D -
 */

public class mapLocation {
    private int x, y, amt;
    private char locationType;
    public mapLocation(int inX, int inY){
        this.x = inX;
        this.y = inY;
        this.locationType = 'A'; //empty space
    }
    public mapLocation(int inX, int inY, char inLocType){
        this.x = inX;
        this.y = inY;
        this.locationType = inLocType;
    }
    public mapLocation(int inX, int inY, char inLocType, int inAmt){
        this.x = inX;
        this.y = inY;
        this.locationType = inLocType;
        this.amt = inAmt;
    }

    public int getX(){return this.x;}
    public int getY(){return this.y;}
    public int getAmt(){return this.amt;}
    public char getLocationType(){return this.locationType;}
}
