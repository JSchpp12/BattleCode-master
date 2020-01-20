package samaritan;
import samaritan.Tile;

import java.util.*;

public class EncodedMessage {
    private BitSet[] _messageData;
    private int _currentSet, _nextBit, _teamId;

    public EncodedMessage(){
        //header information will be in BitSet[2]
        this._messageData = new BitSet[7]; //containers for message data (6 locations AND 1 header)
        this._currentSet = 0;
        this._nextBit = 0;
        this._teamId = 0;
    }

    /**
     * Create a bitset for data storage at the specified index
     * @param index where the set will be prepared
     */
    public void createBitset(int index){
        this._messageData[index] = new BitSet();
        this._currentSet = index;
        this._nextBit = 0;
    }

    /**
     * Constructs the header of the message
     * @param teamValue value that will ensure message is coming from team
     * @param previousMessageTurn turn the last message was sent at
     */
    public void createHeader(int teamValue, int previousMessageTurn){
        BitSet headerSet = new BitSet();
        int bitsetCounter = 0;
        this._currentSet = 2; //set to header block
        createBitset(2);

        addInt(teamValue, 16);
        addInt(previousMessageTurn, 16);
        System.out.println("Header encoded message is - " + this._messageData[2].toString());
    }

    /**
     * Decode the team id from the passed message
     * @param message message to test
     * @return returns the teamId in the message
     */
    public int decodeTeamId(int[] message){
        int decodedId;
        //Read in the int that should contain the team id
        addEncodedInt(message[2], 2);

        decodedId = getIntFromBit(2, 0, 15);
        this._teamId = decodedId; //save for later storage if found valid
        //convert block to int
        return decodedId;

        //return that int

    }

    /**
     * Decodes the turn of the last message from the header information
     * @param message message to be decoed
     * @return returns the turn
     */
    public int decodeLastMessageTurn(int[] message){
        int decodedLastTurn;

        //if the header int has not been read into the bitset, do so
        if (this._messageData[2] == null){ addEncodedInt(message[2], 2);}

        //read the turn from the bitset
        decodedLastTurn = getIntFromBit(2, 16, 32);
        System.out.println("Decoded last message turn as - " + decodedLastTurn);

        return decodedLastTurn;
    }

    /**
     * Decodes tile information from target bitset
     * @param index index of target bitset
     * @return array of tile objects
     */
    public Tile getTile(int index){
        Tile tile;
        int x, y, bitCounter, bitStart, bitEnd, soupAmt;
        bitCounter = 0;
        bitStart = 0;
        bitEnd = 7;

        x = getIntFromBit(index, bitStart, bitEnd);
        bitStart += 8;
        bitEnd += 8;

        y = getIntFromBit(index, bitStart, bitEnd);
        bitStart += 8;
        bitEnd +=8;

        tile = new Tile(x, y); //create tile object

        tile.setLocationType(getCharFromBit(index, bitStart, bitEnd));
        bitStart += 8;
        bitEnd += 8;

        soupAmt = getIntFromBit(index, bitStart, bitEnd);
        if (soupAmt != 0){
            tile.setSoupAmt(soupAmt);
            tile.setSoup(true);
        }else{
            tile.setSoup(false);
        }

        return tile;
    }

    /**
     * Add a char to the current integer being constructed
     * @param inChar char to be encoded and added to message
     */
    public void addChar(char inChar){
        int bitsUsed = 0;
        String binaryString = Integer.toBinaryString(inChar);
        System.out.println("Adding char" + binaryString);
        for (int i = binaryString.length() - 1; i >= 0; i--){
            if (binaryString.charAt(i) == '1'){
                this._messageData[this._currentSet].set(this._nextBit, true);
            }
            /*else{
                this._messageData[this._currentSet].set(this._nextBit, true);
            }*/
            bitsUsed++;
            this._nextBit++;
        }

        //pad data so that the entire 8 bits are used
        while(bitsUsed < 8){
            this._messageData[this._currentSet].set(this._nextBit, false);
            bitsUsed++;
            this._nextBit++;
        }
    }

    /**
     * Convert integer into a char size and resulting bits to message -- must be small integer to fit in 8 bits
     * @param value value to be converted
     */
    public void addInt(int value, int targetNumBits){
        int temp = value;
        int numBitsUsed = 0;

        while (value != 0){
            if (value % 2 != 0){
                this._messageData[this._currentSet].set(this._nextBit);
            }
            numBitsUsed++;
            this._nextBit++;
            value = value >> 1;
        }
        while (numBitsUsed < targetNumBits){
            this._nextBit++;
            numBitsUsed++;
        }

    }

    /**
     * Convert encoded int to bitset for computation
     * @param encodedInt integer that is encoded
     * @param index index where int should be stored
     */
    public void addEncodedInt(int encodedInt, int index){
        this._messageData[index] = new BitSet();
        this._currentSet = index;
        addInt(encodedInt, 32);
    }

    /**
     * Convert a specific bitset at index value into an integer -- for sending messages
     * @param index index of int
     * @return int representation of binary encoded data
     */
    public int getInt(int index){
        int value;
        if (this._messageData[index] != null){
            System.out.println("Converting - " + this._messageData[index].toString());
            value = 0;
            for (int i = 0; i < this._messageData[index].length(); i++){
                value += this._messageData[index].get(i) ? (1 << i) : 0;
            }
            return value;
        }else{
            return -1;
        }
    }


    /**
     * Returns the team id from the encoded message -- CALL AFTER VERIFYING MESSAGE
     */
    public int getTeamId(){return this._teamId;}

    /**
     * Decode a char from a message
     * @param charSetIndex index of bitset in message that is being decoded
     * @param start index of bit that decode will start at
     * @param end index of bit that decode will end at
     * @return decoded char from message
     */
    private char getCharFromBit(int charSetIndex, int start, int end){
        return (char)getIntFromBit(charSetIndex, start, end);
    }

    /**
     * Decode an int from a message
     * @param intSetIndex index of bitset in message that is being decoded
     * @param start index of bit that decode will start at
     * @param end index of bit that decode will end at
     * @return decoded int from message
     */
    private int getIntFromBit(int intSetIndex, int start, int end){
        int value = 0;
        int loopCounter = 0;
        for (int i = start; i < end; i++){
            value += this._messageData[intSetIndex].get(i) ? (1 << loopCounter) : 0;
            loopCounter++;
        }
        return value;
    }


}
