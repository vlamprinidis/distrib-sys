package beans;

import entities.Transaction;
import utilities.StringUtilities;

import java.io.Serializable;
import java.util.ArrayList;

import java.util.Date;

public class Block implements Serializable {

    private int index;
    private long timestamp;
    private ArrayList<Transaction> transactions;
    private int nonce;
    private String hash;
    private String previous_hash;

    public Block(ArrayList<Transaction> transactions, String previous_hash, int index){
        this.nonce = 0;
        this.index = index;
        this.transactions = transactions;
        this.previous_hash = previous_hash;
        this.timestamp = new Date().getTime();
        this.hash = this.calculateHash(giveData());
    }

    private String giveData(){
        String data = "";
        for(Transaction tr: transactions){
            data += tr.getTransaction_id();
        }
        data = previous_hash +
                timestamp +
                nonce +
                index +
                data;

        return data;
    }

    private String calculateHash(String data) {
        return StringUtilities.applySha256(data);
    }

    public boolean verify_hash(){
        return calculateHash(giveData()).equals(hash);
    }

    /*public void mineBlock(int difficulty) {
        String target = new String(new char[difficulty]).replace('\0', '0'); //Create a string with difficulty * "0"
        while(!this.current_hash.substring( 0, difficulty).equals(target)) {
            this.nonce ++;
            this.current_hash = calculateHash();
        }
        System.out.println("Block Mined!!! : " + this.current_hash);
    }*/

    //Getters here

    public int getIndex() {
        return index;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public ArrayList<Transaction> getTransactions() {
        return transactions;
    }

    public int getNonce() {
        return nonce;
    }

    public String getHash() {
        return hash;
    }

    public String getPrevious_hash() {
        return previous_hash;
    }
}
