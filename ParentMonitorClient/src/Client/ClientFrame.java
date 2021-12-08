package Client;

import static Client.Network.CLIENT_EXITED;
import static Client.Network.CLOSE_CLIENT;
import static Client.Network.ENCODING;
import static Client.Network.IMAGE_BUFFER_SIZE;
import static Client.Network.IMAGE_PORT;
import static Client.Network.PNG;
import static Client.Network.PUNISH;
import static Client.Network.SECURITY_KEY;
//import static Client.Network.SHA_1;
import static Client.Network.TEXT_PORT;
import Util.StreamCloser;
import java.awt.AWTException;
import java.awt.Adjustable;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

//The person being "spied on" waits for the parent to connect to it
public class ClientFrame extends JFrame implements Runnable {

    public static final Rectangle SCREEN_BOUNDS = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();

    private Robot screenCapturer;
    
    //stream variables
    private ServerSocket textServer;
    private Socket textConnection;
    private BufferedReader textInput;
    private PrintWriter textOutput;
    
    private ImageSenderWorkerThread worker;
    
    private ImageIcon icon;
    
    private JEditorPane editorPane;
    private JScrollPane scrollPane;
    private JTextField textField;
    private JButton button;
    
    //Initialize components first, then streams
    @SuppressWarnings({"Convert2Lambda", "CallToThreadStartDuringObjectConstruction"})
    public ClientFrame() {
        final Robot screenCapturerReference;
        try {
            screenCapturerReference = new Robot();
        }
        catch (AWTException ex) {
            ex.printStackTrace();
            return;
        }

        final ServerSocket textServerReference;
        try {
            textServerReference = new ServerSocket(TEXT_PORT);
        }
        catch (IOException ex) {
            ex.printStackTrace();
            return;
        }

        /*
         * The application will fully close when the shutdown hook is run.
         * This is useful for debugging purposes.
         */
        try {
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    System.out.println("System Shutdown Detected!");
                }
            });
        }
        catch (IllegalStateException | SecurityException ex) {
            ex.printStackTrace();
        }
        
        //Set title of the frame
        super.setTitle("Parent Monitor - Client");

        //Attempt to load the icon image
        final BufferedImage iconImage = loadIconImage();
        final ImageIcon iconReference;
        if (iconImage != null) {
            iconReference = new ImageIcon(iconImage);
            super.setIconImage(iconImage);
        }
        else {
            iconReference = null;
        }

        //Load the top part of the frame
        {
            final JMenuItem connectionInformation = new JMenuItem("View Address");
            connectionInformation.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent event) {
                    connectionInformation.setArmed(true);
                    connectionInformation.repaint();
                }

                @Override
                public void mouseExited(MouseEvent event) {
                    connectionInformation.setArmed(false);
                    connectionInformation.repaint();
                }
            });
            connectionInformation.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    String hostName;

                    try {
                        hostName = InetAddress.getLocalHost().getHostName();
                    }
                    catch (UnknownHostException ex) {
                        hostName = "Unresolved";
                    }

                    Set<String> networkAddresses = getLocalIPAddresses();
                    StringBuilder message = new StringBuilder("The following may be used by a server to connect to you via LAN:\n");

                    switch (networkAddresses.size()) {
                        case 0: {
                            message.append("IPv4 Address: Unresolved");
                            break;
                        }
                        case 1: {
                            message.append("IPv4 Address: ").append(networkAddresses.iterator().next());
                            break;
                        }
                        default: {
                            message.append("IPv4 Addresses:\n");
                            //technically, we don't need to check hasNext in the for loop
                            //we can check it in the loop body
                            for (Iterator<String> it = networkAddresses.iterator();;) {
                                message.append("- ").append(it.next());
                                if (it.hasNext()) {
                                    message.append("\n");
                                }
                                else {
                                    break;
                                }
                            }
                        }
                    }

                    JOptionPane.showMessageDialog(ClientFrame.this, message.toString() + "\nDevice Name: " + hostName, "Connection Information", JOptionPane.INFORMATION_MESSAGE, iconReference);
                }
            });

            final JMenuBar menuBar = new JMenuBar();
            menuBar.add(connectionInformation);
            super.setJMenuBar(menuBar);
        }
        
        final JEditorPane editorPaneReference = new JEditorPane();
        editorPaneReference.setEditable(false);
        
        final JScrollPane scrollPaneReference = new JScrollPane();
        scrollPaneReference.setViewportView(editorPaneReference);

        final JTextField textFieldReference = new JTextField("Waiting for a server to connect...");
        textFieldReference.setEditable(false);
        textFieldReference.setToolTipText("Enter Message");
        textFieldReference.addFocusListener(new FocusListener() {

            private boolean beenFocused = false;

            @Override
            public void focusGained(FocusEvent event) {
                if (textFieldReference.isEditable()) {
                    if (!beenFocused) {
                        textFieldReference.setText("");
                    }
                    else {
                        beenFocused = true;
                    }
                }
            }

            @Override
            public void focusLost(FocusEvent event) {

            }
        });
        textFieldReference.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent event) {

            }

            @Override
            public void keyPressed(KeyEvent event) {
                PrintWriter textOutputReference = textOutput;
                if (textOutputReference != null) {
                    if (event.getKeyCode() == KeyEvent.VK_ENTER) {
                        String message = textFieldReference.getText().trim();
                        textOutputReference.println(message); //send message to parent
                        message = "You: " + message;
                        textFieldReference.setText("");
                        String previousText = editorPaneReference.getText();
                        editorPaneReference.setText(previousText.isEmpty() ? message : previousText + "\n" + message);
                        scrollToBottom(scrollPaneReference);
                    }
                }
            }

            @Override
            public void keyReleased(KeyEvent event) {

            }
        });

        final JButton buttonReference = new JButton();
        buttonReference.setText("Send Message");
        buttonReference.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                PrintWriter textOutputReference = textOutput;
                if (textOutputReference != null) {
                    String message = textFieldReference.getText().trim();
                    textOutputReference.println(message); //send message to parent
                    message = "You: " + message;
                    textFieldReference.setText("");
                    String previousText = editorPaneReference.getText();
                    editorPaneReference.setText(previousText.isEmpty() ? message : previousText + "\n" + message);
                    scrollToBottom(scrollPaneReference);
                }
                else {
                    JOptionPane.showMessageDialog(ClientFrame.this, "Error: Cannot send messages, no server has connected with you yet.", "Not Connected", JOptionPane.ERROR_MESSAGE, iconReference);
                }
            }
        });

        //Insert all components in proper locations
        {
            final GridBagLayout layout = new GridBagLayout();
            layout.columnWidths = new int[]{10, 0, 65, 5, 0};
            layout.rowHeights = new int[]{10, 0, 30, 5, 0};
            layout.columnWeights = new double[]{0.0, 1.0, 0.0, 0.0, 1.0E-4};
            layout.rowWeights = new double[]{0.0, 1.0, 0.0, 0.0, 1.0E-4};

            Container contentPane = super.getContentPane();
            contentPane.setLayout(layout);

            contentPane.add(scrollPaneReference, new GridBagConstraints(1, 1, 2, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 5, 5), 0, 0));

            contentPane.add(textFieldReference, new GridBagConstraints(1, 2, 2, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 5, 5), 0, 0));

            contentPane.add(buttonReference, new GridBagConstraints(2, 3, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 5, 5), 0, 0));
        }

        //Set frame dimensions and display frame
        {
            final int width = SCREEN_BOUNDS.width / 2;
            final int height = SCREEN_BOUNDS.height;
            final Dimension frameArea = new Dimension(width, height);
            super.setSize(frameArea);
            super.setPreferredSize(frameArea);
            super.setMinimumSize(frameArea);
            super.setMaximumSize(frameArea);
            super.setLocation((SCREEN_BOUNDS.width / 2) - (SCREEN_BOUNDS.width / 4), (SCREEN_BOUNDS.height / 2) - (height / 2));
            
            super.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            super.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent event) {
                    PrintWriter textOutputReference = textOutput;
                    if (JOptionPane.showConfirmDialog(ClientFrame.this,
                            "Are you sure you want to exit?", "Exit?",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE, iconReference) == JOptionPane.YES_OPTION) {
                        //notify parent
                        if (textOutputReference != null) {
                            textOutputReference.println(CLIENT_EXITED);
                        }
                        dispose();
                    }
                    else {
                        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                    }
                }
            });
            super.setVisible(true);
        }
        
        screenCapturer = screenCapturerReference;
        
        textServer = textServerReference;
        
        icon = iconReference;
        
        editorPane = editorPaneReference;
        scrollPane = scrollPaneReference;
        textField = textFieldReference;
        button = buttonReference;

        new Thread(this, "Server Listener Thread").start();
        (worker = new ImageSenderWorkerThread(IMAGE_PORT)).start();
    }

    @Override
    public void dispose() {
        //load all instance variables first
        final ServerSocket textServerReference = textServer;
        final Socket textConnectionReference = textConnection;
        final BufferedReader textInputReference = textInput;
        final PrintWriter textOutputReference = textOutput;
        
        final ImageSenderWorkerThread workerReference = worker;
        
        final JEditorPane editorPaneReference = editorPane;
        final JScrollPane scrollPaneReference = scrollPane;
        final JTextField textFieldReference = textField;
        final JButton buttonReference = button;
        
        if (!super.isEnabled()) {
            System.out.println("Frame already disposed.");
            return;
        }
        
        //Destroy frame resources
        super.setEnabled(false);
        super.setVisible(false);
        super.dispose(); //Destroy the frame
        super.getContentPane().removeAll(); //Remove all sub-components
        
        //Close connections
        StreamCloser.close(textServerReference);
        StreamCloser.close(textConnectionReference);
        StreamCloser.close(textInputReference);
        StreamCloser.close(textOutputReference);
        
        //Close worker thread
        StreamCloser.close(workerReference);
        
        screenCapturer = null;
        
        textServer = null;
        textConnection = null;
        textInput = null;
        textOutput = null;
        
        worker = null;

        icon = null;
        
        if (editorPaneReference != null) {
            editorPaneReference.setEnabled(false);
            editorPaneReference.removeAll();
            editorPane = null;
        }
        
        if (scrollPaneReference != null) {
            scrollPaneReference.setEnabled(false);
            scrollPaneReference.removeAll();
            scrollPane = null;
        }

        if (textFieldReference != null) {
            textFieldReference.setEnabled(false);
            textFieldReference.removeAll();
            textField = null;
        }
        
        if (buttonReference != null) {  
            buttonReference.setEnabled(false);
            buttonReference.removeAll();
            button = null;
        }
        
        System.out.println("Frame disposal complete.");
    }

    private class ImageSenderWorkerThread extends Thread implements Closeable {

        private ServerSocket screenshotServer;
        private Socket screenshotConnection;
        private DataOutputStream screenshotSender;

        private ImageSenderWorkerThread(int port) {
            super("Image Sender Worker Thread");
            try {
                screenshotServer = new ServerSocket(port);
            }
            catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        @Override
        public final void run() {
            //Wait until a stable connection has been found
            ServerSocket screenshotServerReference = screenshotServer;

            if (screenshotServerReference == null) {
                dispose();
                System.out.println(getName() + " Exiting.");
                return;
            }

            Socket screenshotConnectionReference;

            while (true) {
                Socket textConnectionReference = textConnection; //update reference every iteration
                if (screenshotServerReference.isClosed()) {
                    //No streams have been setup
                    dispose();
                    System.out.println(getName() + " Exiting.");
                    return;
                }
                if (textConnectionReference != null && !textConnectionReference.isClosed()) {
                    System.out.println("Text Socket connected succesfully, awaiting Image Socket connection.");
                    try {
                        Socket screenshotConnectionTest = screenshotServerReference.accept();
                        
                        //should not be null!!!
                        InetAddress remoteTextSocketAddress = textConnectionReference.getInetAddress();
                        InetAddress remoteImageSocketAddress = screenshotConnectionTest.getInetAddress();
                    
                        //sanity check!!!
                        if (remoteTextSocketAddress == null) { 
                            StreamCloser.close(screenshotConnectionTest);
                            dispose();
                            System.out.println("Failed to retrieve Text Socket remote address.");
                            System.out.println(getName() + " Exiting.");
                            return;
                        }
                        
                        if (remoteImageSocketAddress == null) {
                            StreamCloser.close(screenshotConnectionTest);
                            System.out.println("Failed to retrieve Image Socket remote address.");
                            continue;
                        }
                        
                        //ensure both sockets are connected to the same server!!!
                        if (remoteTextSocketAddress.equals(remoteImageSocketAddress)) {
                            screenshotConnectionReference = screenshotConnectionTest;
                            System.out.println("Image Socket connected succesfully.");
                            break;
                        }
                        else {
                            StreamCloser.close(screenshotConnectionTest);
                            System.out.println("Warning: Text Socket Address: " + remoteTextSocketAddress.getHostAddress() + " does not match with Image Socket Address: " + remoteImageSocketAddress.getHostAddress());
                        }
                    }
                    catch (IOException ex) {
                        //Wait for a good connection...
                        ex.printStackTrace();
                    }
                }
            }
            
            DataOutputStream screenshotSenderReference;

            try {
                screenshotSenderReference = new DataOutputStream(new BufferedOutputStream(screenshotConnectionReference.getOutputStream(), IMAGE_BUFFER_SIZE));
            }
            catch (IOException ex) {
                StreamCloser.close(screenshotServerReference);
                StreamCloser.close(screenshotConnectionReference);
                dispose();
                ex.printStackTrace();
                System.out.println(getName() + " Exiting.");
                return;
            }
            
            screenshotConnection = screenshotConnectionReference;
            screenshotSender = screenshotSenderReference;
            
            //Use local variables as much as possible here, performance critical!!!
            final Robot screenCapturerReference = screenCapturer;
            final Rectangle deviceScreenSize = SCREEN_BOUNDS;
            final String screenshotImageFormat = PNG;

            //Technically, the server no longer "requests" for an image, its always
            //demands for it, and we always send, except when the server is dealing with
            //multiple clients, clients that are repainted do not update screens
            for (ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream(IMAGE_BUFFER_SIZE); screenshotSender != null; byteBuffer.reset()) {
                try {
                    ImageIO.write(screenCapturerReference.createScreenCapture(deviceScreenSize), screenshotImageFormat, byteBuffer);
                    screenshotSenderReference.writeInt(byteBuffer.size());
                    byteBuffer.writeTo(screenshotSenderReference); //write directly to the output stream, no slow copy
                    screenshotSenderReference.flush();
                }
                catch (IOException | NullPointerException | IllegalArgumentException ex) {
                    ex.printStackTrace();
                    break;
                }
            }

            close();
            System.out.println(getName() + " Exiting.");
        }

        @Override
        public final void close() {
            ServerSocket screenshotServerReference = screenshotServer;
            Socket screenshotConnectionReference = screenshotConnection;
            DataOutputStream screenshotSenderReference = screenshotSender;
            
            StreamCloser.close(screenshotServerReference);
            StreamCloser.close(screenshotConnectionReference);
            StreamCloser.close(screenshotSenderReference);

            screenshotServer = null;
            screenshotConnection = null;
            screenshotSender = null;
        }
    }
    
    @Override
    public final void run() {
        final ServerSocket textServerReference = textServer;
        final JEditorPane editorPaneReference = editorPane;
        final JScrollPane scrollPaneReference = scrollPane;
        final JTextField textFieldReference = textField;

        if (textServerReference == null) {
            dispose();
            System.out.println("Closing without connection."); //Happens when a client closes without a connection
            System.out.println("Server Listener Thread Exiting.");
            return;
        }

        /**
         * Warning, if the local machine's IP address changes, the primary
         * ServerSocket used for text communication will be disabled, rendering
         * the generated security key invalid. Additionally, the ServerSocket
         * used for screen shot sending will be disabled as the underlying
         * address changes. The user should restart the application should their
         * machine's IP address change, such as switching networks or being
         * disconnected from a network.
         */
        final MessageEncoder security = new MessageEncoder(SECURITY_KEY, "AES");

        //InetAddress localDeviceNetworkAddress = InetAddress.getLocalHost();
        //byte[] securityKey = localDeviceNetworkAddress.getHostAddress().getBytes(ENCODING);
        //securityKey = SHA_1.digest(securityKey);
        //securityKey = Arrays.copyOf(securityKey, 16); // use only first 128 bits
        //security = new MessageEncoder(SECURITY_KEY, "AES");

        final Charset encoding = ENCODING;
        final BufferedReader textInputReference;
        final PrintWriter textOutputReference;

        //Note: We wait for the parent to connect to us, so use only 1 connection.

        //Loop until all streams have been properly set up.
        //We do not support reconnecting, once server has told client to shutdown, we do so.
        while (true) {
            if (textServerReference.isClosed()) {
                dispose();
                System.out.println("Closing without connection."); //Happens when a client closes without a connection
                System.out.println("Server Listener Thread Exiting.");
                return;
            }

            final Socket textConnectionTest;
            final BufferedReader textInputTest;
            final PrintWriter textOutputTest;

            try {
                textConnectionTest = textServerReference.accept();
            }
            catch (IOException ex) {
                ex.printStackTrace();
                continue;
            }

            try {
                textInputTest = new BufferedReader(new InputStreamReader(textConnectionTest.getInputStream(), encoding)) {
                    @Override
                    public String readLine() throws IOException {
                        String line = super.readLine();
                        return line == null ? null : security.decode(line);
                    }
                };
            }
            catch (IOException ex) {
                StreamCloser.close(textConnectionTest);
                ex.printStackTrace();
                continue;
            }

            try {
                textOutputTest = new PrintWriter(new BufferedWriter(new OutputStreamWriter(textConnectionTest.getOutputStream(), encoding)), true) {
                    @Override
                    public void println(String line) {
                        super.println(security.encode(line));
                    }
                };
            }
            catch (IOException ex) {
                StreamCloser.close(textConnectionTest);
                StreamCloser.close(textInputTest);
                ex.printStackTrace();
                continue;
            }

            //All streams have been properly set up, so initialize here 
            textConnection = textConnectionTest;
            textInput = textInputReference = textInputTest;
            textOutput = textOutputReference = textOutputTest;

            break;
        }

        {
            //Once streams have been set up
            //Send Infomation to server immediately for validation
            StringBuilder buffer = new StringBuilder(2000);

            for (Iterator<Map.Entry<String, String>> it = System.getenv().entrySet().iterator(); it.hasNext();) {
                Map.Entry<String, String> entry = it.next();
                buffer.append(Network.encode(entry.getKey())).append("->").append(Network.encode(entry.getValue()));
                if (it.hasNext()) {
                    buffer.append("|");
                }
                else {
                    break;
                }
            }

            //send client infomation to server
            textOutputReference.println(buffer.toString());
            buffer.setLength(0); //clear the buffer
        }

        //after all infomation has been forwarded, enable chatting
        textFieldReference.setText("Enter Message...");
        textFieldReference.setEditable(true);

        boolean shutdown = false;

        SERVER_TEXT_READER_LOOP:
        while (textInput != null) {
            try {
                String textFromServer = textInputReference.readLine();
                //server request that we close
                if (textFromServer == null) {
                    break;
                }
                switch (textFromServer) {
                    case CLOSE_CLIENT: {
                        System.out.println(CLOSE_CLIENT);
                        if (super.isVisible()) {
                            JOptionPane.showMessageDialog(ClientFrame.this, "The server has disconnected you.", "System Closing", JOptionPane.WARNING_MESSAGE, icon);
                        }
                        else {
                            System.out.println("Server disconnect dialog should not be displayed, frame is disposed already.");
                        }
                        //This message is slightly misleading when server is exiting normally
                        break SERVER_TEXT_READER_LOOP;
                    }
                    case PUNISH: {
                        shutdown = true;
                        System.out.println(PUNISH);
                        break SERVER_TEXT_READER_LOOP;
                    }
                    default: {
                        String previousText = editorPaneReference.getText();
                        editorPaneReference.setText(previousText.isEmpty() ? "Server: " + textFromServer : previousText + "\nServer: " + textFromServer);
                        scrollToBottom(scrollPaneReference);
                    }
                }
            }
            catch (IOException ex) {
                ex.printStackTrace();
                if (super.isVisible()) {
                    JOptionPane.showMessageDialog(ClientFrame.this, "The server has shutdown.", "System Closing", JOptionPane.WARNING_MESSAGE, icon);
                }
                else {
                    System.out.println("Server shutdown dialog should not be displayed, frame is disposed already.");
                }
                break;
            }
        }

        dispose();
        System.out.println("Server Listener Thread Exiting.");
        
        if (shutdown) {
            System.out.println("Server has punished you!");
            shutdown();
        }
    }
    
    private static BufferedImage loadIconImage() {
        try {
            return ImageIO.read(ClientFrame.class.getResourceAsStream("/Images/Eye.jpg"));
        }
        catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }
    
    //Code from: https://stackoverflow.com/questions/5147768/scroll-jscrollpane-to-bottom
    private static void scrollToBottom(JScrollPane scrollPane) {
        if (scrollPane == null) {
            return;
        }
        JScrollBar verticalBar = scrollPane.getVerticalScrollBar();
        verticalBar.addAdjustmentListener(new AdjustmentListener() {
            @Override
            public void adjustmentValueChanged(AdjustmentEvent event) {
                Adjustable adjustable = event.getAdjustable();
                adjustable.setValue(adjustable.getMaximum());
                verticalBar.removeAdjustmentListener(this);
            }
        });
    }

    private static void shutdown() {
        try {
            String operatingSystem = System.getProperty("os.name");
            if (operatingSystem != null) {
                if (operatingSystem.contains("Linux") || operatingSystem.contains("Mac OS X")) {
                    Runtime.getRuntime().exec("shutdown -h now");
                }
                else if (operatingSystem.contains("Windows")) {
                    Runtime.getRuntime().exec("shutdown.exe -s -t 0");
                }
            }
        }
        catch (SecurityException | IOException ex) {
            ex.printStackTrace();
        }
    }

    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public static void main(String[] args) {
        ImageIO.setUseCache(false);
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
            ex.printStackTrace();
        }
        new ClientFrame();
    }
    
    //https://stackoverflow.com/questions/8083479/java-getting-my-ip-address
    private static Set<String> getLocalIPAddresses() {
        final Enumeration<NetworkInterface> networkInterfaces;

        try {
            networkInterfaces = NetworkInterface.getNetworkInterfaces();
        }
        catch (SocketException ex) {
            ex.printStackTrace();
            return Collections.emptySet();
        }
        
        if (networkInterfaces == null) {
            return Collections.emptySet();
        }

        final TreeSet<String> addressList = new TreeSet<>();
            
        while (networkInterfaces.hasMoreElements()) {
            final NetworkInterface networkInterface = networkInterfaces.nextElement();
            if (networkInterface == null) {
                continue;
            }
            try {
                // filters out 127.0.0.1 and inactive networkInterfaces
                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue;
                }

                final Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                if (addresses == null) {
                    continue;
                }
                while (addresses.hasMoreElements()) {
                    final InetAddress address = addresses.nextElement();
                    if (address instanceof Inet4Address) {
                        if (!address.isLoopbackAddress()) {
                            addressList.add(address.getHostAddress());
                        }
                    }
                }
            }
            catch (SocketException ex) {
                ex.printStackTrace();
            }
        }

        return addressList;
    }
}