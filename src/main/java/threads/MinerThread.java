package threads;

import beans.Block;
import beans.Message;
import beans.MessageType;
import entities.Transaction;

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

    public MinerThread(BlockingQueue<Message> queue) {
        this.outQueue = queue;
        this.inQueue = new LinkedBlockingQueue<>();
    }

    @Override
    public void run() {

        Message msg;
        int x;
        while(true) {
            try {
                msg = inQueue.take();
            } catch (InterruptedException e) {
                LOGGER.warning("Miner got interrupted while take'ing block to mine");
                continue;
            }
            x = ((Integer) msg.data);
            LOGGER.finer("Miner got block to mine : " + x);
            int i = 0;
            while (!isInterrupted()){
                try {
                    LOGGER.info("Miner processing : " + x);
                    Thread.sleep(2000);
                    i++;
                } catch (InterruptedException e) {
                    LOGGER.info("Miner interrupted");
                    break;
                }
                if (x + i == 3) {
                    try {
                        outQueue.put(new Message(MessageType.BlockMined, 42));
                    } catch (InterruptedException e) {
                        LOGGER.warning("Miner interrupted while put'ing mined block");
                    }
                    break;
                }
            }
            LOGGER.finer("Miner waiting new block to mine");
        }
    }

    public void mineBlock(int x) {
        try {
            inQueue.put(new Message(null, x));
        } catch (InterruptedException e) {
            LOGGER.severe("Unexpectedly interrupted while put'ing block to be mined");
        }
    }
}
