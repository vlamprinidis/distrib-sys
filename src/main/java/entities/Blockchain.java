package entities;

import beans.Block;

import java.io.Serializable;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.ListIterator;

public class Blockchain implements Serializable {

    private static ArrayList<Block> myChain;
    private static Wallet myWallet;
    //private static int difficulty;
    private static int maxTransactionInBlock;
    private static HashMap<String,TransactionOutput> UTXOs;
    private ArrayList<Transaction> tsxPool;

    public boolean isValid() throws Exception {
        return true;
    }

    //todo: need to initialize UTXOs
    public Blockchain(int max, Block genesisBlock){
        maxTransactionInBlock = max;
        myWallet = new Wallet();
        tsxPool = new ArrayList<>();
        myChain = new ArrayList<>();
        myChain.add(genesisBlock);
        UTXOs = new HashMap<>();
    }

    //Creates a new block and adds given transactions to it, provided they are as much as a block needs
    public Block addToNewBlock(ArrayList<Transaction> transactions){
        if(transactions.size() < maxTransactionInBlock) return null;
        Block previousBlock = myChain.get(myChain.size()-1);
        Block block = new Block(transactions, previousBlock.getHash(), previousBlock.getIndex()+1);
        return block;
    }

    //Adds current block to chain, provided it is validated
    public void addToChain(Block block){
        myChain.add(block);
    }

    //todo: validate_block in block
    //todo: must validate every transaction as well, but validate_transaction here alters UTXOs, be careful
    public boolean validate_block(HashMap<String,TransactionOutput> UTXOs, Block block){
        if(!block.verify_hash()) return false;
        Block previousBlock = myChain.get(myChain.size()-1);
        return previousBlock.getHash().equals(block.getPrevious_hash());
    }

    public boolean validate_chain(HashMap<String,TransactionOutput> UTXOs, ArrayList<Block> chain){
        try {
            ListIterator<Block> it = chain.listIterator();
            //Ignore genesis block
            it.next();
            Block previousBlock, currentBlock;
            do{
                previousBlock = it.previous();
                it.next();
                currentBlock = it.next();
                if(!currentBlock.verify_hash()) return false;
                if(!previousBlock.getHash().equals(currentBlock.getPrevious_hash())) return false;
            }while (it.hasNext());

            return true;
        } catch(Exception e){
            e.printStackTrace();
            System.out.println("Something went wrong");
            return  false;
        }
    }

    public Transaction create_transaction(HashMap<String,TransactionOutput> UTXOs,
                                          PublicKey receiver_publicKey, int receiverAmount){
        int myBalance = wallet_balance(UTXOs, myWallet.getPublicKey());
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
    public boolean validate_transaction(HashMap<String,TransactionOutput> UTXOs, Transaction transaction){
        try {
            if (!transaction.verifySignature()) return false;
            ArrayList<String> senderPreviousIds = new ArrayList<>();

            //Check if every transaction input is in my UTXOs and save their previousOutputIds
            for (TransactionInput tr : transaction.getTransaction_inputs()) {
                String key = tr.getPreviousOutputId();
                if (!UTXOs.containsKey(key)) return false;
                senderPreviousIds.add(key);
            }

            //Remove all above ids from UTXOs
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

    public int wallet_balance(HashMap<String,TransactionOutput> UTXOs, PublicKey publicKey){
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

    //Getters here

    public PublicKey giveMyPublicKey(){
        return myWallet.getPublicKey();
    }

    public static int getMaxTransactionInBlock() {
        return maxTransactionInBlock;
    }

    public static HashMap<String, TransactionOutput> getUTXOs() {
        return UTXOs;
    }
}
