package entities;

import beans.Block;

import java.io.Serializable;
import java.security.PublicKey;
import java.util.*;
import java.util.logging.Logger;

public class Blockchain implements Serializable {

    private static final Logger LOGGER = Logger.getLogger("NOOBCASH");
    private ArrayList<Block> chain;
    private Wallet wallet;
    private LinkedList<Transaction> tsxPool;
    private HashMap<String,TransactionOutput> UTXOs;
    private static int difficulty;
    private static int blockSize;

    public Blockchain(Wallet wallet, int blockSize, int difficulty) {
        chain = new ArrayList<>();
        this.wallet = wallet;
        UTXOs = new HashMap<>();
        tsxPool = new LinkedList<>();
        Blockchain.difficulty = difficulty;
        Blockchain.blockSize = blockSize;
    }

    public Block generateGenesis(int networkSize){
        PublicKey publicKey = wallet.getPublicKey();

        TransactionOutput genesisUTXO = new TransactionOutput(null, publicKey, 100* networkSize);
        UTXOs.put(genesisUTXO.getId(), genesisUTXO);

        Transaction genesisTSX = createTransaction(publicKey, 100* networkSize);
        // yes, send money to myself, from myself

        if (genesisTSX == null) {
            LOGGER.severe("Not enough money for genesis tsx !?");
            System.exit(1);
        }
        if(!genesisTSX.verify(UTXOs)) {
            LOGGER.severe("Couldn't verify genesis tsx !?");
            System.exit(1);
        }
        genesisTSX.apply(UTXOs);

        Block genesisBlock = new Block(0, new ArrayList<>(Collections.singletonList(genesisTSX)), "1");
        genesisBlock.setNonce(0);
        genesisBlock.hash();

        return genesisBlock;
    }

    /*
     * Given a chain, validate it
     * If valid replace current, rebuild UTXO, modify pool
     * If invalid, return false and don't change anything
     */
    public boolean replaceChain(ArrayList<Block> newChain) {
        HashMap<String, TransactionOutput> newUTXOs = new HashMap<>();
        // start with empty UTXO

        ListIterator<Block> blockIt = newChain.listIterator();
        Block block = blockIt.next(); // genesis block
        if (!(block.isGenesis())){
            LOGGER.warning("Got an chain with invalid genesis block");
            return false;
        }
        TransactionOutput genesisOutput = block.getTransactions().get(0).getOutputs().get(0);
        newUTXOs.put(genesisOutput.getId(), genesisOutput);
        // put genesis UTXO

        int index = 0;
        while(blockIt.hasNext()) {
            String previousHash = block.getCurrentHash();
            block = blockIt.next();
            if (!block.verifyStructure(++index, blockSize, previousHash, difficulty)) {
                LOGGER.warning("Got a chain with invalid block structure");
                return false;
            }
            if (!block.verifyApplyTransactions(newUTXOs)) {
                LOGGER.info("Got a chain with invalid block transactions");
                return false;
            }
        }

        // Everything is validated, changes will be committed

        // Remove from pool all transactions already in new chain
        tsxPool.removeIf(t -> hasTransaction(newChain, t));

        // Try to apply remaining pool transactions, dropping those that now seem invalid
        // Is this the correct behavior ?
        for(ListIterator<Transaction> transactionIt = tsxPool.listIterator(); transactionIt.hasNext();) {
            Transaction transaction = transactionIt.next();
            if (!transaction.verify(newUTXOs)) {
                LOGGER.warning("Dropping tsx from pool while replacing chain");
                transactionIt.remove();
            } else {
                transaction.apply(newUTXOs);
            }
        }

        // Replace chain and UTXOs
        chain = newChain;
        UTXOs = newUTXOs;

        return true;

    }

    /*
     * Create next block from current pool
     */
    public Block createBlock(){
        List<Transaction> transactions = new ArrayList<>(tsxPool.subList(0, blockSize));
        Block previousBlock = chain.get(chain.size() - 1);
        return new Block(previousBlock.getIndex() + 1, transactions, previousBlock.getCurrentHash());
    }

    /*
     * Add a processed block to current chain
     * All transactions are already verified and applied to UTXOs
     * Removes block transactions from pool
     */
    public void addBlock(Block block) {
        chain.add(block);
        block.getTransactions().forEach(t -> tsxPool.remove(t));
    }

    public static boolean hasTransaction(ArrayList<Block> chain, Transaction transaction) {
        for (Block block : chain) {
            for (Transaction t : block.getTransactions()) {
                if (t.getTxid().equals(transaction.getTxid())) return true;
            }
        }
        return false;
    }


