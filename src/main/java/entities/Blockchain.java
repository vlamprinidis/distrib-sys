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
    private UTXOs confirmedUTXOs;
    private UTXOs unconfirmedUTXOs;
    private TransactionOutput genesisUTXO;
    private static int difficulty;
    private static int blockSize;

    public Blockchain(Wallet wallet, int blockSize, int difficulty) {
        chain = new ArrayList<>();
        this.wallet = wallet;
        confirmedUTXOs = new UTXOs();
        unconfirmedUTXOs = new UTXOs();
        tsxPool = new LinkedList<>();
        Blockchain.difficulty = difficulty;
        Blockchain.blockSize = blockSize;
    }

    public Block generateGenesis(int networkSize){
        PublicKey publicKey = wallet.getPublicKey();

        TransactionOutput genesisUTXO = new TransactionOutput(null, publicKey, 100* networkSize);
        this.genesisUTXO = genesisUTXO;
        confirmedUTXOs.add(genesisUTXO);
        unconfirmedUTXOs = new UTXOs(confirmedUTXOs);

        Transaction genesisTSX = createTransaction(publicKey, 100* networkSize);
        // yes, send money to myself, from myself

        if (genesisTSX == null) {
            LOGGER.severe("Not enough money for genesis tsx !?");
            System.exit(1);
        }
        if(!genesisTSX.verify(unconfirmedUTXOs)) {
            LOGGER.severe("Couldn't verify genesis tsx !?");
            System.exit(1);
        }
        genesisTSX.apply(unconfirmedUTXOs);
        // tsx hasn't been confirmed yet

        Block genesisBlock = new Block(0, new ArrayList<>(Collections.singletonList(genesisTSX)), "1");
        genesisBlock.setNonce(0);
        genesisBlock.hash();

        return genesisBlock;
    }

    /*
     * Validate given chain
     * If valid replace current, rebuild UTXO, modify pool
     * If invalid, return false and don't change anything
     */
    public boolean replaceChain(ArrayList<Block> newChain) {
        UTXOs newConfirmedUTXOs = new UTXOs();
        newConfirmedUTXOs.add(genesisUTXO);
        // start with just genesis UTXO

        ArrayList<Block> myNewChain = new ArrayList<>();
        // try to add every newChain's block to myNewChain
        for (Block block : newChain) {
            if (!addBlock(block, myNewChain, newConfirmedUTXOs)) {
                LOGGER.info("Replace chain : couldn't add block");
                return false;
            }
        }

        // Everything is validated, changes will be committed
        confirmedUTXOs = newConfirmedUTXOs;
        chain = myNewChain;

        // Remove from pool all transactions already in new chain
        chain.forEach(b -> removeFromPool(tsxPool, b));

        // Try to apply remaining pool transactions, dropping those that now seem invalid
        unconfirmedUTXOs = buildUnconfirmedUTXOs(confirmedUTXOs, tsxPool);

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
     * Remove block's transactions from pool
     */
    private static void removeFromPool(LinkedList<Transaction> tsxPool, Block block) {
        block.getTransactions().forEach(tsxPool::remove);
    }

    /*
     * Builds unconfirmed UTXOs (= confirmed.apply(tsxPool)), dropping now invalid pool transactions
     */
    private static UTXOs buildUnconfirmedUTXOs (UTXOs confirmedUTXOs, LinkedList<Transaction> tsxPool) {

        UTXOs unconfirmedUTXOs = new UTXOs(confirmedUTXOs);
        for(ListIterator<Transaction> it = tsxPool.listIterator(); it.hasNext(); ) {
            Transaction t = it.next();
            if (!t.verify(unconfirmedUTXOs)) {
                it.remove();
                LOGGER.warning("BuildUnconfirmedUTXOs : dropped transaction");
            } else {
                t.apply(unconfirmedUTXOs);
            }
        }
        return unconfirmedUTXOs;
    }

    /*
     * Add a block to a chain
     * Validate block (order + structure)
     * Validate and apply block's transactions using given confirmed UTXOs
     * Return false if something is invalid
     * Modifies given UTXOs either way
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean addBlock(Block block, ArrayList<Block> chain, UTXOs confirmedUTXOs){
        // Check order and structure
        if (chain.isEmpty()) {
            if (!block.isGenesis()) {
                LOGGER.warning("Add block : non-genesis block to empty chain !?");
                return false;
            }
        } else {
            if(!isExpectedNext(chain, block)) {
                LOGGER.info("Add block : not expected next");
                return false;
            }
            if (!block.verifyStructure(blockSize, difficulty)) {
                LOGGER.warning("Add block : bad structure");
                return false;
            }
        }

        // Verify and apply, changes confirmed UTXO even if it fails
        if (!block.verifyApplyTransactions(confirmedUTXOs)) {
            return false;
        }

        // Finally add block to chain
        chain.add(block);
        return true;
    }

    /*
     * Try to add a new block to current chain
     * If invalid return false and don't change anything
     * If valid, add it, remove block's transactions from pool and
     * re-apply, if possible, remaining pool transactions to unconfirmed UTXOs
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean addBlock(Block block) {
        UTXOs newConfirmedUTXOs = new UTXOs(confirmedUTXOs);
        // Copy to avoid custom rollback

        if(!addBlock(block, chain, newConfirmedUTXOs)) {
            return false;
        }
        // Block is A-OK, verified and put in chain, commit UTXOs
        confirmedUTXOs = newConfirmedUTXOs;
        removeFromPool(tsxPool, block);
        unconfirmedUTXOs = buildUnconfirmedUTXOs(confirmedUTXOs, tsxPool);
        return true;
    }

    private static boolean isExpectedNext(ArrayList<Block> chain, Block block) {
        Block lastBlock = chain.get(chain.size() - 1);
        return (block.getIndex() == lastBlock.getIndex() + 1) && (block.getPreviousHash().equals(lastBlock.getCurrentHash()));
    }

    /*
     * Apply a transaction to the unconfirmed UTXOs and add it to pool
     * Return false if it is invalid without modifying UTXOs
     */
    public boolean verifyApplyTransaction(Transaction transaction) {
        if (!transaction.verify(unconfirmedUTXOs)) return false;
        transaction.apply(unconfirmedUTXOs);
        tsxPool.add(transaction);
        return true;
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
     * Uses unconfirmed UTXOs
     * Returns null if not enough coins
     */
    public Transaction createTransaction(PublicKey recipient, int amount){
        PublicKey sender = wallet.getPublicKey();
        ArrayList<TransactionInput> inputs = new ArrayList<>();
        int sum = 0;
        for(TransactionOutput output : unconfirmedUTXOs.values()){
            if(output.belongsTo(sender)) {
                sum += output.getAmount();
                inputs.add(new TransactionInput(output));
            }
            if (sum >= amount) break;
        }
        if (sum >= amount) {
            Transaction transaction = new Transaction(sender, recipient, inputs, amount, sum - amount);
            transaction.sign(wallet.getPrivateKey());
            return transaction;
        }

        return null;
    }

    /*
     * Get balance, using unconfirmed UTXOs
     */
    public int getBalance(PublicKey publicKey){
        int sum = 0;
        for (TransactionOutput output : unconfirmedUTXOs.values()){
            if (output.belongsTo(publicKey)){
                sum += output.getAmount();
            }
        }
        return sum;
    }

    public Block getLastBlock() {
        return chain.get(chain.size() - 1);
    }

    public TransactionOutput getGenesisUTXO() {
        return genesisUTXO;
    }

    public void setGenesisUTXO(TransactionOutput genesisUTXO) {
        this.genesisUTXO = genesisUTXO;
    }

    public ArrayList<Block> getChain() {
        return chain;
    }

    public UTXOs getConfirmedUTXOs() {
        return confirmedUTXOs;
    }

    public UTXOs getUnconfirmedUTXOs() {
        return unconfirmedUTXOs;
    }

    public int getDifficulty() {
        return difficulty;
    }

    public LinkedList<Transaction> getTsxPool() {
        return tsxPool;
    }

}
