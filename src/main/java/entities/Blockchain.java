package entities;

import beans.Block;

import java.io.Serializable;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;

public class Blockchain implements Serializable {

    private static ArrayList<Block> myChain = new ArrayList<>();
    private static Wallet myWallet;
    //private static int difficulty;
    private static int maxTransactionInBlock;
    private static HashMap<String,TransactionOutput> UTXOs = new HashMap<>();

    public boolean isValid() throws Exception {
        return true;
    }

    public Blockchain(){
        myWallet = new Wallet();
    }

    public Transaction create_transaction(PublicKey receiver_publicKey, int receiverAmount){
        int myBalance = wallet_balance(myWallet.getPublicKey());
        if( myBalance < receiverAmount) return null;

        ArrayList<String> myPreviousIds = getIds(UTXOs, myWallet.getPublicKey());
        if(myPreviousIds == null) return null;

        ArrayList<TransactionInput> transactionInputs = createTransactionInputs(myPreviousIds);
        if(transactionInputs == null) return null;

        if(!removeByIds(UTXOs, myPreviousIds)) return null;

        // create the transaction. It generates the outputs and is signed.
        Transaction current_transaction = new Transaction(
                myWallet.getPublicKey(), myWallet.getPrivateKey(), transactionInputs, myBalance,
                receiver_publicKey, receiverAmount);

        placeOutputs(UTXOs, current_transaction);

        return current_transaction;
    }

    //Verify the transaction's signature and check if its inputs are UTXOs and remove accordingly
    public boolean validate_transaction(Transaction transaction){
        try {
            if (!transaction.verifySignature()) return false;
            //Check if every transaction input is in my UTXOs
            for (TransactionInput tr : transaction.getTransaction_inputs()) {
                String key = tr.getPreviousOutputId();
                if (!UTXOs.containsKey(key)) return false;
            }
            //Remove all above ids from UTXOs
            ArrayList<String> senderPreviousIds = getIds(UTXOs, transaction.getSender_address());
            if (!removeByIds(UTXOs, senderPreviousIds)) throw new Exception();
            //Now put the proper ones
            placeOutputs(UTXOs, transaction);
            return true;
        } catch (Exception e){
            e.printStackTrace();
            System.out.println("Something went terribly wrong");
            return false;
        }

    }

    public int wallet_balance(PublicKey publicKey){
        int sum = 0;
        for (HashMap.Entry<String, TransactionOutput> entry : UTXOs.entrySet()) {
            TransactionOutput tr = entry.getValue();
            if(publicKey.equals(tr.getRecipient())){
                sum += tr.getAmount();
            }
        }
        return sum;
    }

    private void placeOutputs(HashMap<String,TransactionOutput> UTXOs, Transaction transaction) {
        TransactionOutput sender_out = transaction.getSender_out();
        if(sender_out != null){
            UTXOs.put(sender_out.getId(), sender_out);
        }

        TransactionOutput receiver_out = transaction.getReceiver_out();
        UTXOs.put(receiver_out.getId(), receiver_out);
    }

    //Give ids of the UTXOs that belong to given public key
    private ArrayList<String> getIds(HashMap<String,TransactionOutput> UTXOs, PublicKey publicKey){
        try {
            ArrayList<String> usedOutputIds = new ArrayList<>();
            for (HashMap.Entry<String, TransactionOutput> entry : UTXOs.entrySet()) {
                TransactionOutput tr = entry.getValue();
                if (publicKey.equals(tr.getRecipient())) {
                    usedOutputIds.add(entry.getKey());
                }
            }
            if(usedOutputIds.isEmpty()) return null;
            return usedOutputIds;
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    //Create transaction input list given the list of ids
    private ArrayList<TransactionInput> createTransactionInputs(ArrayList<String> previousOutputIds){
        try {
            ArrayList<TransactionInput> transactionInputs = new ArrayList<>();
            for (String id : previousOutputIds) {
                transactionInputs.add(new TransactionInput(id));
            }
            return transactionInputs;
        }
        catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }

    //Remove all UTXOs with the ids in the given list
    private boolean removeByIds(HashMap<String,TransactionOutput> UTXOs, ArrayList<String> previousOutputIds){
        try {
            for (String id : previousOutputIds) {
                UTXOs.remove(id);
            }
            return true;
        }
        catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }
}
