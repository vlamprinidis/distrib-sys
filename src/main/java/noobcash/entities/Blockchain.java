package noobcash.entities;

import java.io.Serializable;
import java.security.PublicKey;
import java.util.*;
import java.util.logging.Logger;

import static noobcash.utilities.ErrorUtilities.fatal;


public class Blockchain implements Serializable {

    private static final Logger LOGGER = Logger.getLogger("NOOBCASH");
    private LinkedList<Block> chain;
    private Wallet wallet;
    private LinkedList<Transaction> tsxPool;
    private UTXOs confirmedUTXOs;
    private UTXOs unconfirmedUTXOs;
    private TransactionOutput genesisUTXO;
    private static int difficulty;
    private static int blockSize;

    public Blockchain(Wallet wallet, int blockSize, int difficulty) {
        chain = new LinkedList<>();
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

        // Yes, send money to myself, from myself
        Transaction genesisTSX = createTransaction(publicKey, 100* networkSize);

        if (genesisTSX == null) fatal("Can't create genesis transaction");
        if (!verifyApplyTransaction(genesisTSX)) fatal("Can't verify genesis transaction");

        Block genesisBlock = new Block(0, new ArrayList<>(Collections.singletonList(genesisTSX)), "1");
        genesisBlock.hash();

        return genesisBlock;
    }

    /*
     * Validate given chain
     * If valid replace current, rebuild UTXOs's, modify pool
     * If invalid, return false and don't change anything
     */
    public boolean replaceChain(LinkedList<Block> newChain) {
        // Start with just genesis UTXO

        UTXOs newConfirmedUTXOs = new UTXOs();
        newConfirmedUTXOs.add(genesisUTXO);

        // Chain that might replace mine
        // Copy not necessary but helps to reuse existing functions
        LinkedList<Block> myNewChain = new LinkedList<>();
        for (Block block : newChain) {
            if (!addBlock(block, myNewChain, newConfirmedUTXOs)) {
                LOGGER.fine("Abort replace chain");
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
     * Must be called only if isFull
     */
    public Block createBlock(){
        List<Transaction> transactions = new ArrayList<>(tsxPool.subList(0, blockSize));
        Block previousBlock = chain.getLast();
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
                LOGGER.info("Transaction dropped");
            } else {
                t.apply(unconfirmedUTXOs);
            }
        }
        return unconfirmedUTXOs;
    }

    /*
     * Check if a block has valid structure but index bigger that expected
     * If true we might need to ask about possible fork
     */
    public boolean possibleLongerFork(Block block) {
        if (!block.verifyStructure(blockSize, difficulty)) {
            LOGGER.warning("Bad block structure");
            return false;
        }
        return block.getIndex() > (getLastBlock().getIndex() + 1);
    }

    /*
     * Add a block to a chain
     * Validate block (order + structure)
     * Validate and apply block's transactions using given confirmed UTXOs
     * Return false if something is invalid
     * Modifies given UTXOs either way
     * Doesn't modify unconfirmed UTXOs and tsxPool in any way
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean addBlock(Block block, LinkedList<Block> chain, UTXOs confirmedUTXOs){
        // Check order and structure
        if (chain.isEmpty()) {
            if (!block.isGenesis()) {
                LOGGER.warning("Trying to add non-genesis block to empty chain");
                return false;
            }
        } else {
            if(!isExpectedNext(chain, block)) {
                LOGGER.finer("Block not next expected");
                return false;
            }
            if (!block.verifyStructure(blockSize, difficulty)) {
                LOGGER.warning("Bad block structure");
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

    private static boolean isExpectedNext(LinkedList<Block> chain, Block block) {
        Block lastBlock = chain.getLast();
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
        return chain.getLast();
    }

    public TransactionOutput getGenesisUTXO() {
        return genesisUTXO;
    }

    public void setGenesisUTXO(TransactionOutput genesisUTXO) {
        this.genesisUTXO = genesisUTXO;
    }

    public LinkedList<Block> getChain() {
        return chain;
    }

    public UTXOs getConfirmedUTXOs() {
        return confirmedUTXOs;
    }

    public UTXOs getUnconfirmedUTXOs() {
        return unconfirmedUTXOs;
    }

    public LinkedList<Transaction> getTsxPool() {
        return tsxPool;
    }

}