    /*
     * Apply a transaction to the current UTXO and add it to pool
     * Return false if it is invalid
     */
    public boolean applyTransaction(Transaction transaction) {
        if (!transaction.verify(UTXOs)) return false;
        transaction.apply(UTXOs);
        tsxPool.add(transaction);
        return true;
    }

    /*
     * Apply a valid transaction to given UTXO
     */
    public static void applyTransaction(Transaction transaction, HashMap<String, TransactionOutput> UTXOs) {
        transaction.getInputs().forEach(t -> UTXOs.remove(t.getPreviousOutputId()));
        transaction.getOutputs().forEach(t -> UTXOs.put(t.getId(), t));
    }

    /*
     * Check if there are at least blockSize transactions in current pool
     */
    public boolean isFull() {
        return tsxPool.size() >= blockSize;
    }

    /*
     * Create a transaction to send amount coins to recipient
     * Doesn't modify UTXO or pool
     * Returns null if not enough coins
     */
    public Transaction createTransaction(PublicKey recipient, int amount){
        PublicKey sender = wallet.getPublicKey();
        ArrayList<TransactionInput> inputs = new ArrayList<>();
        int sum = 0;
        for (HashMap.Entry<String, TransactionOutput> entry : UTXOs.entrySet()) {
            TransactionOutput tr = entry.getValue();
            if(tr.belongsTo(sender)) {
                sum += tr.getAmount();
                inputs.add(new TransactionInput(tr));
            }
            if (sum >= amount) break;
        }
        if (sum >= amount) {
            Transaction transaction = new Transaction(sender, recipient, inputs, amount, sum - amount);
            transaction.sign(wallet.getPrivateKey());
            return transaction;
        } else {
            return null;
        }

        /*
        int myBalance = getBalance(UTXOs, wallet.getPublicKey());
        if( myBalance < amount) return null;

        ArrayList<String> myPreviousIds = getIds(UTXOs, wallet.getPublicKey());
        if(myPreviousIds == null) return null;

        ArrayList<TransactionInput> transactionInputs = createTransactionInputs(myPreviousIds);
        if(transactionInputs == null) return null;

        if(!removeByIds(UTXOs, myPreviousIds)) return null;

        // create the transaction. It generates the outputs and is signed.
        Transaction current_transaction = new Transaction(
                wallet.getPublicKey(), wallet.getPrivateKey(), transactionInputs, myBalance,
                receiverPublicKey, amount);

        placeOutputs(UTXOs, current_transaction);

        return current_transaction;
        */
    }

    //Verify the transaction's signature and check if its inputs are UTXOs and remove accordingly
    public boolean validate_transaction(HashMap<String,TransactionOutput> UTXOs, Transaction transaction){
        try {
            if (!transaction.verifySignature()) return false;
            ArrayList<String> senderPreviousIds = new ArrayList<>();

            //Check if every transaction input is in my UTXOs and save their previousOutputIds
            for (TransactionInput tr : transaction.getInputs()) {
                String key = tr.getPreviousOutputId();
                if (!UTXOs.containsKey(key)) return false;
                senderPreviousIds.add(key);
            }

            //Remove all above ids from UTXOs
            //if (!removeByIds(UTXOs, senderPreviousIds)) throw new Exception();

            //Now put the proper ones
            //placeOutputs(UTXOs, transaction);
            return true;
        } catch (Exception e){
            e.printStackTrace();
            System.out.println("Something went terribly wrong");
            return false;
        }

    }

    public int getBalance(PublicKey publicKey){
        int sum = 0;
        for (HashMap.Entry<String, TransactionOutput> entry : UTXOs.entrySet()) {
            TransactionOutput tr = entry.getValue();
            if (tr.belongsTo(publicKey)){
                sum += tr.getAmount();
            }
        }
        return sum;
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

    //Getters here

    public ArrayList<Block> getChain() {
        return chain;
    }


    public PublicKey giveMyPublicKey(){
        return wallet.getPublicKey();
    }

    public int getBlockSize() {
        return blockSize;
    }

    public HashMap<String, TransactionOutput> getUTXOs() {
        return UTXOs;
    }

    public int getDifficulty() {
        return difficulty;
    }

    public LinkedList<Transaction> getTsxPool() {
        return tsxPool;
    }


    public void setTsxPool(LinkedList<Transaction> tsxPool) {
        this.tsxPool = tsxPool;
    }


    public void setChain(ArrayList<Block> chain) {
        this.chain = chain;
    }


}
