package noobcash;

import noobcash.entities.Block;
import noobcash.entities.Blockchain;
import noobcash.entities.Transaction;
import noobcash.entities.Wallet;
import noobcash.network.InPeers;
import noobcash.network.OutPeers;
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

import noobcash.communication.*;
import noobcash.network.*;
import noobcash.threads.CliThread;
import noobcash.threads.MinerThread;

import static noobcash.utilities.ErrorUtilities.fatal;


public class Backend {
    private static final Logger LOGGER = Logger.getLogger("NOOBCASH");
    private static final int BS_PORT = 5000;

    @SuppressWarnings({"Duplicates", "InfiniteLoopStatement"})
    public static void main(String[] args) throws ClassNotFoundException {
        Options options = new Options();
        Option num_opt = new Option("n", "number", true, "number of peers");
        num_opt.setRequired(true);
        options.addOption(num_opt);

        Option port_opt = new Option("p", "port", true, "port to use");
        options.addOption(port_opt);

        Option block_opt = new Option("b", "block", true, "block size");
        block_opt.setRequired(true);
        options.addOption(block_opt);

        Option diff_opt = new Option("d", "difficulty", true, "PoW difficulty");
        diff_opt.setRequired(true);
        options.addOption(diff_opt);

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
        String b = cmd.getOptionValue("block");
        String d = cmd.getOptionValue("difficulty");
        String p = cmd.getOptionValue("port");

        final int networkSize = Integer.parseInt(n);
        final int blockSize = Integer.parseInt(b);
        final int difficulty = Integer.parseInt(d);
        final boolean isBootstrap = p == null;
        final int myPort = (isBootstrap) ? BS_PORT : Integer.parseInt(p);


        LOGGER.setUseParentHandlers(false);
        LOGGER.setLevel(Level.ALL);

        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(new SimpleFormatter());
        consoleHandler.setLevel(Level.INFO);
        LOGGER.addHandler(consoleHandler);

        FileHandler fileHandler;
        try {
            fileHandler = new FileHandler("noobcash.n-" + networkSize + ".b-" + blockSize + ".d-" +
                    difficulty + ".%u.log");
            fileHandler.setFormatter(new SimpleFormatter());
            fileHandler.setLevel(Level.ALL);
            LOGGER.addHandler(fileHandler);
        } catch (IOException e) {
            LOGGER.warning("Can't open log file");
        }

        InetAddress BS_ADDR;
        try {
            BS_ADDR = InetAddress.getByName("192.168.0.1");
        } catch (UnknownHostException e) {
            LOGGER.severe("Can't find BS address");
            return;
        }

        int myId;
        PeerInfo[] peers;
        BlockingQueue<Message> inQueue = new LinkedBlockingDeque<>();
        // InPeers inPeers;
        OutPeers outPeers;
        Wallet wallet;
        try {
            wallet = new Wallet();
        } catch (NoSuchAlgorithmException e) {
            LOGGER.severe("Can't generate key pair");
            return;
        }

        Blockchain blockchain = new Blockchain(wallet, blockSize, difficulty);

        CliThread cliThread = new CliThread(myPort + 1, inQueue);
        cliThread.setDaemon(true);
        cliThread.start();

        MinerThread minerThread = new MinerThread(inQueue, difficulty);
        minerThread.setDaemon(true);
        minerThread.start();

        if (isBootstrap) {
            Block genesisBlock = blockchain.generateGenesis(networkSize);
            if (!blockchain.addBlock(genesisBlock)) fatal("Can't add genesis block");

            myId = 0;
            peers = new PeerInfo[networkSize];
            ServerSocket server;
            try {
                server = new ServerSocket(BS_PORT);
            } catch (IOException e) {
                fatal("Can't open server socket");
                return;
            }
            peers[0] = new PeerInfo(BS_ADDR, BS_PORT, null, wallet.getPublicKey());
            for (int i = 1; i < networkSize; i++){
                Socket socket;
                try {
                    socket = server.accept();
                } catch (IOException e) {
                    fatal("Can't accept connection");
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
                    LOGGER.fine("Peer connected, port = " + data.port);
                } else {
                    fatal("Unexpected message : "+ msg.messageType);
                }
                Transaction tsx = blockchain.createTransaction(peers[i].publicKey, 100);
                if (tsx == null) fatal("Can't create initial transaction");
                if (!blockchain.verifyApplyTransaction(tsx)) fatal("Can't verify self-made initial transaction");
            }
            LOGGER.info("All peers have joined");
            while(blockchain.isFull()) {
                Block block = blockchain.createBlock();
                Random randomStream = new Random();
                // Explicit to avoid warnings
                while(true) {
                    if (block.tryMine(randomStream.nextInt(), difficulty)) break;
                }
                if (!blockchain.addBlock(block)) fatal("Can't add initial block's to chain");
            }

            LOGGER.info("Initial confirmed UTXOs size : " + blockchain.getConfirmedUTXOs().size());
            LOGGER.info("Initial unconfirmed UTXOs size : " + blockchain.getUnconfirmedUTXOs().size());

            for(int j = 1; j < networkSize; j++){
                ObjectOutputStream oos;
                try {
                    oos = new ObjectOutputStream(peers[j].server_socket.getOutputStream());
                    JoinResponseData data = new JoinResponseData(j, peers, blockchain.getChain(),
                            blockchain.getTsxPool(), blockchain.getGenesisUTXO());
                    oos.writeObject(new Message(MessageType.JoinResponse, data));
                } catch (IOException e) {
                    fatal("Can't send initial data to peer");
                }
            }
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
                fatal("Can't establish communication with BS");
                return;
            }
            try {
                msg = (Message) ois.readObject();
            } catch (IOException e) {
                fatal("Can't read initial joinData");
                return;
            }
            if (msg.messageType != MessageType.JoinResponse) {
                fatal("Instead of JoinResponse, got : " + msg.messageType);
            }
            JoinResponseData data = (JoinResponseData) msg.data;
            myId = data.id;
            peers = data.peerInfo;

            // Manually set genesisUTXO (= input of first TSX that gives BS coins)
            blockchain.setGenesisUTXO(data.genesisUTXO);

            if (!blockchain.replaceChain(data.chain)) fatal("Can't verify initial chain");

            // Pretend that you received initial unconfirmed transaction, adding each one
            for (Transaction t : data.tsxPool) {
                if (!blockchain.verifyApplyTransaction(t)) fatal("Can't verify initial unconfirmed transactions");
            }
            outPeers = new OutPeers(peers, socket, myId);
        }

