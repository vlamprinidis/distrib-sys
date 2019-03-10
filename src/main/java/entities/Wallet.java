package entities;

import java.io.Serializable;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.security.KeyPairGenerator.getInstance;

public class Wallet implements Serializable {
    public static Logger LOGGER = Logger.getLogger(Wallet.class.getName());

    public PublicKey publicKey;
    private PrivateKey privateKey;

    public HashMap<String,TransactionOutput> UTXOs = new HashMap<String,TransactionOutput>(); //only UTXOs owned by this wallet.

    public Wallet(){
        KeyPair keyPair = generateKeyPair();
        publicKey = keyPair.getPublic();
        privateKey = keyPair.getPrivate();

    }

    /**
     * Function generating a new Keypair of public and private key for this wallet
     */
    public KeyPair generateKeyPair() {
        try {
            KeyPairGenerator generator = getInstance("RSA");
            generator.initialize(2048, new SecureRandom());
            return generator.generateKeyPair();
        }
        catch (NoSuchAlgorithmException e){
            return null;
        }
    }

    public void generateSignature(){

    }


    /**
     * Get the balance on this wallet
     * @param allUTXOs (unspent transactions)
     * @return the balance as float
     */
    public float getBalance(HashMap<String,TransactionOutput> allUTXOs) {
        return 0f;
    }

    /**
     * Return and creates a transaction from this wallet to a recipient knowing its public key
     * @param _recipient
     * @param value
     * @param allUTXOs
     * @return
     */
    public Transaction sendFunds(PublicKey _recipient, float value, HashMap<String,TransactionOutput> allUTXOs) {
        return null;
    }
}
