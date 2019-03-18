package mains;

import beans.Block;
import beans.CliTsxData;
import beans.Message;
import beans.MessageType;
import entities.Transaction;
import network.PeerInfo;
import org.apache.commons.cli.*;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;
import java.util.logging.*;

import static utilities.StringUtilities.publicKeyToString;

public class NoobCashCLI {
    private static final Logger LOGGER = Logger.getLogger("NOOBCASH");
    @SuppressWarnings("Duplicates")
    public static void main(String[] args) throws ClassNotFoundException {
        Options options = new Options();
        Option port_opt = new Option("p", "port", true, "server port");
        port_opt.setRequired(true);
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

        String p = cmd.getOptionValue("port");
        int port = Integer.parseInt(p);

        LOGGER.setUseParentHandlers(false);
        LOGGER.setLevel(Level.ALL);

        /*
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(new SimpleFormatter());
        consoleHandler.setLevel(Level.INFO);
        LOGGER.addHandler(consoleHandler);
        */

        FileHandler fileHandler;
        try {
            fileHandler = new FileHandler("noobcash-cli.%u.%g.log");
            fileHandler.setFormatter(new SimpleFormatter());
            fileHandler.setLevel(Level.ALL);
            LOGGER.addHandler(fileHandler);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        InetAddress serverAddress;
        try {
            serverAddress  = InetAddress.getByName("127.0.0.1");
        } catch (UnknownHostException e) {
            LOGGER.severe("Couldn't find backend address");
            return;
        }

        System.out.println("NoobCash cli ");

        Socket socket;
        ObjectInputStream ois;
        ObjectOutputStream oos;
        try {
            socket = new Socket(serverAddress, port);
        } catch (IOException e) {
            LOGGER.severe("Couldn't connect to backend");
            return;
        }
        LOGGER.info("Connected");
        Message msg;
        try {
            oos = new ObjectOutputStream(socket.getOutputStream());
            oos.writeObject(new Message(MessageType.IdRequest, null));
            ois = new ObjectInputStream(socket.getInputStream());
            msg = (Message) ois.readObject();
        } catch (IOException e){
            LOGGER.severe("Couldn't write-read id messages");
            return;
        }
        if (msg.messageType != MessageType.IdResponse) {
            LOGGER.severe("Instead of PeerId, got : " + msg.messageType);
            return;
        }
        int id = (Integer) msg.data;
        LOGGER.info("Got backend id : " + id);

        String line;
        BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));


        while (true) {
            System.out.print("# ");
            try {
                line = consoleReader.readLine();
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Couldn't read line", e);
                return;
            }
            LOGGER.finest("Processing command : " + line);
            if (line.trim().isEmpty()) continue;
            String[] tokens = line.trim().split(" ");
            switch (tokens[0]) {
                case "help":
                    System.out.println("*helpful message*");
                    break;
                case "peer":
                    Integer pn = tokens.length > 1 ? Integer.parseInt(tokens[1]) : null;
                    Message message = sendMessage(oos, ois, new Message(MessageType.PeerInfoRequest, pn),
                            MessageType.PeerInfoResponse);
                    if (message == null) return;
                    if (message.data == null) {
                        System.out.println("Invalid request");
                        continue;
                    }
                    if (pn == null) {
                        PeerInfo[] peers = (PeerInfo[]) message.data;
                        for (int i = 0; i < peers.length; i++){
                            System.out.println(i + " : " + publicKeyToString(peers[i].publicKey));
                        }
                    } else {
                        PeerInfo peer = (PeerInfo) message.data;
                        System.out.println(publicKeyToString(peer.publicKey));
                    }
                    break;
                case "t":
                    int pid = Integer.parseInt(tokens[1]);
                    int amount = Integer.parseInt(tokens[2]);
                    Message tMessage = sendMessage(oos, ois,
                            new Message(MessageType.CliTsxRequest, new CliTsxData(pid, amount)),
                            MessageType.CliTsxResponse);
                    if (tMessage == null) return;
                    System.out.println(tMessage.data);
                    break;
                case "view":
                    Message vMsg = sendMessage(oos, ois, new Message(MessageType.LastBlockRequest, null),
                            MessageType.LastBlockResponse);
                    if (vMsg == null) return;
                    Block block = (Block) vMsg.data;
                    System.out.println("Block index : " + block.getIndex());
                    List<Transaction> transactionList = block.getTransactions();
                    for (int i = 0; i < transactionList.size(); i++) {
                        Transaction tr = transactionList.get(i);
                        System.out.println("Transaction " + i);
                        System.out.println("    Txid = " + tr.getTxid());
                        System.out.println("    Amount = " + tr.getAmount());
                    }
                    break;
                case "balance":
                    Integer x = tokens.length > 1 ? Integer.parseInt(tokens[1]) : null;
                    Message bMsg = sendMessage(oos, ois, new Message(MessageType.BalanceRequest, x),
                            MessageType.BalanceResponse);
                    if (bMsg == null) return;
                    if (bMsg.data == null) {
                        System.out.println("Invalid request");
                        continue;
                    }
                    System.out.println(bMsg.data + " coins" );
                    break;
                case "file":
                    File file = new File("./transactions/5nodes/transactions" + id + ".txt");
                    BufferedReader fileReader;
                    try {
                        fileReader = new BufferedReader(new FileReader(file));
                    } catch (FileNotFoundException e) {
                        LOGGER.severe("Couldn't open transactions file");
                        return;
                    }
                    System.out.println("*send file cmd's to backedn*");
                    break;
                case "q":
                    LOGGER.info("Bye !");
                    return;
                default:
                    System.out.println("Sorry, what?");
            }
        }
    }

    private static Message sendMessage(ObjectOutputStream oos, ObjectInputStream ois, Message outMessage,
                                       MessageType expectedType) throws ClassNotFoundException {

        Message inMessage;
        try{
            oos.writeObject(outMessage);
            inMessage = (Message) ois.readObject();
        } catch (IOException e) {
            LOGGER.severe("Couldn't write-read message");
            return null;
        }
        if (inMessage.messageType != expectedType) {
            LOGGER.severe("Instead of " + expectedType + " got : " + inMessage.messageType);
            return null;
        }
        return inMessage;
    }
}
