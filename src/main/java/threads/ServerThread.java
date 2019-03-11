package threads;

import beans.Message;
import beans.MessageType;
import entities.NodeMiner;
import utilities.MessageUtilities;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerThread extends Thread {

    private static final Logger LOGGER = Logger.getLogger("NOOBCASH");
    private Socket socket;
    private BlockingQueue<Message> queue;

    public ServerThread(Socket socket, BlockingQueue<Message> queue) {
        this.socket = socket;
        this.queue = queue;
    }

    @Override
    public void run() {
        ObjectInputStream ois;
        try {
            ois = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e.toString(), e);
            return;
        }

        while(true) {
            try {
                queue.put((Message) ois.readObject());
            } catch (InterruptedException | IOException e) {
                LOGGER.log(Level.SEVERE, e.toString(), e);
                return;
            } catch (ClassNotFoundException e) {
                LOGGER.log(Level.WARNING, "Unexpectedly serverThread got non-Message object from queue : [{0}]", e);
                continue;
            }
            LOGGER.finest("Successfully got message from peer and put it into queue");
        }

    }
}
