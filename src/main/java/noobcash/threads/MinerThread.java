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
    private int lastBlockIndex;
    // Last block requested to mine
    private int difficulty;

    public MinerThread(BlockingQueue<Message> queue, int difficulty) {
        this.outQueue = queue;
        inQueue = new LinkedBlockingQueue<>();
        this.difficulty = difficulty;
        lastBlockIndex = 0;
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
                LOGGER.severe("Interrupted while take'ing block from queue");
                continue;
            }
            if (msg.messageType == MessageType.Stop) {
                LOGGER.info("Miner stopped, wasn't mining anyway");
                continue;
            }
            if (msg.messageType != MessageType.BlockToMine) {
                LOGGER.severe("Instead of BlocKToMine, got : " + msg.messageType);
                continue;
            }
            block = ((Block) msg.data);

            LOGGER.info("Mining block " + block.getIndex());

            while(true) {
                Message newMessage;

                // New message -> mining will stop
                if ((newMessage = inQueue.peek()) != null) {
                    if (newMessage.messageType == MessageType.Stop) {
                        LOGGER.info("Miner stopped");
                        // If stop message, consume it
                        inQueue.poll();
                    }
                    break;
                }

                if (block.tryMine(randomStream.nextInt(), difficulty)) {
                    LOGGER.info("Mined block " + block.getIndex());
                    try {
                        outQueue.put(new Message(MessageType.BlockMined, block));
                    } catch (InterruptedException e) {
                        LOGGER.severe("Interrupted while put'ing mined block");
                    }
                    break;
                }
            }
        }
    }

    /*
     * Inform miner to stop mining current block
     */
    public void stopMining() {
        try {
            inQueue.put(new Message(MessageType.Stop, null));
        } catch (InterruptedException e) {
            LOGGER.severe("Interrupted while STOP-ing miner");
        }
    }

    /*
     * Send a block to miner, if necessary
     */
    public void maybeMine(Blockchain blockchain){
        if (blockchain.getLastBlock().getIndex()  >= lastBlockIndex && blockchain.isFull()) {
            try {
                Block block = blockchain.createBlock();
                // Invariant : index' > index
                lastBlockIndex = block.getIndex();
                inQueue.put(new Message(MessageType.BlockToMine, blockchain.createBlock()));
            } catch (InterruptedException e) {
                LOGGER.severe("Interrupted while put'ing block to miner queue");
            }
        }
    }
}
