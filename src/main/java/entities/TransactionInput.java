package entities;

import java.io.Serializable;

public class TransactionInput implements Serializable {
    public String previousOutputId; //Reference to TransactionOutputs -> transactionId

    public TransactionInput(String previousOutputId){
        this.previousOutputId = previousOutputId;
    }
}
