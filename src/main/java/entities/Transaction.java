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
    private String signature="";

    public static Logger LOGGER = Logger.getLogger(Transaction.class.getName());

    //public Transaction(PublicKey send, PublicKey receive, double value, List<TransactionInput> in, List<TransactionOutput> out){
    public Transaction(Wallet wallet_send, Wallet wallet_receive, double value, List<TransactionInput> in, List<TransactionOutput> out){
        sender_address = wallet_send.publicKey;
        receiver_address = wallet_receive.publicKey;
        this.amount = value;
        transaction_inputs = in;
        transaction_outputs = out;

        transaction_id = calculateHash(giveData());
    }

    private String giveData(){
        String data="";
        if(transaction_inputs != null) {
            for (TransactionInput tr : transaction_inputs) {
                data += tr.previousOutputId;
            }
        }
        if(transaction_outputs != null) {
            for (TransactionOutput tr : transaction_outputs) {
                data += tr.id;
                data += tr.parentTransactionId;
                data += tr.amount;
                data += tr.recipient;
            }
        }
        data += sender_address;
        data += receiver_address;
        data += amount;
        return data;
    }


    // This Calculates the transaction hash (which will be used as its Id)
    private String calculateHash(String data) {
        return StringUtilities.applySha256(data);
    }

    //Signs all the data we dont wish to be tampered with.

    //public void generateSignature(PrivateKey privateKey) {
    public void generateSignature(Wallet wallet_send) {
       this.signature = StringUtilities.sign(giveData(), wallet_send.privateKey);
    }

    //Verifies the data we signed hasnt been tampered with
    public boolean verifySignature() {
        return StringUtilities.verify(giveData(), this.signature, sender_address);
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

