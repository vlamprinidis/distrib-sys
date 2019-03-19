package noobcash.threads;

import noobcash.communication.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Logger;

import static noobcash.utilities.ErrorUtilities.fatal;

public class CliThread extends Thread {
    private static final Logger LOGGER = Logger.getLogger("NOOBCASH");
    private int port;
    private BlockingQueue<Message> queue;
    private ObjectOutputStream oos;

    public CliThread(int port, BlockingQueue<Message> queue) {
        this.port = port;
        this.queue = queue;
    }

    @SuppressWarnings({"Duplicates", "InfiniteLoopStatement"})
    @Override
    public void run() {
        ObjectInputStream ois;
        ServerSocket serverSocket;
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            fatal("Can't open cli server socket");
            return;
        }
        try {
            Socket socket = serverSocket.accept();
            serverSocket.close();
            ois = new ObjectInputStream(socket.getInputStream());
            oos = new ObjectOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            fatal("Can't create cli streams");
            return;
        }

        while(true) {
            try {
                queue.put((Message) ois.readObject());
            } catch (InterruptedException e) {
                // LOGGER.warning("Interrupted while put'ing message");
            } catch (ClassNotFoundException e) {
                fatal(e.toString());
            } catch (IOException e) {
                fatal("Can't read object from cli stream");
            }
            // LOGGER.finest("Forwarded cli message to main thread");
        }
    }

    public void sendMessage(Message msg) {
        try {
            oos.writeObject(msg);
        } catch (IOException e) {
            LOGGER.severe("Can't write message to cli stream");
        }
    }
}
