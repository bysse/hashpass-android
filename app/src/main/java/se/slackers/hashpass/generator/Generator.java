package se.slackers.hashpass.generator;

import android.util.Base64;

import java.security.MessageDigest;

/**
 * Created by erikb on 2014-12-30.
 */
public class Generator {
    public static String generate(String salt, String password) {
        try {
            byte[] input = (salt + "/" + password).getBytes("UTF_8");

            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            for (int i=0;i<65536;i++) {
                digest.update(input);
                input = digest.digest();
                digest.reset();
            }

            return new String(Base64.encode(input, 0, 12, 0), "UTF_8");
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }
}
