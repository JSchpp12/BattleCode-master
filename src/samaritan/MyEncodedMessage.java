package samaritan;

import java.util.BitSet;

public class MyEncodedMessage {

    private int[][] _messageData;
    private int _currentSet, _teamId, _currentIndex;

    public MyEncodedMessage(){
        //header information will be in BitSet[2]
        this._messageData = new int[7][]; //containers for message data (6 locations AND 1 header)
        this._currentSet = 0;
        this._teamId = 0;
    }

    public void createHeader(int teamId, int previousMessageTurn) {
        this._messageData[2][0] = teamId;
        this._messageData[2][1] = previousMessageTurn;
    }

    public void createBitset(int index) {
        this._messageData[index] = new int[4];
        this._currentSet = index;
        this._currentIndex = 0;
    }


    public void addInt(int value) {
        this._messageData[this._currentSet][this._currentIndex] = value;
        this._currentIndex++;
    }

    public void addChar(char locationType) {
        this._messageData[this._currentSet][this._currentIndex] = locationType;
        this._currentIndex++;
    }

    public int decodeTeamId(int[] message) {
        return (int)Math.floor((double)message[2]/65535);
    }

    public int getInt(int i) {
        int temp = -1;
        if(i == 2) {
            temp = this._messageData[2][0]*65535 + this._messageData[2][1];
        } else if(this._messageData[i] != null){
            temp = this._messageData[i][0]*65535 + this._messageData[i][1]*256 + this._messageData[i][2];
        }
        return temp;
    }

    public void addBigInt(int index, int value) {
        this._messageData[index][0] = value;
    }

    public Tile getTile(int index) {
        int bigInt = this._messageData[index][0];
        char locType = (char) (Math.floor(bigInt/65535));
        bigInt = bigInt%65535;
        int x = (int) Math.floor(bigInt/256);
        bigInt = bigInt%256;
        int y = bigInt;
        Tile temp = new Tile(x, y);
        temp.setLocationType(locType);
        return temp;
    }

    public int decodeLastMessageTurn(int[] message) {
        return message[2]%65535;
    }
}
