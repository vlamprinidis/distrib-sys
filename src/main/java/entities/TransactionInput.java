package entities;

import java.io.Serializable;

public class TransactionInput implements Serializable {
    private String previousOutputId; //Reference to TransactionOutputs -> transactionId

    public TransactionInput(String previousOutputId){
        this.previousOutputId = previousOutputId;
    }

    public String getPreviousOutputId() {
        return previousOutputId;
    }
}
