package entities;

import java.io.Serializable;

class TransactionInput implements Serializable {
    private String previousOutputId;

    TransactionInput(TransactionOutput transactionOutput){
        this.previousOutputId = transactionOutput.getId();
    }

    String getPreviousOutputId() {
        return previousOutputId;
    }
}
