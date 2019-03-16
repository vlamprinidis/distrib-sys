package beans;

import java.io.Serializable;

/*
 * No time for proper generic message types etc, just tag + data
 */

public class Message implements Serializable {
    public MessageType messageType;
    public Object data;
    public Message(MessageType messageType, Object data) {
        this.messageType = messageType;
        this.data = data;
    }

}