        LOGGER.info("Processing incoming messages");

        Message msg;
        while(true) {
            try {
                msg = inQueue.take();
            } catch (InterruptedException e) {
                LOGGER.severe("Interrupted while take'ing message from queue");
                continue;
            }

            LOGGER.finest("Processing : " + msg.messageType);
            switch (msg.messageType) {
                case IdRequest:
                    cliThread.sendMessage(new Message(MessageType.IdResponse, myId));
                    break;
                case BalanceRequest:
                    int x = msg.data == null ? myId : (Integer) msg.data;
                    if (x >= networkSize) {
                        LOGGER.warning("CLI request with invalid id (balance)");
                        cliThread.sendMessage(new Message(MessageType.BalanceResponse,
                                null));
                        break;
                    }
                    cliThread.sendMessage(new Message(MessageType.BalanceResponse,
                            blockchain.getBalance(peers[x].publicKey)));
                    break;
                case BalancesRequest:
                    int[] balances = new int[networkSize];
                    for (int k = 0; k < networkSize; k++) balances[k] = blockchain.getBalance(peers[k].publicKey);
                    cliThread.sendMessage(new Message(MessageType.BalancesResponse, balances));
                    break;
                case PeerInfoRequest:
                    Integer y = (Integer) msg.data;
                    if (y != null) {
                        if (y >= networkSize) {
                            LOGGER.warning("CLI request with invalid id (peer info)");
                            cliThread.sendMessage(new Message(MessageType.PeerInfoResponse, "Invalid id"));
                            break;
                        }
                        cliThread.sendMessage(new Message(MessageType.PeerInfoResponse, peers[y]));
                    } else {
                        cliThread.sendMessage(new Message(MessageType.PeerInfoResponse, peers));
                    }
                    break;
                case CliTsxRequest:
                    CliTsxRequestData cliTsxRequestData = (CliTsxRequestData) msg.data;
                    if (cliTsxRequestData.id >= networkSize) {
                        LOGGER.warning("CLI request with invalid id (transaction)");
                        cliThread.sendMessage(new Message(MessageType.CliTsxResponse, "Invalid id"));
                        break;
                    }
                    Transaction cliTsx = blockchain.createTransaction(peers[cliTsxRequestData.id].publicKey,
                            cliTsxRequestData.amount);
                    String responseString;
                    if (cliTsx == null) {
                        responseString = "Transaction rejected";
                    } else {
                        if (!blockchain.verifyApplyTransaction(cliTsx)) fatal("Can't verify transaction I just made");
                        responseString = "Transaction accepted";
                        minerThread.maybeMine(blockchain);
                        outPeers.broadcast(new Message(MessageType.NewTransaction, cliTsx));
                    }
                    cliThread.sendMessage(new Message(MessageType.CliTsxResponse, responseString));
                    break;
                case NewTransaction:
                    Transaction newTransaction = (Transaction) msg.data;
                    LOGGER.finest("New transaction received from peer, txid = " + newTransaction.getTxid());
                    if (blockchain.verifyApplyTransaction(newTransaction)) {
                        LOGGER.finest("Transaction accepted");
                        minerThread.maybeMine(blockchain);
                    } else {
                        LOGGER.info("Transaction rejected");
                    }
                    break;
                case LastBlockRequest:
                    cliThread.sendMessage(new Message(MessageType.LastBlockResponse,
                            blockchain.getLastBlock()));
                    break;
                case NewBlock:
                    NewBlockData newBlockData = (NewBlockData) msg.data;
                    LOGGER.info("New block received from peer, index = " + newBlockData.block.getIndex());
                    if (!blockchain.addBlock(newBlockData.block)) {
                        if (!blockchain.possibleLongerFork(newBlockData.block)) {
                            LOGGER.fine("Discarding received (from peer) block");
                        } else {
                            LOGGER.info("Requesting chain, longer fork possibly exists");
                            outPeers.send(newBlockData.id, new Message(MessageType.ChainRequest, myId));
                        }
                    } else {
                        LOGGER.info("Added received (from peer) block");
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
                        LOGGER.info("Received a bigger chain, length : " + newChain.size());
                        if (blockchain.replaceChain(newChain)) {
                            LOGGER.info("Replaced chain with bigger");
                            minerThread.stopMining();
                            minerThread.maybeMine(blockchain);
                        } else {
                            LOGGER.warning("Bad received chain");
                        }
                    } else {
                        LOGGER.info("Received a smaller chain, length : " + newChain.size());
                    }
                    break;
                case BlockMined:
                    Block minedBlock = (Block) msg.data;
                    LOGGER.info("New block received from miner, index = " + minedBlock.getIndex());
                    if (blockchain.addBlock(minedBlock)) {
                        LOGGER.info("Added received (from miner) block");
                        minerThread.maybeMine(blockchain);
                        outPeers.broadcast(new Message(MessageType.NewBlock, new NewBlockData(minedBlock, myId)));
                    } else {
                        LOGGER.info("Discarding received (from miner) block");
                        minerThread.maybeMine(blockchain);
                    }
                    break;
                default:
                    LOGGER.warning("Unexpected message : " + msg.messageType);
            }
        }
    }
}
