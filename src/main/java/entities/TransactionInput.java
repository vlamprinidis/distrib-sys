package entities;

import java.io.Serializable;

public class TransactionInput implements Serializable {
    public String previousOutputId; //Reference to TransactionOutputs -> transactionId
    //public TransactionOutput UTXO; //Contains the Unspent transaction output

    public TransactionInput(String previousOutputId){
        this.previousOutputId = previousOutputId;
    }



}
