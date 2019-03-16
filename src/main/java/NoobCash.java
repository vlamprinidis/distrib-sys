import beans.Block;
import entities.*;

import java.util.ArrayList;


public class NoobCash {
    public static ArrayList<Block> blockchain = new ArrayList<Block>();
    public static int difficulty = 5;
    public static Wallet walletA;
    public static Wallet walletB;

    public static void main(String[] args) {
        /*
        walletA = new Wallet();
        walletB = new Wallet();
        //Test public and private keys
        System.out.println("Private and public keys:");
        System.out.println(walletA.privateKey);
        System.out.println(walletA.publicKey);

        Transaction transaction = new Transaction(walletA, walletB, 10, null, null);
        transaction.generateSignature(walletA);
        //Verify the signature works and verify it from the public key
        System.out.println("Is signature verified");
        System.out.println(transaction.verifySignature());
        */

    }
}
