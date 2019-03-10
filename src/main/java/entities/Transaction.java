package entities;

import utilities.StringUtilities;

import java.io.Serializable;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Iterator;

public class Transaction implements Serializable {
    public PublicKey sender_address;
    public PublicKey receiver_address;
    public double amount;
    public int transaction_id;
    public List<TransactionInput> transaction_inputs;
    public List<TransactionOutput> transaction_outputs;
    private PrivateKey signature;

    public static Logger LOGGER = Logger.getLogger(Transaction.class.getName());


    // This Calculates the transaction hash (which will be used as its Id)
    private String calulateHash() {

        return "";
    }

    //Signs all the data we dont wish to be tampered with.

    public void generateSignature(PrivateKey privateKey) {
    }

    //Verifies the data we signed hasnt been tampered with
    public boolean verifiySignature() {
        return true;
    }

    //Returns true if new transaction could be created.
    // It also checks the validity of a transaction
    public boolean processTransaction(Blockchain blockchain) {
        return true;
    }

    //returns sum of inputs(UTXOs) values
    public float getInputsValue() {
        return 0f;
    }

    //returns sum of outputs:
    public float getOutputsValue() {
        return 0f;
    }


}

