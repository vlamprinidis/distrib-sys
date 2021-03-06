package noobcash;

import noobcash.entities.Block;
import noobcash.communication.CliTsxRequestData;
import noobcash.communication.Message;
import noobcash.communication.MessageType;
import noobcash.entities.Transaction;
import noobcash.network.PeerInfo;
import org.apache.commons.cli.*;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;

import static noobcash.utilities.StringUtilities.publicKeyToString;

public class Cli {
    @SuppressWarnings("Duplicates")
    public static void main(String[] args) throws ClassNotFoundException {
        Options options = new Options();
        Option port_opt = new Option("p", "port", true, "server port");
        options.addOption(port_opt);

        Option file_opt = new Option("f", "file", false, "file transactions first");
        options.addOption(file_opt);

        Option ten_opt = new Option("t", "ten", false, "use 10nodes instead of 5nodes");
        options.addOption(ten_opt);

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
        int port = cmd.hasOption("p") ? Integer.parseInt(p) : Backend.BS_PORT;
        // CLI will be using port + 1
        port++;
        boolean file_first = cmd.hasOption("f");
        boolean ten = cmd.hasOption("t");

        InetAddress serverAddress;
        try {
            serverAddress  = InetAddress.getByName("127.0.0.1");
        } catch (UnknownHostException e) {
            fatal("Can't find backend address");
            return;
        }

        System.out.println("NoobCash cli");

        Socket socket;
        ObjectInputStream ois;
        ObjectOutputStream oos;
        try {
            socket = new Socket(serverAddress, port);
        } catch (IOException e) {
            fatal("Can't connect to backend");
            return;
        }
        System.out.println("Connected !");
        Message msg;
        try {
            oos = new ObjectOutputStream(socket.getOutputStream());
            oos.writeObject(new Message(MessageType.IdRequest, null));
            ois = new ObjectInputStream(socket.getInputStream());
            msg = (Message) ois.readObject();
        } catch (IOException e){
            fatal("Can't write-read id messages");
            return;
        }
        if (msg.messageType != MessageType.IdResponse) {
            fatal("Instead of PeerId, got : " + msg.messageType);
            return;
        }
        int id = (Integer) msg.data;
        System.out.println("ID : " + id);

        if (file_first) fileTransaction(oos, ois, id, ten);

        String line;
        BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));

        while (true) {
            System.out.print("# ");
            try {
                line = consoleReader.readLine();
            } catch (IOException e) {
                fatal("Can't read line");
                return;
            }
            if (line.trim().isEmpty()) continue;
            String[] tokens = line.trim().split(" ");
            switch (tokens[0]) {
                case "help":
                    System.out.println("help : print this message");
                    System.out.println("peer <id> : print public key of peer with given id");
                    System.out.println("peer : print public key of every peer");
                    System.out.println("t <id> <amount> : send amount coins to peer with given id");
                    System.out.println("view : print transactions of last confirmed block");
                    System.out.println("balance : print my balance");
                    System.out.println("balance <id> : print balance of peer with given id");
                    System.out.println("balances : print node's balance");
                    System.out.println("file : read and apply every transaction in file");
                    System.out.println("q : quit");
                    break;
                case "peer":
                    Integer pn = tokens.length > 1 ? Integer.parseInt(tokens[1]) : null;
                    Message message = sendMessage(oos, ois, new Message(MessageType.PeerInfoRequest, pn),
                            MessageType.PeerInfoResponse);
                    // Will never happen but compiler can't know
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
                            new Message(MessageType.CliTsxRequest, new CliTsxRequestData(pid, amount)),
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
                    System.out.println("Block nonce : " + block.getNonce());
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
                case "balances":
                    Message bsMsg = sendMessage(oos, ois, new Message(MessageType.BalancesRequest, null),
                            MessageType.BalancesResponse);
                    if (bsMsg == null) return;
                    int[] balances = (int[]) bsMsg.data;
                    for (int k = 0; k < balances.length; k++) {
                        System.out.println(k + " : " + balances[k] + " coins");
                    }
                    break;
                case "file":
                    fileTransaction(oos, ois, id, ten);
                    break;
                case "q":
                    System.out.println("Bye!");
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
            fatal("Cant't write-read message");
            return null;
        }
        if (inMessage.messageType != expectedType) {
            fatal("Instead of " + expectedType + " got : " + inMessage.messageType);
            return null;
        }
        return inMessage;
    }

    private static void fileTransaction(ObjectOutputStream oos, ObjectInputStream ois, int id, boolean ten)
            throws ClassNotFoundException {
        int size = ten ? 10 : 5;
        File file = new File("./transactions/" + size + "nodes/transactions" + id + ".txt");
        BufferedReader fileReader;
        try {
            fileReader = new BufferedReader(new FileReader(file));
        } catch (FileNotFoundException e) {
            fatal("Can't open transactions file");
            return;
        }
        System.out.println("Reading transactions" + id + ".txt");
        String line;
        try {

        while ((line = fileReader.readLine()) != null) {
            String[] tokens = line.trim().split(" ");
            int x = Character.getNumericValue(tokens[0].charAt(2));
            int c = Integer.parseInt(tokens[1]);
            System.out.println("Transaction : " + c + " coins -> " + x);
            Message resp = sendMessage(oos, ois, new Message(MessageType.CliTsxRequest, new CliTsxRequestData(x, c)),
                    MessageType.CliTsxResponse);
            if (resp == null) {
                fileReader.close();
                return;
            }
            System.out.println(resp.data);
        }
        fileReader.close();
        } catch (IOException e) {
            fatal(e.toString());
        }
    }

    private static void fatal(String message) {
        System.err.println(message);
        System.exit(1);
    }
}
