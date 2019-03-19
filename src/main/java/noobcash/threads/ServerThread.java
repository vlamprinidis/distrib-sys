package noobcash.threads;

import noobcash.communication.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Logger;

import static noobcash.utilities.ErrorUtilities.fatal;

public class ServerThread extends Thread {

    private static final Logger LOGGER = Logger.getLogger("NOOBCASH");
    private Socket socket;
    private BlockingQueue<Message> queue;

    public ServerThread(Socket socket, BlockingQueue<Message> queue) {
        this.socket = socket;
        this.queue = queue;
    }

    @SuppressWarnings("InfiniteLoopStatement")
    @Override
    public void run() {
        ObjectInputStream ois;
        try {
            ois = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            fatal("Can't create input stream");
            return;
        }

        while(true) {
            try {
                queue.put((Message) ois.readObject());
            } catch (InterruptedException e) {
                LOGGER.severe("Interrupted while put'ing message");
            } catch (ClassNotFoundException e) {
                fatal(e.toString());
            } catch (IOException e) {
                fatal("Can't read object from peer stream");
            }
            // LOGGER.finest("Forwarded peer message to main thread");
        }

    }
}
