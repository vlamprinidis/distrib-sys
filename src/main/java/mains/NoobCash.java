package mains;

import entities.Blockchain;
import entities.Transaction;
import entities.Wallet;
import network.InPeers;
import network.OutPeers;
import org.apache.commons.cli.*;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.logging.*;

import beans.*;
import network.*;
import threads.CliThread;
import threads.MinerThread;


public class NoobCash {
    private static final Logger LOGGER = Logger.getLogger("NOOBCASH");
    private static final int BS_PORT = 5000;

    @SuppressWarnings("Duplicates")
    public static void main(String[] args) throws ClassNotFoundException {
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
        boolean isBootstrap = p == null;


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

        InetAddress BS_ADDR;
        try {
            BS_ADDR = InetAddress.getByName("127.0.0.1");
        } catch (UnknownHostException e) {
            LOGGER.severe("Couldn't find BS address");
            return;
        }

        int myPort = (isBootstrap) ? BS_PORT : Integer.parseInt(p);
        int myId;
        PeerInfo[] peers;
        final int networkSize = Integer.parseInt(n);
        BlockingQueue<Message> inQueue = new LinkedBlockingDeque<>();
        // InPeers inPeers;
        OutPeers outPeers;
        Wallet wallet;
        try {
            wallet = new Wallet();
        } catch (NoSuchAlgorithmException e) {
            LOGGER.severe("Couldn't generate key pair");
            return;
        }
        Blockchain blockchain = new Blockchain(wallet, 2, 3);

        CliThread cliThread = new CliThread(myPort + 1, inQueue);
        cliThread.setDaemon(true);
        cliThread.start();

        MinerThread minerThread = new MinerThread(inQueue);
        minerThread.setDaemon(true);
        minerThread.start();

        if (isBootstrap) {
            Block genesisBlock = blockchain.generateGenesis(networkSize);
            assert genesisBlock.isGenesis();
            blockchain.addBlock(genesisBlock);
            assert blockchain.getUTXOs().isEmpty();

            myId = 0;
            peers = new PeerInfo[networkSize];
            ServerSocket server;
            try {
                server = new ServerSocket(BS_PORT);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, e.toString(), e);
                return;
            }
            peers[0] = new PeerInfo(BS_ADDR, BS_PORT, null, wallet.getPublicKey());
            for (int i = 1; i < networkSize; i++){
                Socket socket;
                try {
                    socket = server.accept();
                } catch (IOException e) {
                    LOGGER.severe("Couldn't accept connection");
                    return;
                }
                InetAddress address = socket.getInetAddress();
                ObjectInputStream ois;
                Message msg;
                try {
                    ois = new ObjectInputStream(socket.getInputStream());
                    msg = (Message) ois.readObject();
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, e.toString(), e);
                    return;
                }
                if (msg.messageType == MessageType.PeerJoin) {
                    PeerJoinData data = (PeerJoinData) msg.data;
                    peers[i] = new PeerInfo(address, data.port, socket, data.publicKey);
                    LOGGER.info("BS : peer connected, port = " + data.port);
                } else {
                    LOGGER.warning("BS : Got unexpected message type : " + msg.messageType);
                }
                Transaction tsx = blockchain.createTransaction(peers[i].publicKey, 100);
                if (tsx == null) {
                    LOGGER.severe("Not enough money to send initial coins !?");
                    return;
                }
                if (!blockchain.applyTransaction(tsx)){
                    LOGGER.severe("Invalid initial transaction !?");
                    return;
                }
            }
            LOGGER.info("All peers have joined");
            int ind = 1;
            int difficulty = blockchain.getDifficulty();
            int blockSize = blockchain.getBlockSize();
            String prevHash = genesisBlock.getCurrentHash();
            while(blockchain.isFull()) {
                Block block = blockchain.createBlock();
                int nonce = 0;
                while(!block.tryMine(nonce, difficulty)) nonce++;
                if (!block.verifyStructure(ind++, blockSize, prevHash, difficulty)) {
                    LOGGER.severe("Couldn't verify my own chain :(");
                    return;
                }
                LOGGER.info("Nonce : " + block.getNonce());
                prevHash = block.getCurrentHash();
                blockchain.addBlock(block);
            }

            for(int j = 1; j < networkSize; j++){
                ObjectOutputStream oos;
                try {
                    oos = new ObjectOutputStream(peers[j].server_socket.getOutputStream());
                    JoinAnswerData data = new JoinAnswerData(j, peers, blockchain.getChain(), blockchain.getTsxPool());
                    oos.writeObject(new Message(MessageType.JoinAnswer, data));
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, e.toString(), e);
                    return;
                }
            }
            LOGGER.info("Initial data have been sent");
            outPeers = new OutPeers(peers);
            new InPeers(peers, inQueue);

        } else {

            new InPeers(networkSize - 1, myPort, inQueue);
            Socket socket;
            ObjectOutputStream oos;
            ObjectInputStream ois;
            Message msg;
            try {
                socket = new Socket(BS_ADDR, BS_PORT);
                oos = new ObjectOutputStream(socket.getOutputStream());
                oos.writeObject(new Message(MessageType.PeerJoin, new PeerJoinData(myPort, wallet.getPublicKey())));
                ois = new ObjectInputStream(socket.getInputStream());
            } catch (IOException e) {
                LOGGER.severe("Couldn't establish communication with bs");
                return;
            }
            try {
                msg = (Message) ois.readObject();
                if (msg.messageType != MessageType.JoinAnswer) {
                    LOGGER.severe("Instead of JoinAnswer, got : " + msg.messageType);
                    return;
                }
                JoinAnswerData data = (JoinAnswerData) msg.data;
                myId = data.id;
                peers = data.peerInfo;

                blockchain.setTsxPool(data.tsxPool);
                // manually set initial pending transactions
                if (!blockchain.replaceChain(data.chain)) {
                    LOGGER.severe("Bootstrap sent invalid chain ?!");
                    return;
                }
                LOGGER.info("Got initial data");

            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, e.toString(), e);
                return;
            }
            outPeers = new OutPeers(peers, socket, myId);
        }
        for (int i = 0; i < networkSize; i++) {
            System.out.println("Peer with id = " + i + " has " + blockchain.getBalance(peers[i].publicKey) + " coins");
        }

        LOGGER.info("Main loop started");

        minerThread.mineBlock(0);
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
                case IdRequest:
                    LOGGER.info("CLI requested id, sending : " + myId);
                    cliThread.sendMessage(new Message(MessageType.IdAnswer, myId));
                    break;
                case Ping:
                    outPeers.broadcast(new Message(MessageType.Pong, msg.data));
                    LOGGER.finer("Got ping");
                    break;
                case Pong:
                    LOGGER.finer("Got pong");
                    break;
                case BlockMined:
                    LOGGER.info("Mined new block!");
                    break;
                case Stop:
                    LOGGER.info("Bye bye");
                    return;
                default:
                    LOGGER.warning("Unexpected message : " + msg.messageType);
            }
        }
    }

}
