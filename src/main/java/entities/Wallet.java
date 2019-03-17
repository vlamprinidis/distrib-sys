package entities;

import java.io.Serializable;
import java.security.*;
import static java.security.KeyPairGenerator.getInstance;

public class Wallet implements Serializable {

    private PublicKey publicKey;
    private PrivateKey privateKey;

    public Wallet() throws NoSuchAlgorithmException {
        KeyPair keyPair = generateKeyPair();
        publicKey = keyPair.getPublic();
        privateKey = keyPair.getPrivate();
    }

    private KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator generator = getInstance("RSA");
        generator.initialize(2048, new SecureRandom());
        return generator.generateKeyPair();
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    PrivateKey getPrivateKey() {
        return privateKey;
    }
}
