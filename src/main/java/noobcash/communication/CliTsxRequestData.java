package noobcash.communication;

import java.io.Serializable;

public class CliTsxRequestData implements Serializable {
    public int id;
    public int amount;

    public CliTsxRequestData(int id, int amount) {
        this.id = id;
        this.amount = amount;
    }
}
