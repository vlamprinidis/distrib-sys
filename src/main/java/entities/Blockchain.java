package entities;

import beans.Block;

import java.io.Serializable;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Blockchain will be part a node-miner. It should be able to be sent to
 * a new miner joining the network
 */
public class Blockchain implements Serializable {

    private static ArrayList<Block> myChain = new ArrayList<>();
    private static Wallet myWallet;
    //private static int difficulty;
    private static int maxTransactionInBlock;
    private static HashMap<String,TransactionOutput> UTXOs = new HashMap<>();


    /**
     * Method checking if the list of blocks contained in this object is
     * creates a valid myChain
     *
     * @return True, if the myChain is valid, else false
     */
    public boolean isValid() throws Exception {
        return true;
    }

    public Blockchain(){
        myWallet = new Wallet();
    }

    public Transaction create_transaction(PublicKey receiver_publicKey, int receiverAmount){
        int myBalance = wallet_balance();
        if( myBalance < receiverAmount) return null;

        ArrayList<TransactionInput> transactionInputs = popMyUTXOsAsInputs();

        // create the transaction. It generates the outputs and is signed.
        Transaction current_transaction = new Transaction(
                myWallet.getPublicKey(), myWallet.getPrivateKey(), transactionInputs, myBalance,
                receiver_publicKey, receiverAmount);

        TransactionOutput sender_out = current_transaction.getSender_out();
        if(sender_out != null) {
            UTXOs.put(sender_out.getId(), sender_out);
        }

        TransactionOutput receiver_out = current_transaction.getReceiver_out();
        UTXOs.put(receiver_out.getId(), receiver_out);

        return current_transaction;
    }

    public boolean isMe(PublicKey publicKey){
        return myWallet.getPublicKey().equals(publicKey);
    }

    public int wallet_balance() {
        int sum = 0;
        for (HashMap.Entry<String, TransactionOutput> entry : UTXOs.entrySet()) {
            TransactionOutput tr = entry.getValue();
            if(isMe(tr.getRecipient())){
                sum += tr.getAmount();
            }
        }
        return sum;
    }

    // This function alters UTXOs!
    private ArrayList<TransactionInput> popMyUTXOsAsInputs(){
        ArrayList<TransactionInput> transactionInputs = new ArrayList<>();
        ArrayList<String> usedOutputIds = new ArrayList<>();

        for (HashMap.Entry<String, TransactionOutput> entry : UTXOs.entrySet()) {
            TransactionOutput tr = entry.getValue();

            if(isMe(tr.getRecipient())){
                transactionInputs.add(new TransactionInput(tr.getId()));
                //save ids of the transaction outputs to be removed
                usedOutputIds.add(entry.getKey());
            }
        }

        for(String id : usedOutputIds){
            //remove all ids of my transaction outputs
            UTXOs.remove(id);
        }
        return transactionInputs;
    }
}
