package threads;

import beans.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CliThread extends Thread {
    private static final Logger LOGGER = Logger.getLogger("NOOBCASH");
    private int port;
    private BlockingQueue<Message> queue;
    private Socket socket;

    public CliThread(int port, BlockingQueue<Message> queue) {
        this.port = port;
        this.queue = queue;
    }

    @SuppressWarnings("Duplicates")
    @Override
    public void run() {
        ObjectInputStream ois;
        ServerSocket serverSocket;
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e.toString(), e);
            return;
        }
        try {
            Socket socket = serverSocket.accept();
            serverSocket.close();
            this.socket = socket;
            ois = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e.toString(), e);
            System.exit(1);
            return;
        }

        while(true) {
            try {
                queue.put((Message) ois.readObject());
            } catch (InterruptedException | IOException e) {
                LOGGER.log(Level.SEVERE, e.toString(), e);
                return;
            } catch (ClassNotFoundException e) {
                LOGGER.log(Level.WARNING, "Unexpectedly cliThread got non-Message object from queue : [{0}]", e);
                continue;
            }
            LOGGER.finest("Successfully got message from cli and put it into queue");
        }
    }

    public void sendMessage(Message msg) {
        try {
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            oos.writeObject(msg);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e.toString(), e);
        }
    }
}
