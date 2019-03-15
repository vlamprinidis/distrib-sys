package network;


import java.net.Socket;
import java.util.HashMap;

import beans.Message;
import threads.ClientThread;


public class OutPeers {
    private HashMap<Integer, ClientThread> threads = new HashMap<>();

    public OutPeers(PeerInfo[] peerInfos) {
        for (int i = 1; i < peerInfos.length; i++) {
            ClientThread thread = new ClientThread(peerInfos[i].address, peerInfos[i].port);
            thread.setDaemon(true);
            thread.start();
            threads.put(i, thread);
        }
    }

    public OutPeers(PeerInfo[] peerInfos, Socket socket, int myId) {
        ClientThread thread = new ClientThread(socket);
        thread.setDaemon(true);
        thread.start();
        threads.put(0, thread);
        for (int i = 1; i < peerInfos.length; i++) {
            if (i == myId) continue;
            thread = new ClientThread(peerInfos[i].address, peerInfos[i].port);
            thread.start();
            threads.put(i, thread);
        }
    }

    public void broadcast(Message msg) {
        threads.values().forEach(t -> t.sendMessage(msg));
    }

    public void send(int id, Message msg) {
        threads.get(id).sendMessage(msg);
    }
}

