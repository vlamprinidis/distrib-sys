package mains;

import network.InPeers;
import network.OutPeers;
import org.apache.commons.cli.*;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import beans.*;
import network.*;
import threads.CliThread;


public class NoobCash {
    private static final int BS_PORT = 5000;
    private static InetAddress BS_ADDR;

    @SuppressWarnings("Duplicates")
    public static void main(String[] args) {
        Options options = new Options();
        Option num_opt = new Option("n", "number", true, "number of peers");
        num_opt.setRequired(true);
        options.addOption(num_opt);

        Option port_opt = new Option("p", "port", true, "port to use");
        options.addOption(port_opt);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("utility-name", options);
            System.exit(1);
        }

        String n = cmd.getOptionValue("number");
        String p = cmd.getOptionValue("port");

        try {
            BS_ADDR = InetAddress.getByName("127.0.0.1");
        } catch (UnknownHostException e) {
            e.printStackTrace();
            System.exit(1);
        }

        int myId = -1; // initialize to invalid value
        PeerInfo[] peers;
        final int node_num = Integer.parseInt(n);
        BlockingQueue<Message> inQueue = new LinkedBlockingDeque<>();
        InPeers inPeers;
        OutPeers outPeers;
        int my_port = (p == null) ? BS_PORT : Integer.parseInt(p);
        CliThread cliThread = new CliThread(my_port + 1, inQueue);
        cliThread.start();

        if (p == null) {
            myId = 0;
            peers = new PeerInfo[node_num];
            peers[0] = null;
            try {
                ServerSocket server = new ServerSocket(BS_PORT);
                peers[0] = new PeerInfo(BS_ADDR, BS_PORT, null);
                int i = 1;
                System.out.println("Server started, accept'ing");
                while (i < node_num) {
                    Socket socket = server.accept();
                    InetAddress address = socket.getInetAddress();
                    ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                    Message msg = (Message) ois.readObject();
                    if (msg.messageType == MessageType.PeerPort) {
                        int port = (Integer) msg.data;
                        peers[i] = new PeerInfo(address, port, socket);
                        i++;
                        System.out.println("Peer connected : port " + port);
                    } else {
                        assert false;
                    }
                }
                for(int j = 1; j < node_num; j++){
                    ObjectOutputStream oos = new ObjectOutputStream(peers[j].server_socket.getOutputStream());
                    oos.writeObject(new Message(MessageType.PeerID, j));
                    oos.writeObject(new Message(MessageType.PeerInfo, peers));
                }
                System.out.println("Info's sent, start client connections");
                outPeers = new OutPeers(peers);
                inPeers = new InPeers(peers, inQueue);
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            }
        } else {
            try {
                System.out.println("Starting listen threads");
                inPeers = new InPeers(node_num - 1, my_port, inQueue);
                System.out.println("Connecting to bs");
                Socket socket = new Socket(BS_ADDR, BS_PORT);
                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                oos.writeObject(new Message(MessageType.PeerPort, my_port));
                ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                Message msg = (Message) ois.readObject();
                assert msg.messageType == MessageType.PeerID;
                myId = (Integer) msg.data;
                System.out.println("Got my ID : " + myId);
                msg = (Message) ois.readObject();
                assert msg.messageType == MessageType.PeerInfo;
                peers = (PeerInfo[]) msg.data;
                System.out.println("Got peer info, starting client connections");
                outPeers = new OutPeers(peers, socket, myId);
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            }

        }
        System.out.println("Start polling queue");

        Message msg;
        while(true) {
            try {
                msg = inQueue.take();
                System.out.println("Got msg type : " + msg.messageType + " data = " + msg.data);
                if (msg.messageType == MessageType.IdRequest) {
                   cliThread.sendMessage(new Message(MessageType.PeerID, myId));
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
