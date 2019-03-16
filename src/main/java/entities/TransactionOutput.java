package entities;

import utilities.StringUtilities;

import java.io.Serializable;
import java.security.PublicKey;

public class TransactionOutput implements Serializable {

    private String parentTransactionId;
    private PublicKey recipient;
    private int amount;
    private String id;

    public TransactionOutput(String parentTransactionId, PublicKey recipient, int amount){
        this.parentTransactionId = parentTransactionId;
        this.recipient = recipient;
        this.amount = amount;
        this.id = getHash();
    }

    private String getStringData(){
        String data;
        data = parentTransactionId + recipient + amount;
        return data;
    }

    private String getHash() {
        return StringUtilities.applySha256(getStringData());
    }

    public boolean belongsTo(PublicKey publicKey) {
        return recipient == publicKey;
    }


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
