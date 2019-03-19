package noobcash.entities;

import java.io.Serializable;
import java.security.PublicKey;
import java.util.UUID;

public class TransactionOutput implements Serializable {

    private String parentTransactionId;
    private PublicKey recipient;
    private int amount;
    private UUID id;

    TransactionOutput(String parentTransactionId, PublicKey recipient, int amount){
        this.parentTransactionId = parentTransactionId;
        this.recipient = recipient;
        this.amount = amount;
        this.id = UUID.randomUUID();
    }

    boolean belongsTo(PublicKey publicKey) {
        return recipient.equals(publicKey);
    }

    UUID getId() {
        return id;
    }

    String getParentTransactionId() {
        return parentTransactionId;
    }

    int getAmount() {
        return amount;
    }

}
