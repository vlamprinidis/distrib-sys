package noobcash.communication;

import noobcash.entities.Block;
import noobcash.entities.Transaction;
import noobcash.entities.TransactionOutput;
import noobcash.network.PeerInfo;

import java.io.Serializable;
import java.util.LinkedList;

public class JoinResponseData implements Serializable {
    public int id;
    public PeerInfo[] peerInfo;
    public LinkedList<Block> chain;
    public LinkedList<Transaction> tsxPool;
    public TransactionOutput genesisUTXO;

    public JoinResponseData(int id, PeerInfo[] peerInfo, LinkedList<Block> chain,
                            LinkedList<Transaction> tsxPool, TransactionOutput genesisUTXO) {
        this.id = id;
        this.peerInfo = peerInfo;
        this.chain = chain;
        this.tsxPool = tsxPool;
        this.genesisUTXO = genesisUTXO;
    }
}
