package Server;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.StringTokenizer;

public final class Network {

    private Network() {

    }

    public static final Charset ENCODING = StandardCharsets.UTF_8;

    /**
     * http://www.allkeysgenerator.com/Random/Security-Encryption-Key-Generator.aspx
     * 
     * In practice, key security is accomplished through
     * much more sophisticated means.
     * 
     * The simplest way to secure keys is to have all parties
     * agree on a key beforehand and each party must keep this key private.
     * If the key is compromised a new key must be agreed upon. 
     * 
     * Sometimes security keys are even stored in physical 
     * areas (such as a safe), to keep them
     * away from the dangerous digital realm.
     */
    public static final byte[] SECURITY_KEY = "H@McQfTjWnZr4u7x".getBytes(ENCODING);
    
    /**
     * Port used for Client-Server text communication.
     */
    public static final int TEXT_PORT = 2200;

    /**
     * Port used for Client-Server image transmission.
     */
    public static final int IMAGE_PORT = 2500;

    /**
     * 1MB buffer size for sending and receiving images.
     */
    public static final int IMAGE_BUFFER_SIZE = 1024 * 1024;

    /**
     * Image File Format used by this application.
     */
    public static final String PNG = "PNG";

    /**
     * Message sent by server to client ordering the client to send a screen
     * shot.
     *
     * This is no longer needed, instead of sending a slow request to the client
     * for a image, we just expect the client to send a image at all times. This
     * reduces latency.
     */
    @Deprecated
    public static final String REQUEST_IMAGE = "Server Request: Screenshot";

    /**
     * Message sent by server to client ordering the client to close their side
     * of the application.
     */
    public static final String CLOSE_CLIENT = "Server Request: Exit";

    /**
     * Message sent by client to server when client closes their side of the
     * application.
     */
    public static final String CLIENT_EXITED = "Client Action: Exit";

    /**
     * Message sent by server to client ordering the client to shutdown their
     * entire device.
     */
    public static final String PUNISH = "Server Request: Shutdown";
    
    public static final MessageDigest SHA_1;
    
    static {
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance("SHA-1");
        }
        catch (NoSuchAlgorithmException ex) {
            messageDigest = null;
            ex.printStackTrace();
        }
        SHA_1 = messageDigest;
    }
    
    public static final boolean isLocalAddress(InetAddress address) {
        // Check if the address is a valid special local or loop back
        if (address.isAnyLocalAddress() || address.isLoopbackAddress()) {
            return true;
        }

        // Check if the address is defined on any interface
        try {
            return NetworkInterface.getByInetAddress(address) != null;
        }
        catch (SocketException ex) {
            return false;
        }
    }
    
    public static final String encode(final String str) {
        if (str == null) {
            throw new NullPointerException();
        }
        
        if (str.isEmpty()) {
            return "";
        }
        
        final byte[] encode = str.getBytes(ENCODING);
        
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
   
    public static final String decode(final String str) {
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
        return new String(bytes, ENCODING);
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