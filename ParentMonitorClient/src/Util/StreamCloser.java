package Util;

import java.io.Closeable;
import java.io.IOException;

public final class StreamCloser {

    private StreamCloser() {

    }

    /**
     * Silently closes a stream.
     *
     * @param stream The stream to be closed.
     */
    public static final void close(Closeable stream) {
        if (stream != null) {
            try {
                stream.close();
            }
            catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Closes any amount of streams, also clears the input array of all
     * references.
     *
     * @param streams The streams to be closed
     */
    public static final void closeMultiple(Closeable... streams) {
        for (int index = streams.length - 1; index >= 0; --index) {
            Closeable stream = streams[index];
            if (stream != null) {
                try {
                    stream.close();
                }
                catch (IOException ex) {
                    ex.printStackTrace();
                }
                streams[index] = null;
            }
        }
    }
}