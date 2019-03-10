package threads;

import beans.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;

public class CliThread extends Thread {
    private int port;
    private BlockingQueue<Message> queue;
    Socket socket;

    public CliThread(int port, BlockingQueue<Message> queue) {
        this.port = port;
        this.queue = queue;
    }

    @Override
    public void run() {
        ObjectInputStream ois;
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            Socket socket = serverSocket.accept();
            this.socket = socket;
            serverSocket.close();
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

    public void sendMessage(Message msg) {
        try {
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            oos.writeObject(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
