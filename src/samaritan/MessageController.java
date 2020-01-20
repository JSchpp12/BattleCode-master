package samaritan;

import samaritan.Tile;

public class MessageController {
    private EncodedMessage _encodedMessage;
    private int _numIntsEncoded;
    private final int[] _intOrder = {2, 0, 1, 3, 4, 5, 6};

    public MessageController(){
    }

    /**
     * Create a new message with header information
     */
    public void createMapMessage(int turnNumber, int previousMessageTurn){
        int teamId;
        this._encodedMessage = new EncodedMessage();
        teamId = calculateTeamId(turnNumber);
        //System.out.println("Team Id Calculated as - " + teamId);

        this._encodedMessage.createHeader(teamId, previousMessageTurn);
        this._numIntsEncoded = 1;
    }

    /**
     * Validate teamId information
     * @param message
     * @param messageTurn
     * @return
     */
    public boolean validateMessage(int[] message, int messageTurn){
        int decodedId;
        this._encodedMessage = new EncodedMessage();

        //get team id from message
        decodedId = this._encodedMessage.decodeTeamId(message);
        //System.out.println("Decoded team id is - "  + decodedId );
        return verifyTeamId(decodedId, messageTurn);
    }

    /**
     * Encode a tile into the message
     * @param tile tile information to be encoded
     * @return return false if current message is full
     */
    public boolean encodeLocation(Tile tile){
        if (this._numIntsEncoded < 7){
            _encodedMessage.createBitset(this._intOrder[this._numIntsEncoded]);
            //this._message.createBitset(this._numIntsEncoded);
            _encodedMessage.addInt(tile.getX(), 8);
            _encodedMessage.addInt(tile.getY(), 8);
            _encodedMessage.addChar(tile.getLocationType());
            _encodedMessage.addChar('0');
            this._numIntsEncoded++;
            return true;
        }else {
            return false;
        }
    }

    public int[] createCommandMessage(int commandType, int turnNumber){
        this._encodedMessage = new EncodedMessage();
        int[] message = new int[3];
        message[2] = calculateTeamId(turnNumber);
        message[0] = commandType;
        return message;
    }

    public int[] createCommandMessage(int x, int y, int commandType, int turnNumber){
        this._encodedMessage = new EncodedMessage();
        int[] message = new int[4];
        message[2] = calculateTeamId(turnNumber);
        message[0] = commandType;
        message[1] = x;
        message[3] = y;
        return message;
    }

    /**
     * Decode message and return tile information
     * @param message message to be decoded
     * @param messageTurn turn the passed message was posted to the blockChain
     * @return
     */
    public DecodedMessage decodeMessage(int[] message, int messageTurn){
        DecodedMessage decodedMessage = new DecodedMessage();
        int numTiles = 0;
        //this._encodedMessage = new EncodedMessage();

        //read message into message structure
        for (int i = 0; i < message.length - 1; i++){
            //ensure that header is skipped here
            //ensure that message is not blank (noted by -1 value)
            if (i != 2 && message[i] != -1) {
                this._encodedMessage.createBitset(i);
                this._encodedMessage.addInt(message[i], 32);
                decodedMessage.addTile(this._encodedMessage.getTile(i));
                numTiles++;
            }
        }
        decodedMessage.setLastMessageTurn(this._encodedMessage.decodeLastMessageTurn(message));
        return decodedMessage;
    }

    /**
     * Get the int representation of the encoded data to be sent
     * @return int array containing encoded information
     */
    public int[] getEncodedMessage(){
        int[] sendMessage = new int[7];
        for (int i = 0; i < 7; i++){
            if (this._encodedMessage != null){
                sendMessage[i] = this._encodedMessage.getInt(i);
            }else{
                sendMessage[i] = -1;
            }
        }
        return sendMessage;
    }

    /**
     * Calculates the header information for message
     * @param turnNumber current turn number
     * @return header information
     */
    private int calculateTeamId(int turnNumber){
        return ((turnNumber * 2) + 50);
    }

    private boolean verifyTeamId(int teamId, int messageTurn){
        int calcuationResult;
        calcuationResult = (teamId - 50) / 2;

        if (calcuationResult == messageTurn){
            return true;
        }else{
            return false;
        }
    }
}