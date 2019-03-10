package beans;

import entities.Blockchain;
import entities.Transaction;
import utilities.StringUtilities;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import java.util.Date;

/**
 * Block class represents the basic part of the blockchain
 *
 * Implements the Serializable inteface in order to be sent above
 * network when a new miner joins the blockchain network
 */
public class Block implements Serializable {

    private int index;
    private long timestamp;
    private List<Transaction> transactions;
    private int nonce;
    public String current_hash;
    private String previous_hash;

    public Block(List<Transaction> transactions, String previous_hash, int previous_index){
    //public Block(String data, String previous_hash, int previous_index){
        this.nonce = 0;
        this.index = previous_index + 1;
        this.transactions = new ArrayList<>(transactions);
        this.previous_hash = previous_hash;
        this.timestamp = new Date().getTime();
        this.current_hash = this.calculateHash();

    }

    /*
     * todo:
     * Function that calculates the hash on the current block
     */
    public String calculateHash() {
        String data = "";
        for(Transaction tr: transactions){
            data += tr.transaction_id;
        }
        return StringUtilities.applySha256(
                previous_hash +
                        timestamp +
                        nonce +
                        index +
                        data
        );
    }

    public void mineBlock(int difficulty) {
        String target = new String(new char[difficulty]).replace('\0', '0'); //Create a string with difficulty * "0"
        while(!this.current_hash.substring( 0, difficulty).equals(target)) {
            this.nonce ++;
            this.current_hash = calculateHash();
        }
        System.out.println("Block Mined!!! : " + this.current_hash);
    }

    /*
     * todo:
     * Function that adds a Transaction on the current block if it is valid
     */
    public boolean addTransaction(Transaction transaction, Blockchain blockchain) {

        return true;
    }

}
