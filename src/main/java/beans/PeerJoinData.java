package beans;

import java.io.Serializable;
import java.security.PublicKey;

public class PeerJoinData implements Serializable {
    public int port;
    public PublicKey publicKey;

    public PeerJoinData(int port, PublicKey publicKey) {
        this.port = port;
        this.publicKey = publicKey;
    }
}
