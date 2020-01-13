//this class is a container to hold future moves and the next move
package samaritan;
import java.util.*;

public class futureMove {
    private int[] moveList; //array to contain the future moves
    private int nextMove = 0;

    public futureMove(int[] inMoveList) {
        this.moveList = inMoveList;
    }

    public int getNextMove() {
        int returnMove = this.moveList[nextMove];
        this.nextMove++;
        return returnMove;
    }
}
