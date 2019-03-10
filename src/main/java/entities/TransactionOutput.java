package entities;

import utilities.StringUtilities;

import java.io.Serializable;
import java.security.PublicKey;

public class TransactionOutput implements Serializable {

    public String id;
    public String parentTransactionId; //the id of the transaction this output was created in
    public PublicKey reciepient; //also known as the new owner of these coins.
    public double amount; //the amount of coins they own



    //Check if coin belongs to you
    public boolean isMine(PublicKey publicKey) {
        return true;
    }


}
