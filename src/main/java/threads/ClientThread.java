package threads;

import beans.Message;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientThread extends Thread {

    private static final Logger LOGGER = Logger.getLogger("NOOBCASH");
    private Socket socket = null;
    private InetAddress address;
    private int port;
    private BlockingQueue<Message> queue = new LinkedBlockingQueue<>();

    public ClientThread(Socket socket) {
        this.socket = socket;
    }

    public ClientThread(InetAddress address, int port) {
        this.address = address;
        this.port = port;
    }

    @Override
    public void run() {
        if (socket == null) {
            try {
                socket = new Socket(address, port);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, e.toString(), e);
                return;
            }
        }
        LOGGER.fine("Connected to peer with port : " + port);
        ObjectOutputStream oos;
        try {
            oos = new ObjectOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e.toString(), e);
            return;
        }
        Message msg;
        while (true) {
            try {
                msg = queue.take();
                oos.writeObject(msg);
            } catch (InterruptedException e) {
                LOGGER.log(Level.SEVERE, e.toString(), e);
                return;
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, e.toString(), e);
            }
            LOGGER.finer("Successfully sent message to CLI");
        }
    }

    public void sendMessage(Message msg) {
        try {
            queue.put(msg);
        } catch (InterruptedException e) {
            LOGGER.log(Level.SEVERE, e.toString(), e);
        }
    }
}
