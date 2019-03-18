package entities;

import java.io.Serializable;
import java.util.UUID;

class TransactionInput implements Serializable {
    private UUID previousOutputId;

    TransactionInput(TransactionOutput transactionOutput){
        this.previousOutputId = transactionOutput.getId();
    }

    UUID getPreviousOutputId() {
        return previousOutputId;
    }
}
