package threads;

import beans.Message;
import beans.MessageType;
import entities.NodeMiner;
import entities.Transaction;

import javax.sound.sampled.LineEvent;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientThread extends Thread {

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
                System.out.println("Trying to connect to " + address.toString() + port);
                socket = new Socket(address, port);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        ObjectOutputStream oos;
        try {
            oos = new ObjectOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        Message msg;
        while (true) try {
            msg = queue.take();
            oos.writeObject(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(Message msg) {
        try {
            queue.put(msg);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
