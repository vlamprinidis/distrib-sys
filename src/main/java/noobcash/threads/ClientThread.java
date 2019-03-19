package noobcash.threads;

import noobcash.communication.Message;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
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
                LOGGER.severe("Can't open client socket");
                return;
            }
        }
        ObjectOutputStream oos;
        try {
            oos = new ObjectOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            LOGGER.severe("Can't create output stream");
            return;
        }
        Message msg;
        while (true) {
            try {
                msg = queue.take();
                oos.writeObject(msg);
            } catch (InterruptedException e) {
                LOGGER.severe("Interrupted while take'ing message");
                continue;
            } catch (IOException e) {
                LOGGER.severe("Can't write to client socket");
                return;
            }
            // LOGGER.finest("Successfully sent message to cli");
        }
    }

    public void sendMessage(Message msg) {
        try {
            queue.put(msg);
        } catch (InterruptedException e) {
            LOGGER.severe("Interrupted while put'ing to clientThread queue");
        }
    }
}
