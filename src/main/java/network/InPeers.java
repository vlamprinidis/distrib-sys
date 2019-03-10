package network;

import beans.Message;
import threads.ServerThread;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;

public class InPeers {
    private BlockingQueue<Message> queue;

    public InPeers(PeerInfo[] peerInfos, BlockingQueue<Message> queue) {
        this.queue = queue;
        for (int i = 1; i < peerInfos.length; i++) {
            ServerThread thread = new ServerThread(peerInfos[i].server_socket, queue);
            thread.start();
        }
    }

    public InPeers(int size, int port, BlockingQueue<Message> queue) {
        this.queue = queue;
        MasterServerThread mst = new MasterServerThread(size, port);
        mst.start();
    }

    class MasterServerThread extends Thread {
        private int size;
        private int port;

        MasterServerThread(int size, int port) {
            this.size = size;
            this.port = port;
        }

        @Override
        public void run(){
            int i = 0;
            try {
                ServerSocket server = new ServerSocket(port);
                while(i < size) {
                    Socket socket = server.accept();
                    ServerThread thread = new ServerThread(socket, queue);
                    thread.start();
                    i++;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
