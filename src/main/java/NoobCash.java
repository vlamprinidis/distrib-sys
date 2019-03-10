import beans.Block;

public class NoobCash {
    public static void main(String[] args) {

        Block genesisBlock = new Block("Hi im the first block", "0",0);
        System.out.println("Hash for block 1 : " + genesisBlock.current_hash);

        Block secondBlock = new Block("Yo im the second block",genesisBlock.current_hash,1);
        System.out.println("Hash for block 2 : " + secondBlock.current_hash);

        Block thirdBlock = new Block("Hey im the third block",secondBlock.current_hash,2);
        System.out.println("Hash for block 3 : " + thirdBlock.current_hash);

    }
}
