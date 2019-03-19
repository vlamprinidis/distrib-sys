package noobcash.threads;

import noobcash.entities.Block;
import noobcash.communication.Message;
import noobcash.communication.MessageType;
import noobcash.entities.Blockchain;

import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

/*
 * Class that represents a miner.
 */
public class MinerThread extends Thread{

    private static final Logger LOGGER = Logger.getLogger("NOOBCASH");
    private BlockingQueue<Message> inQueue;
    private BlockingQueue<Message> outQueue;
    private boolean mineInProgress;
    // Main thread's view about whether or not mining is in progress
    private int difficulty;

    public MinerThread(BlockingQueue<Message> queue, int difficulty) {
        this.outQueue = queue;
        inQueue = new LinkedBlockingQueue<>();
        this.difficulty = difficulty;
        mineInProgress = false;
    }

    @SuppressWarnings("InfiniteLoopStatement")
    @Override
    public void run() {

        Message msg;
        Block block;
        Random randomStream = new Random();
        while(true) {
            try {
                msg = inQueue.take();
            } catch (InterruptedException e) {
                LOGGER.info("Interrupted while take'ing block from queue");
                continue;
            }
            if (msg.messageType != MessageType.BlockToMine) {
                LOGGER.severe("Instead of BlocKToMine, got : " + msg.messageType);
                continue;
            }
            block = ((Block) msg.data);

            LOGGER.info("Mining block " + block.getIndex());

            while (!isInterrupted()){
                if (block.tryMine(randomStream.nextInt(), difficulty)) {
                    LOGGER.info("Mined block " + block.getIndex());
                    try {
                        outQueue.put(new Message(MessageType.BlockMined, block));
                    } catch (InterruptedException e) {
                        interrupt();
                    }
                    break;
                }
            }
            if (interrupted()) LOGGER.info("Interrupted while mining");
        }
    }

    /*
     * Interrupt miner, aborting possible mining operation
     * Called when new block is found or chain is replaced
     */
    public void stopMining() {
        mineInProgress = false;
        interrupt();
        // Don't trust flag, interrupt thread anyway (no harm done)
    }

    /*
     * Send a block to miner, if necessary
     */
    public void maybeMine(Blockchain blockchain){
        if (mineInProgress || !blockchain.isFull()) return;
        try {
            mineInProgress = true;
            inQueue.put(new Message(MessageType.BlockToMine, blockchain.createBlock()));
        } catch (InterruptedException e) {
            LOGGER.severe("Interrupted while put'ing block to miner queue");
        }
    }

    /*
     * Called from main thread when
     * BlockMined message is seen
     */
    public void blockMinedAck() {
        mineInProgress = false;
    }
}
