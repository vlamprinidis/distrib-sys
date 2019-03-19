package noobcash.communication;

import noobcash.entities.Block;

import java.io.Serializable;

public class NewBlockData implements Serializable {
    public Block block;
    public int id;

    public NewBlockData(Block block, int id) {
        this.block = block;
        this.id = id;
    }
}
