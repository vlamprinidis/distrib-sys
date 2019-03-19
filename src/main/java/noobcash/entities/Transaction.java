package noobcash.entities;

import noobcash.utilities.StringUtilities;

import java.io.Serializable;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;
import java.util.logging.Logger;

public class Transaction implements Serializable {

    private transient static final Logger LOGGER = Logger.getLogger("NOOBCASH");
    private PublicKey senderAddress;
    private PublicKey receiverAddress;
    private int amount;
    private long timestamp;
    private ArrayList<TransactionInput> inputs;
    private String txid;
    private ArrayList<TransactionOutput> outputs;
    private String signature;

    Transaction(PublicKey senderAddress, PublicKey receiverAddress, ArrayList<TransactionInput> inputs,
                       int amount, int change){
        this.senderAddress = senderAddress;
        this.receiverAddress = receiverAddress;
        this.amount = amount;
        timestamp = new Date().getTime();
        this.inputs = inputs;
        txid = calculateHash();
        outputs = new ArrayList<>();
        outputs.add(new TransactionOutput(txid, receiverAddress, amount));
        if (change > 0) {
            outputs.add(new TransactionOutput(txid, senderAddress, change));
        }
    }

    private String getStringData(){
        StringBuilder data = new StringBuilder();
        data.append(senderAddress);
        data.append(receiverAddress);
        data.append(amount);
        data.append(timestamp);
        for (TransactionInput tr : inputs) {
            data.append(tr.getPreviousOutputId());
        }
        return data.toString();
    }

    private String calculateHash() {
        return StringUtilities.applySha256(getStringData());
    }

    void sign(PrivateKey privateKey) {
        signature = StringUtilities.sign(getStringData(), privateKey);
    }

    private boolean verifySignature() {
        return StringUtilities.verify(getStringData(), this.signature, senderAddress);
    }

    private boolean verifyTxid(){
        return txid.equals(calculateHash());
    }

    /*
     * Verify that transaction is valid according to given UTXO
     * Doesn't modify any structure
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    boolean verify(UTXOs UTXOs) {
        if (!(verifySignature() && verifyTxid())) {
            LOGGER.warning("Invalid signature or txid !?");
            return false;
        }
        int inSum = 0, outSum = 0;
        for (TransactionInput input : inputs) {
            TransactionOutput output = UTXOs.get(input);
            if (output == null) return false;
            if (!output.belongsTo(senderAddress)) {
                LOGGER.warning("Transaction inputs don't belong to transaction sender ?!");
                return false;
            }
            inSum += output.getAmount();
        }
        for (TransactionOutput output : outputs) {
            if (!output.getParentTransactionId().equals(txid)) {
                LOGGER.warning("Transaction output parentId and txid don't match ?!");
                return false;
            }
            outSum += output.getAmount();
        }
        if (inSum != outSum) {
            LOGGER.warning("Transaction sum(in) != sum(out) ?!");
            return false;
        }
        return true;
    }

    /*
     * Apply a transaction to given UTXOs
     * No validation at all
     */
    void apply(UTXOs utxOs) {
        inputs.forEach(utxOs::remove);
        outputs.forEach(utxOs::add);
    }

    public String getTxid() {
        return txid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return txid.equals(that.txid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(txid);
    }

    public int getAmount() {
        return amount;
    }
}

