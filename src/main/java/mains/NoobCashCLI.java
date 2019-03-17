package mains;

import beans.Message;
import beans.MessageType;
import org.apache.commons.cli.*;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.logging.*;

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

        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(new SimpleFormatter());
        consoleHandler.setLevel(Level.INFO);
        LOGGER.addHandler(consoleHandler);

        FileHandler fileHandler;
        try {
            fileHandler = new FileHandler("noobcash-cli.%u.%g.log");
            fileHandler.setFormatter(new SimpleFormatter());
            fileHandler.setLevel(Level.ALL);
            LOGGER.addHandler(fileHandler);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, e.toString(), e);
        }

        InetAddress serverAddress;
        try {
            serverAddress  = InetAddress.getByName("127.0.0.1");
        } catch (UnknownHostException e) {
            LOGGER.severe("Couldn't find backend address");
            return;
        }

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
        System.out.println("ok");
        BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("ok!");


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
                case "t":
                    System.out.print("New tsx cmd");
                    break;
                case "view":
                    System.out.println("View tsxs");
                    break;
                case "balance":
                    Integer x = tokens.length > 1 ? Integer.parseInt(tokens[1]) : null;
                    try{
                        oos.writeObject(new Message(MessageType.BalanceRequest, x));
                        msg = (Message) ois.readObject();
                    } catch (IOException e) {
                        LOGGER.severe("Couldn't write-read balance messages");
                        return;
                    }
                    if (msg.messageType != MessageType.BalanceResponse) {
                        LOGGER.severe("Instead of BalanceResponse, got : " + msg.messageType);
                        return;
                    }
                    System.out.println(msg.data + " coins" );
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
}
