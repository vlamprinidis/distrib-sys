package entities;

import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;

public class UTXOs {
    private HashMap<UUID, TransactionOutput> map;
    UTXOs(UTXOs utxOs) {
        map = new HashMap<>(utxOs.map);
    }
    UTXOs (){
        map = new HashMap<>();
    }

    Collection<TransactionOutput> values(){
        return map.values();
    }

    /*
     * Return output that is pointed by input
     */
    TransactionOutput get(TransactionInput input) {
        return map.get(input.getPreviousOutputId());
    }

    void add(TransactionOutput output) {
        map.put(output.getId(), output);
    }

    /*
     * Remove output that is pointed by input
     */
    void remove(TransactionInput input) {
        map.remove(input.getPreviousOutputId());
    }

    public int size() {
        return map.size();
    }
}
