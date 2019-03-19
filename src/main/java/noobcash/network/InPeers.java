package noobcash.network;

import noobcash.communication.Message;
import noobcash.threads.ServerThread;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import static noobcash.utilities.ErrorUtilities.fatal;

public class InPeers {
    private static final Logger LOGGER = Logger.getLogger("NOOBCASH");
    private BlockingQueue<Message> queue;

    public InPeers(PeerInfo[] peerInfos, BlockingQueue<Message> queue) {
        this.queue = queue;
        for (int i = 1; i < peerInfos.length; i++) {
            ServerThread thread = new ServerThread(peerInfos[i].server_socket, queue);
            thread.setDaemon(true);
            thread.start();
        }
    }

    public InPeers(int size, int port, BlockingQueue<Message> queue) {
        this.queue = queue;
        MasterServerThread mst = new MasterServerThread(size, port);
        mst.setDaemon(true);
        mst.start();
    }

    @SuppressWarnings("Duplicates")
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
            ServerSocket server;
            try {
                server = new ServerSocket(port);
            } catch (IOException e) {
                fatal("Can't open server socket");
                return;
            }
            while(i < size) {
                Socket socket;
                try {
                    socket = server.accept();
                } catch (IOException e) {
                    fatal("Can't accept connection");
                    return;
                }
                ServerThread thread = new ServerThread(socket, queue);
                thread.setDaemon(true);
                thread.start();
                i++;
            }
        }
    }
}
