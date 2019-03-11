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
import java.util.logging.*;

import beans.*;
import network.*;
import threads.CliThread;


public class NoobCash {
    private static final Logger LOGGER = Logger.getLogger("NOOBCASH");
    private static final int BS_PORT = 5000;

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
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("utility-name", options);
            return;
        }

        String n = cmd.getOptionValue("number");
        String p = cmd.getOptionValue("port");


        LOGGER.setUseParentHandlers(false);
        LOGGER.setLevel(Level.ALL);

        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(new SimpleFormatter());
        consoleHandler.setLevel(Level.INFO);
        LOGGER.addHandler(consoleHandler);

        FileHandler fileHandler;
        try {
            fileHandler = new FileHandler("noobcash.%u.%g.log");
            fileHandler.setFormatter(new SimpleFormatter());
            fileHandler.setLevel(Level.ALL);
            LOGGER.addHandler(fileHandler);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, e.toString(), e);
        }

        final InetAddress BS_ADDR;
        try {
            BS_ADDR = InetAddress.getByName("127.0.0.1");
        } catch (UnknownHostException e) {
            LOGGER.severe("Couldn't find BS address");
            return;
        }

        int myId;
        PeerInfo[] peers;
        final int node_num = Integer.parseInt(n);
        BlockingQueue<Message> inQueue = new LinkedBlockingDeque<>();
        // InPeers inPeers;
        OutPeers outPeers;
        int my_port = (p == null) ? BS_PORT : Integer.parseInt(p);
        CliThread cliThread = new CliThread(my_port + 1, inQueue);
        cliThread.setDaemon(true);
        cliThread.start();

        if (p == null) {
            myId = 0;
            peers = new PeerInfo[node_num];
            peers[0] = null;
            ServerSocket server;
            try {
                server = new ServerSocket(BS_PORT);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, e.toString(), e);
                return;
            }
            peers[0] = new PeerInfo(BS_ADDR, BS_PORT, null);
            int i = 1;
            while (i < node_num) {
                Socket socket;
                try {
                    socket = server.accept();
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, e.toString(), e);
                    return;
                }
                InetAddress address = socket.getInetAddress();
                ObjectInputStream ois;
                Message msg;
                try {
                    ois = new ObjectInputStream(socket.getInputStream());
                    msg = (Message) ois.readObject();
                } catch (IOException | ClassNotFoundException e) {
                    LOGGER.log(Level.SEVERE, e.toString(), e);
                    return;
                }
                if (msg.messageType == MessageType.PeerPort) {
                    int port = (Integer) msg.data;
                    peers[i] = new PeerInfo(address, port, socket);
                    i++;
                    LOGGER.info("BS : peer connected, port = " + port);
                } else {
                    LOGGER.warning("BS : Got unexpected message type : " + msg.messageType);
                }
            }
            LOGGER.info("All peers have joined");
            for(int j = 1; j < node_num; j++){
                ObjectOutputStream oos;
                try {
                    oos = new ObjectOutputStream(peers[j].server_socket.getOutputStream());
                    oos.writeObject(new Message(MessageType.PeerID, j));
                    oos.writeObject(new Message(MessageType.PeerInfo, peers));
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, e.toString(), e);
                    return;
                }
            }
            LOGGER.info("PeerInfo's have been sent");
            outPeers = new OutPeers(peers);
            new InPeers(peers, inQueue);

        } else {

            new InPeers(node_num - 1, my_port, inQueue);
            Socket socket;
            ObjectOutputStream oos;
            ObjectInputStream ois;
            Message msg;
            try {
                socket = new Socket(BS_ADDR, BS_PORT);
                oos = new ObjectOutputStream(socket.getOutputStream());
                oos.writeObject(new Message(MessageType.PeerPort, my_port));
                ois = new ObjectInputStream(socket.getInputStream());
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, e.toString(), e);
                return;
            }
            try {
                msg = (Message) ois.readObject();
                myId = (Integer) msg.data;
                if (msg.messageType != MessageType.PeerID) {
                    LOGGER.severe("Instead of PeerID, got : " + msg.messageType);
                    return;
                }

                msg = (Message) ois.readObject();
                if (msg.messageType != MessageType.PeerInfo) {
                    LOGGER.severe("Instead of PeerInfo, got : " + msg.messageType);
                    return;
                }
                peers = (PeerInfo[]) msg.data;
                LOGGER.info("Got PeerInfo's");
            } catch (IOException | ClassNotFoundException e) {
                LOGGER.log(Level.SEVERE, e.toString(), e);
                return;
            }
            outPeers = new OutPeers(peers, socket, myId);

        }
        LOGGER.info("Start main loop");

        Message msg;
        while(true) {
            try {
                msg = inQueue.take();
            } catch (InterruptedException e) {
                LOGGER.log(Level.SEVERE, "Unexpectedly interrupted while processing : [{0}] ", e);
                continue;
            }
            LOGGER.log( Level.FINER, "processing[{0}]: {1}", new Object[]{ msg.messageType,  msg.data });
                switch (msg.messageType) {
                    case PeerID:
                        break;
                    case PeerPort:
                        break;
                    case PeerInfo:
                        break;
                    case IdRequest:
                        LOGGER.info("CLI requests id");
                        cliThread.sendMessage(new Message(MessageType.PeerID, myId));
                        break;
                    case Ping:
                        outPeers.broadcast(new Message(MessageType.Pong, msg.data));
                        LOGGER.finer("Got ping");
                        break;
                    case Pong:
                        LOGGER.finer("Got pong");
                        break;
                    default:
                        LOGGER.warning("Unexpected message : " + msg.messageType);
                }
        }
    }

}
