package noobcash.entities;

import noobcash.utilities.StringUtilities;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

public class Block implements Serializable {

    private int index;
    private long timestamp;
    private List<Transaction> transactions;
    private String previousHash;
    private int nonce;
    private String currentHash;

    Block(int index, List<Transaction> transactions, String previousHash){
        this.index = index;
        this.timestamp = new Date().getTime();
        this.transactions = transactions;
        this.previousHash = previousHash;
        // Dummy initialization for genesis block
        nonce = 0;
    }

    private String getStringData(){
        StringBuilder data = new StringBuilder();
        data.append(index);
        data.append(timestamp);
        for(Transaction tr: transactions){
            data.append(tr.getTxid());
        }
        data.append(previousHash);
        data.append(nonce);

        return data.toString();
    }

    private String calculateHash() {
        return StringUtilities.applySha256(getStringData());
    }

    void hash() {
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
    private boolean verifyNonce(int difficulty) {
        return currentHash.substring(0, difficulty).equals(new String(new char[difficulty]).replace('\0', '0'));
    }

    private boolean verifyHash(){
        return calculateHash().equals(currentHash);
    }

    boolean verifyStructure(int blockSize, int difficulty) {
        return transactions.size() == blockSize && verifyNonce(difficulty) && verifyHash();
    }

    /*
     * Verify and apply every transaction modifying given UTXOs accordingly
     * Return false if invalid transaction found
     */
    boolean verifyApplyTransactions(UTXOs utxOs) {
        for (Transaction transaction : transactions) {
            if (!transaction.verify(utxOs)) return false;
            transaction.apply(utxOs);
        }
        return true;
    }

    public int getIndex() {
        return index;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public int getNonce() {
        return nonce;
    }

    String getCurrentHash() {
        return currentHash;
    }

    String getPreviousHash() {
        return previousHash;
    }
}
