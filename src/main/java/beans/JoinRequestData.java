package beans;

import java.io.Serializable;
import java.security.PublicKey;

public class JoinRequestData implements Serializable {
    public int port;
    public PublicKey publicKey;

    public JoinRequestData(int port, PublicKey publicKey) {
        this.port = port;
        this.publicKey = publicKey;
    }
}
