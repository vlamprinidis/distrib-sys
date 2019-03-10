package mains;

import beans.Message;
import beans.MessageType;
import org.apache.commons.cli.*;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class NoobCashCLI {
    @SuppressWarnings("Duplicates")
    public static void main(String[] args) {
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

        InetAddress serverAddress = null;
        try {
            serverAddress  = InetAddress.getByName("127.0.0.1");
        } catch (UnknownHostException e) {
            e.printStackTrace();
            System.exit(1);
        }

        Socket socket = null;
        ObjectInputStream ois = null;
        ObjectOutputStream oos = null;
        try {
            socket = new Socket(serverAddress, port);
            System.out.println("Connected");
            oos = new ObjectOutputStream(socket.getOutputStream());
            oos.writeObject(new Message(MessageType.IdRequest, null));
            ois = new ObjectInputStream(socket.getInputStream());
            Message msg = (Message) ois.readObject();
            assert msg.messageType == MessageType.PeerID;
            int id = (Integer) msg.data;
            System.out.println("Got id : " + id);
            Thread.sleep(800000);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
