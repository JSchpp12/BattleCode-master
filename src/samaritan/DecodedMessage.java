package com.company;

import samaritan.Tile;

public class DecodedMessage {
    private int _teamId, _messageTurn, _lastMessageTurn, _numTiles;
    private Tile[] _tiles;

    public DecodedMessage(){
        this._tiles = new Tile[6];
        this._numTiles = 0;
    }

    public void addTile(Tile inTile){
        this._tiles[this._numTiles] = inTile;
        this._numTiles++;
    }

    public Tile getTile(int index){
        return this._tiles[index];
    }


    public int getNumTiles(){return this._numTiles; }
    public int getMessageTurn(){return this._messageTurn; }
    public int getTeamId(){return this._teamId; }
    public int getLastMessageTurn(){return this._lastMessageTurn;}

    public void setTeamId(int teamId){this._teamId = teamId; }
    public void setMessageTurn(int messageTurn){this._messageTurn = messageTurn; }
    public void setLastMessageTurn(int lastMessageTurn){this._lastMessageTurn = lastMessageTurn; }
}
