package entities;

import utilities.StringUtilities;

import java.io.Serializable;
import java.security.PublicKey;

public class TransactionOutput implements Serializable {

    private String parentTransactionId;
    private PublicKey recipient;
    private int amount;
    private String id;

    TransactionOutput(String parentTransactionId, PublicKey recipient, int amount){
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

    boolean belongsTo(PublicKey publicKey) {
        return recipient.equals(publicKey);
    }


    String getId() {
        return id;
    }

    String getParentTransactionId() {
        return parentTransactionId;
    }

    int getAmount() {
        return amount;
    }

    public PublicKey getRecipient() {
        return recipient;
    }
}
