package beans;

import entities.Transaction;
import network.PeerInfo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;

public class JoinAnswerData implements Serializable {
    public int id;
    public PeerInfo[] peerInfo;
    public ArrayList<Block> chain;
    public LinkedList<Transaction> tsxPool;

    public JoinAnswerData(int id, PeerInfo[] peerInfo, ArrayList<Block> chain, LinkedList<Transaction> tsxPool) {
        this.id = id;
        this.peerInfo = peerInfo;
        this.chain = chain;
        this.tsxPool = tsxPool;
    }
}