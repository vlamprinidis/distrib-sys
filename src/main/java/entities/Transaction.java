package entities;

import utilities.StringUtilities;

import java.io.Serializable;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Date;

public class Transaction implements Serializable {
    private PublicKey sender_address;
    private PublicKey receiver_address;
    private int amount;
    private String transaction_id;
    private ArrayList<TransactionInput> transaction_inputs;
    private TransactionOutput sender_out=null;
    private TransactionOutput receiver_out;
    private String signature;
    private long timestamp;

    public Transaction(
            PublicKey sender_publicKey, PrivateKey sender_privateKey, ArrayList<TransactionInput> in, int senderBalance,
            PublicKey receiver_publicKey, int receiverAmount){

        sender_address = sender_publicKey;
        receiver_address = receiver_publicKey;
        this.amount = receiverAmount;
        transaction_inputs = in;
        timestamp = new Date().getTime();

        String data = giveData();

        transaction_id = calculateHash(data);

        int leftOver = senderBalance - receiverAmount;
        if(leftOver > 0) {
            sender_out = new TransactionOutput(sender_publicKey, leftOver, transaction_id);
        }
        receiver_out = new TransactionOutput(receiver_publicKey, receiverAmount, transaction_id);

        signature = generateSignature(data, sender_privateKey);
    }

    private String giveData(){
        String data="";
        if(transaction_inputs != null) {
            for (TransactionInput tr : transaction_inputs) {
                data += tr.previousOutputId;
            }
        }
        data += sender_address;
        data += receiver_address;
        data += amount;
        data += timestamp;
        return data;
    }

    // This Calculates the transaction hash (which will be used as its Id)
    private String calculateHash(String data) {
        return StringUtilities.applySha256(data);
    }

    //Signs all the data we dont wish to be tampered with.
    private String generateSignature(String data, PrivateKey privateKey) {
       return StringUtilities.sign(data, privateKey);
    }

    //Verifies the data we signed hasn't been tampered with
    public boolean verifySignature() {
        return StringUtilities.verify(giveData(), this.signature, sender_address);
    }

    //Getters here


    public PublicKey getSender_address() {
        return sender_address;
    }

    public PublicKey getReceiver_address() {
        return receiver_address;
    }

    public int getAmount() {
        return amount;
    }

    public String getTransaction_id() {
        return transaction_id;
    }

    public ArrayList<TransactionInput> getTransaction_inputs() {
        return transaction_inputs;
    }

    public TransactionOutput getSender_out() {
        return sender_out;
    }

    public TransactionOutput getReceiver_out() {
        return receiver_out;
    }

    public String getSignature() {
        return signature;
    }
}

