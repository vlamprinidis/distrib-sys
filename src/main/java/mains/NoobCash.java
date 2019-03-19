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
import java.util.ArrayList;
import java.util.Random;
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
        final int difficulty = 3;
        Blockchain blockchain = new Blockchain(wallet, 2, difficulty);

        CliThread cliThread = new CliThread(myPort + 1, inQueue);
        cliThread.setDaemon(true);
        cliThread.start();

        MinerThread minerThread = new MinerThread(inQueue, difficulty);
        minerThread.setDaemon(true);
        minerThread.start();

        if (isBootstrap) {
            Block genesisBlock = blockchain.generateGenesis(networkSize);
            assert genesisBlock.isGenesis();
            // block created, not confirmed := added to chain
            if (!blockchain.addBlock(genesisBlock)) {
                LOGGER.severe("Couldn't add genesis block to chain !?");
                return;
            }

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
                if (msg.messageType == MessageType.JoinRequest) {
                    JoinRequestData data = (JoinRequestData) msg.data;
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
                if (!blockchain.verifyApplyTransaction(tsx)){
                    LOGGER.severe("Invalid initial transaction !?");
                    return;
                }
            }
            LOGGER.info("All peers have joined");
            while(blockchain.isFull()) {
                Block block = blockchain.createBlock();
                Random randomStream = new Random();
                // Explicit to avoid warnings
                while(true) {
                    if (block.tryMine(randomStream.nextInt(), difficulty)) break;
                }
                LOGGER.info("Nonce : " + block.getNonce());
                if(!blockchain.addBlock(block)) {
                    LOGGER.severe("Couldn't add my own block to chain !?");
                    return;
                }
            }

            LOGGER.info("Confirmed UTXOs size : " + blockchain.getConfirmedUTXOs().size());
            LOGGER.info("Unconfirmed UTXOs size : " + blockchain.getUnconfirmedUTXOs().size());

            for(int j = 1; j < networkSize; j++){
                ObjectOutputStream oos;
                try {
                    oos = new ObjectOutputStream(peers[j].server_socket.getOutputStream());
                    JoinResponseData data = new JoinResponseData(j, peers, blockchain.getChain(),
                            blockchain.getTsxPool(), blockchain.getGenesisUTXO());
                    oos.writeObject(new Message(MessageType.JoinResponse, data));
                } catch (IOException e) {
                    LOGGER.severe("Couldn't send initial data to peer");
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
                oos.writeObject(new Message(MessageType.JoinRequest, new JoinRequestData(myPort, wallet.getPublicKey())));
                ois = new ObjectInputStream(socket.getInputStream());
            } catch (IOException e) {
                LOGGER.severe("Couldn't establish communication with bs");
                return;
            }
            try {
                msg = (Message) ois.readObject();
                if (msg.messageType != MessageType.JoinResponse) {
                    LOGGER.severe("Instead of JoinResponse, got : " + msg.messageType);
                    return;
                }
                JoinResponseData data = (JoinResponseData) msg.data;
                myId = data.id;
                peers = data.peerInfo;

                // Manually set genesisUTXO (= input of first TSX that gives BS coins)
                blockchain.setGenesisUTXO(data.genesisUTXO);

                if (!blockchain.replaceChain(data.chain)) {
                    LOGGER.severe("Bootstrap sent invalid chain ?!");
                    return;
                }

                // Pretend that you received initial unconfirmed transaction, adding each one
                for (Transaction t : data.tsxPool) {
                    if (!blockchain.verifyApplyTransaction(t)) {
                        LOGGER.severe("Couldn't initial unconfirmed transactions !?");
                        return;
                    }
                }
                LOGGER.info("Got and validated all initial data");

            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, e.toString(), e);
                return;
            }
            outPeers = new OutPeers(peers, socket, myId);
        }

        LOGGER.info("Main loop started");

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
                    LOGGER.finer("CLI requested id, sending : " + myId);
                    cliThread.sendMessage(new Message(MessageType.IdResponse, myId));
                    break;
                case BalanceRequest:
                    int x = msg.data == null ? myId : (Integer) msg.data;
                    if (x >= networkSize) {
                        LOGGER.warning("CLI request with invalid id (balance)");
                        break;
                    }
                    LOGGER.finer("CLI request balance of : " + x);
                    cliThread.sendMessage(new Message(MessageType.BalanceResponse,
                            blockchain.getBalance(peers[x].publicKey)));
                    break;
                case PeerInfoRequest:
                    Integer y = (Integer) msg.data;
                    if (y != null) {
                        if (y >= networkSize) {
                            LOGGER.warning("CLI request with invalid id (peer info)");
                            break;
                        }
                        LOGGER.finer("CLI request peer info of : " + y);
                        cliThread.sendMessage(new Message(MessageType.PeerInfoResponse,
                                peers[y]));
                    } else {
                        LOGGER.finer("CLI request peer infos");
                        cliThread.sendMessage(new Message(MessageType.PeerInfoResponse,
                                peers));
                    }
                    break;
                case CliTsxRequest:
                    CliTsxRequestData cliTsxRequestData = (CliTsxRequestData) msg.data;
                    if (cliTsxRequestData.id >= networkSize) {
                        LOGGER.warning("CLI request with invalid id (transaction)");
                        cliThread.sendMessage(new Message(MessageType.CliTsxResponse, "Invalid id"));
                        break;
                    }
                    String tsxStr = cliTsxRequestData.amount + " -> " + cliTsxRequestData.id;
                    LOGGER.info("Cli request : send " + tsxStr);
                    Transaction cliTsx = blockchain.createTransaction(peers[cliTsxRequestData.id].publicKey,
                            cliTsxRequestData.amount);
                    String responseString;
                    if (cliTsx == null) {
                        responseString = "Transaction rejected";
                    } else {
                        if (!blockchain.verifyApplyTransaction(cliTsx)) {
                            LOGGER.severe("Couldn't verify transaction I just made !?");
                            return;
                        }
                        responseString = "Transaction accepted";
                        outPeers.broadcast(new Message(MessageType.NewTransaction, cliTsx));
                        minerThread.maybeMine(blockchain);
                    }
                    cliThread.sendMessage(new Message(MessageType.CliTsxResponse, responseString));
                    break;
                case NewTransaction:
                    LOGGER.info("Got a new transaction from peer");
                    Transaction newTransaction = (Transaction) msg.data;
                    if (blockchain.verifyApplyTransaction(newTransaction)) {
                        LOGGER.info("Transaction accepted");
                        minerThread.maybeMine(blockchain);
                    } else {
                        LOGGER.warning("Transaction rejected");
                    }
                    break;
                case LastBlockRequest:
                    cliThread.sendMessage(new Message(MessageType.LastBlockResponse,
                            blockchain.getLastBlock()));
                    break;
                case NewBlock:
                    NewBlockData newBlockData = (NewBlockData) msg.data;
                    if (!blockchain.addBlock(newBlockData.block)) {
                        if (!blockchain.isBetter(newBlockData.block)) {
                            LOGGER.info("Drop received block, either invalid or not better that ours");
                        } else {
                            LOGGER.info("Seemingly valid block but can't add it, ask sender for his chain");
                            outPeers.send(newBlockData.id,
                                    new Message(MessageType.ChainRequest, myId));
                        }
                    } else {
                        LOGGER.info("Added received block");
                        minerThread.stopMining();
                        minerThread.maybeMine(blockchain);
                    }
                    break;
                case ChainRequest:
                    int sender = (Integer) msg.data;
                    outPeers.send(sender, new Message(MessageType.ChainResponse, blockchain.getChain()));
                    break;
                case ChainResponse:
                    @SuppressWarnings("unchecked")
                    ArrayList<Block> newChain = (ArrayList<Block>) msg.data;
                    if (newChain.size() > blockchain.getChain().size()) {
                        LOGGER.info("Received a bigger chain, try to replace mine");
                        if (blockchain.replaceChain(newChain)) {
                            LOGGER.info("Successfully replaced my chain with bigger");
                            minerThread.stopMining();
                            minerThread.maybeMine(blockchain);
                        } else {
                            LOGGER.warning("Couldn't replace chain with bigger");
                        }
                    } else {
                        LOGGER.info("Received a smaller chain");
                    }
                    break;
                case BlockMined:
                    LOGGER.info("Received new block from miner thread");
                    minerThread.blockMinedAck();
                    Block minedBlock = (Block) msg.data;
                    if (blockchain.addBlock(minedBlock)) {
                        LOGGER.info("Added mined block to my chain, broadcasting it");
                        outPeers.broadcast(new Message(MessageType.NewBlock, new NewBlockData(minedBlock, myId)));
                    } else {
                        LOGGER.info("Discarded mined block, invalid or old");
                    }
                    minerThread.maybeMine(blockchain);
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
