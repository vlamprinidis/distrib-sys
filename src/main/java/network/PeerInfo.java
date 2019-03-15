package network;


import java.io.Serializable;
import java.net.InetAddress;
import java.net.Socket;

public class PeerInfo implements Serializable {
    InetAddress address;
    int port;
    public transient Socket server_socket;

    public PeerInfo(InetAddress address, int port, Socket server_socket){
        this.address = address;
        this.port = port;
        this.server_socket = server_socket;
    }

}
