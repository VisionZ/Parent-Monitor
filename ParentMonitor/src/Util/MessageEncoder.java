package Util;

import static Server.Network.ENCODING;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.StringTokenizer;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

public final class MessageEncoder {
   
    private final Key key;
    private final Cipher encoder;
    private final Cipher decoder;
    
    public MessageEncoder(byte[] keyValue, String algorithm) {
        Key secretKey = null;
        Cipher encrypt = null;
        Cipher decrypt = null;
        boolean cipherGenerationError = false;
        try {
            encrypt = Cipher.getInstance(algorithm);
            decrypt = Cipher.getInstance(algorithm);
            secretKey = new SecretKeySpec(keyValue, algorithm);
            encrypt.init(Cipher.ENCRYPT_MODE, secretKey);
            decrypt.init(Cipher.DECRYPT_MODE, secretKey);
        }
        catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException ex) {
            cipherGenerationError = true;
            ex.printStackTrace();
        }
        if (cipherGenerationError) {
            key = null;
            encoder = decoder = null;
        }
        else {
            key = secretKey;
            encoder = encrypt;
            decoder = decrypt;
        }
    }
    
    public final boolean isValid() {
        return key != null && encoder != null && decoder != null;
    }
    
    public synchronized final String encode(final String str) {
        if (str == null) {
            throw new NullPointerException();
        }
        
        if (str.isEmpty()) {
            return "";
        }
        
        final byte[] encode = encrypt(str.getBytes(ENCODING));
        if (encode == null) {
            return null;
        }
        
        final int lastIndex = encode.length - 1;
        if (lastIndex == -1) {
            return "";
        }
        
        int capacity = lastIndex;
        for (int index = 0; index < lastIndex; ++index) {
            capacity += getDigitCount(encode[index]);
        }
        capacity += getDigitCount(encode[lastIndex]);
        
        final StringBuilder buffer = new StringBuilder(capacity);
        for (int index = 0; index < lastIndex; ++index) {
            buffer.append(encode[index]).append(',');
        }
        return buffer.append(encode[lastIndex]).toString();
    }
   
    public synchronized final String decode(final String str) {
        if (str == null) {
            throw new NullPointerException();
        }
        if (str.isEmpty()) {
            return "";
        }
        final StringTokenizer tokenizer = new StringTokenizer(str, ",");
        final int length = tokenizer.countTokens();
        final byte[] bytes = new byte[length];
        for (int index = 0; index < length; ++index) {
            bytes[index] = Byte.parseByte(tokenizer.nextToken());
        }
        final byte[] decode = decrypt(bytes);
        return decode == null ? null : (new String(decode, ENCODING));
    }

    //Use 2 ciphers to reduce thread contention while not making new Cipher objects
    //on every method call
    
    private byte[] encrypt(final byte[] unsecureData) {
        try {
            return encoder.doFinal(unsecureData);
        }
        catch (NullPointerException | IllegalBlockSizeException | BadPaddingException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    private byte[] decrypt(final byte[] secureData) {
        try {
            return decoder.doFinal(secureData);
        }
        catch (NullPointerException | IllegalBlockSizeException | BadPaddingException ex) {
            ex.printStackTrace();
        }
        return null;
    }
    
    private static int getDigitCount(int num) {
        if (num == 0) {
            return 1;
        }
        int count;
        if (num < 0) {
            num = -num;
            count = 2;
        }
        else {
            count = 1;
        }
        for (num /= 10; num > 0; num /= 10) {
            ++count;
        }
        return count;
    }
}