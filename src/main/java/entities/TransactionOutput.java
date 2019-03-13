package entities;

import utilities.StringUtilities;

import java.io.Serializable;
import java.security.PublicKey;

public class TransactionOutput implements Serializable {

    private String id;
    private String parentTransactionId; //the id of the transaction this output was created in
    private PublicKey recipient; //also known as the new owner of these coins.
    private int amount; //the amount of coins they will get

    public TransactionOutput(PublicKey recipient, int amount, String parentTransactionId){
        this.recipient = recipient;
        this.amount = amount;
        this.parentTransactionId = parentTransactionId;
        this.id = calculateHash(giveData());
    }

    private String giveData(){
        String data;
        data = parentTransactionId + recipient + amount;
        return data;
    }

    private String calculateHash(String data) {
        return StringUtilities.applySha256(data);
    }

    //Check if coin belongs to you
    public boolean isMine(PublicKey publicKey) {
        return publicKey == recipient;
    }

    //Getters here

    public String getId() {
        return id;
    }

    public String getParentTransactionId() {
        return parentTransactionId;
    }

    public PublicKey getRecipient() {
        return recipient;
    }

    public int getAmount() {
        return amount;
    }
}
