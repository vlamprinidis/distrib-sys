package utilities;

import entities.Transaction;

import java.security.*;
import java.util.ArrayList;
import java.util.List;

public class StringUtilities {
    /**
     * Function that applies the hash function to a string
     * @param String input
     * @return the hash value
     */
    public static String applySha256(String input){
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            //Applies sha256 to our input,
            byte[] hash = digest.digest(input.getBytes("UTF-8"));
            StringBuffer hexString = new StringBuffer(); // This will contain hash as hexidecimal
            for (int i = 0; i < hash.length; i++) {
                String hex = Integer.toHexString(0xff & hash[i]);
                if(hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        }
        catch(Exception e) {
            throw new RuntimeException(e);
        }
    }


    //Applies ECDSA Signature and returns the result ( as bytes ).
    public static byte[] applyECDSASig(PrivateKey privateKey, String input) {
        return null;
    }

    //Verifies a String signature
    public static boolean verifyECDSASig(PublicKey publicKey, String data, byte[] signature) {
        return true;
    }


}
