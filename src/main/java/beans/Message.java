package beans;

import java.io.Serializable;

/*
 * Todo : Message Class Contains anything that will be sent above the network
 */
public class Message implements Serializable {
    public MessageType messageType;
    public Object data;
    public Message(MessageType messageType, Object data) {
        this.messageType = messageType;
        this.data = data;
    }

}
