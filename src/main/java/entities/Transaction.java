package entities;

import utilities.StringUtilities;

import java.io.Serializable;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class Transaction implements Serializable {
    public PublicKey sender_address;
    public PublicKey receiver_address;
    public double amount;
    public String transaction_id;
    public List<TransactionInput> transaction_inputs;
    public List<TransactionOutput> transaction_outputs;
    private PrivateKey signature;

    public static Logger LOGGER = Logger.getLogger(Transaction.class.getName());

    public Transaction(PublicKey send, PublicKey receive, double value, List<TransactionInput> in, List<TransactionOutput> out){
        sender_address = send;
        receiver_address = receive;
        this.amount = value;
        transaction_inputs = new ArrayList<>(in);
        transaction_outputs = new ArrayList<>(out);

        transaction_id = calculateHash();


    }


    // This Calculates the transaction hash (which will be used as its Id)
    private String calculateHash() {
        String data="";
        for(TransactionInput tr: transaction_inputs ){
            data += tr.previousOutputId;
        }
        for(TransactionOutput tr: transaction_outputs ){
            data += tr.id;
            data += tr.parentTransactionId;
        }
        data += sender_address;
        data += receiver_address;
        data += amount;
        return StringUtilities.applySha256(data);
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

