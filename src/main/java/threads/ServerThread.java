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
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        while(true) try {
            queue.put((Message) ois.readObject());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
