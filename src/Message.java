
import java.io.*;

// message class to send message to other peer with its type
public class Message implements Serializable {

    static final int SEND = 1, STOP = 2;
    private int type;
    private String message;
    Message(int type, String message) {
        this.type = type;
        this.message = message;
    }
    int getType() {
        return type;
    }
    String getMessage() {
        return message;
    }
}
