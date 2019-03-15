package entities;

import java.io.Serializable;
import java.security.*;
import static java.security.KeyPairGenerator.getInstance;

public class Wallet implements Serializable {

    private PublicKey publicKey;
    private PrivateKey privateKey;

    public Wallet(){
        KeyPair keyPair = generateKeyPair();
        publicKey = keyPair.getPublic();
        privateKey = keyPair.getPrivate();
    }

    private KeyPair generateKeyPair() {
        try {
            KeyPairGenerator generator = getInstance("RSA");
            generator.initialize(2048, new SecureRandom());
            return generator.generateKeyPair();
        }
        catch (NoSuchAlgorithmException e){
            return null;
        }
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }
}
