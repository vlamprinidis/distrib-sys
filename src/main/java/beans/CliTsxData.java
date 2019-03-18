package beans;

import java.io.Serializable;

public class CliTsxData implements Serializable {
    public int id;
    public int amount;

    public CliTsxData(int id, int amount) {
        this.id = id;
        this.amount = amount;
    }
}
