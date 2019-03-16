package beans;

import entities.Transaction;
import entities.TransactionOutput;
import utilities.StringUtilities;

import java.io.Serializable;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class Block implements Serializable {

    private int index;
    private long timestamp;
    private List<Transaction> transactions;
    private String previousHash;
    private int nonce;
    private String currentHash;

    public Block(int index, List<Transaction> transactions, String previousHash){
        this.index = index;
        this.timestamp = new Date().getTime();
        this.transactions = transactions;
        this.previousHash = previousHash;
    }

    private String getStringData(){
        String data = "";
        data += index;
        data += timestamp;
        for(Transaction tr: transactions){
            data += tr.getTxid();
        }
        data += previousHash;
        data += nonce;

        return data;
    }

    private String calculateHash() {
        return StringUtilities.applySha256(getStringData());
    }

    public void hash() {
        this.currentHash = calculateHash();
    }

    public boolean tryMine(int nonce, int difficulty) {
        this.nonce = nonce;
        this.currentHash = calculateHash();
        return verifyNonce(difficulty);
    }

    public boolean isGenesis() {
        return this.index == 0 && "1".equals(this.previousHash) && this.nonce == 0 && this.transactions.size() == 1 && verifyHash();
    }

    /*
     * Check if correct PoW
     */
    public boolean verifyNonce(int difficulty) {
        return currentHash.substring(0, difficulty).equals(new String(new char[difficulty]).replace('\0', '0'));
    }

    public boolean verifyHash(){
        return calculateHash().equals(currentHash);
    }

    public void setNonce(int nonce) {
        this.nonce = nonce;
    }

    public boolean verifyStructure(int index, int blockSize, String previousHash, int difficulty) {
        return this.index == index && transactions.size() == blockSize && this.previousHash.equals(previousHash) && verifyNonce(difficulty) && verifyHash();
    }

    /*
     * Verify and apply every transaction modifying given UTXOs accordingly
     * Return false if invalid transaction found
     */
    public boolean verifyApplyTransactions(HashMap<String, TransactionOutput> UTXOs) {
        for (Transaction transaction : transactions) {
            if (!transaction.verify(UTXOs)) return false;
            transaction.apply(UTXOs);
        }
        return true;
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

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public int getNonce() {
        return nonce;
    }

    public String getCurrentHash() {
        return currentHash;
    }

    public String getPreviousHash() {
        return previousHash;
    }
}
