package Server;

import static Server.Network.IMAGE_BUFFER_SIZE;
import Util.StreamCloser;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import javax.imageio.ImageIO;

public final class ImageSocket implements Closeable {
    
    private Socket socket;
    private DataInputStream receiveImage;
    private ReusableByteArrayInputStream byteBuffer;
    
    public ImageSocket(String host, int port) {
        Socket connection = new Socket();
        
        try {
            connection.setReuseAddress(true);
            connection.setSoTimeout(5000); //Only wait 5 seconds for images to be sent, otherwise terminate connection
            connection.connect(new InetSocketAddress(host, port), 100);
        }
        catch (IOException ex) {
            StreamCloser.close(connection);
            ex.printStackTrace();
            return; //there's no point in continuing initialization
        }
        
        DataInputStream screenshotStream;
       
        try {
            screenshotStream = new DataInputStream(new BufferedInputStream(connection.getInputStream(), IMAGE_BUFFER_SIZE));
        }
        catch (IOException ex) {
            ex.printStackTrace();
            StreamCloser.close(connection);
            return;
        }
        
        socket = connection;
        receiveImage = screenshotStream;
        byteBuffer = new ReusableByteArrayInputStream();
    }

    public boolean isActive() {
        Socket socketReference = socket;
        DataInputStream recieveImageReference = receiveImage;
        return (socketReference == null || recieveImageReference == null) ? false : !socketReference.isClosed();
    }

    @Override
    public void close() {
        //load instance variables first
        Socket socketReference = socket;
        DataInputStream receiveImageReference = receiveImage;
        ReusableByteArrayInputStream byteStreamReference = byteBuffer;
        //close local references at the same time
        StreamCloser.close(socketReference);
        StreamCloser.close(receiveImageReference);
        StreamCloser.close(byteStreamReference);
        //dispose of instance variables
        socket = null;
        receiveImage = null;
        byteBuffer = null;
    }

    /**
     * Receive
     * @return
     * @throws IOException 
     */
    public BufferedImage readImage() throws IOException {
        DataInputStream receiveImageReference = receiveImage; //avoid getfield opcode
        ReusableByteArrayInputStream byteBufferReference = byteBuffer; //avoid getfield opcode
        int bytesRead = receiveImageReference.readInt();
        byte[] buffer = new byte[bytesRead];
        receiveImageReference.readFully(buffer, 0, bytesRead);
        byteBufferReference.setBuffer(buffer, bytesRead);
        return ImageIO.read(byteBufferReference);
    }

    @Override
    public String toString() {
        return socket == null ? "Not Connected" : socket.toString();
    }

    private static final class ReusableByteArrayInputStream extends ByteArrayInputStream implements Recyclable {

        //shared for all instances
        private static final byte[] EMPTY = {};

        public ReusableByteArrayInputStream() {
            super(EMPTY);
        }

        private void setBuffer(byte[] buffer, int length) {
            buf = buffer;
            pos = mark = 0;
            count = length;
        }

        @Override
        public void recycle() {
            count = 0;
            buf = EMPTY;
        }
        
        @Override
        public void close() {
            count = 0;
            buf = EMPTY;
        }
    }
}