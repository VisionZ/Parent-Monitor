package Server;

import static Server.Network.CLIENT_EXITED;
import static Server.Network.CLOSE_CLIENT;
//import static Server.Network.ENCODING;
import static Server.Network.PUNISH;
import static Server.Network.SECURITY_KEY;
//import static Server.Network.SHA_1;
import static Server.ServerFrame.SCREEN_BOUNDS;
import Util.MessageEncoder;
import Util.StreamCloser;
import Util.ThreadSafeBoolean;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.io.IOException;
//import java.util.Arrays;
import java.util.Date;
import java.util.regex.Pattern;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;

public final class ParentPanel extends JPanel implements Runnable {

    //thread control
    private final ThreadSafeBoolean terminated;

    //reference to ServerFrame's tabs, so we can remove ourselves from the
    //tabs when necessary
    private JTabbedPane parentTabs;
    private TextFrame parentConnectionHistory;
    //do not dispose this here, ServerFrame must take care of this to ensure proper closing
    //of the Server application, all threads and frames must be closed for our application to exit
    //without System.exit()

    //stream variable
    private TextSocket textConnection;

    private JSplitPane split;
    private ClientPanel client;
    private TextPanel text;

    //info variables
    private String clientName;
    private TextFrame clientInfoFrame; //Must be disposed!

    private static final String DATA_DELIMITER = Pattern.quote("|");
    private static final String PAIR_DELIMITER = Pattern.quote("->");

    @SuppressWarnings("CallToThreadStartDuringObjectConstruction")
    public ParentPanel(ServerFrame parent, TextSocket clientTextConnection, ImageSocket clientImageConnection) throws IOException {
        //MUST PERFORM INITIAL READ
        final StringBuilder clientData;
        String username = "Unknown";
       
        String remoteAddress = clientTextConnection.getAddress();
        System.out.println("Remote Address: " + remoteAddress);
        //byte[] securityKey = remoteAddress.getBytes(ENCODING);
        //securityKey = SHA_1.digest(securityKey);
        //securityKey = Arrays.copyOf(securityKey, 16); // use only first 128 bits

        MessageEncoder security = new MessageEncoder(SECURITY_KEY, "AES");
        if (security.isValid()) {
            clientTextConnection.setEncoder(security);
            System.out.println("Valid security key created for " + remoteAddress + ".");
        }
        else {
            System.out.println("Error: Could not create valid security key for " + remoteAddress + ".");
        }
        
        try {
            //contains all client data
            //Device Name
            //Device OS
            //Device User Name
            //Device SystemEnv
            String[] data = clientTextConnection.readText().split(DATA_DELIMITER);
            //after waiting for 5 seconds for the data to be read through, we want to allow an
            //infinite wait time for data to be read through, or else this socket will screw up
            clientTextConnection.setReadWaitTime(0);
            System.out.println("Reading System Data from: " + clientTextConnection.toString());
            clientData = new StringBuilder();
            final int lastIndex = data.length - 1;
            if (lastIndex >= 0) {
                final String pairDelimiter = PAIR_DELIMITER;
                for (int index = 0; index < lastIndex; ++index) {
                    String[] entry = data[index].split(pairDelimiter);
                    switch (entry.length) {
                        case 2: {
                            String key = Network.decode(entry[0]);
                            String value = Network.decode(entry[1]);
                            if ("USERNAME".equals(key)) {
                                username = value;
                            }
                            clientData.append(key).append(" -> ").append(value).append("\n");
                            break;
                        }
                        case 1: {
                            clientData.append(Network.decode(entry[0])).append(" -> Unresolved").append("\n");
                            break;
                        }
                    }
                }
                {
                    String[] entry = data[lastIndex].split(pairDelimiter);
                    switch (entry.length) {
                        case 2: {
                            String key = Network.decode(entry[0]);
                            String value = Network.decode(entry[1]);
                            if ("USERNAME".equals(key)) {
                                username = value;
                            }
                            clientData.append(key).append(" -> ").append(value);
                            break;
                        }
                        case 1: {
                            clientData.append(Network.decode(entry[0])).append(" -> Unresolved");
                            break;
                        }
                    }
                }
            }
        }
        catch (IOException ex) {
            //clean up used resources only
            StreamCloser.close(clientTextConnection);
            StreamCloser.close(clientImageConnection);
            ex.printStackTrace();
            throw ex;
        }

        terminated = new ThreadSafeBoolean(false);

        parentTabs = parent.getTabs();
        parentConnectionHistory = parent.getConnectionHistoryFrame();

        //setup SplitPanel with: ClientPanel & TextPanel with cheeky initialization
        split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                client = new ClientPanel(parent, clientName = username, clientImageConnection),
                text = new TextPanel(textConnection = clientTextConnection));
        split.setDividerLocation(SCREEN_BOUNDS.width / 2);

        //add components
        super.setLayout(new GridLayout(1, 1));
        super.add(split);

        //add other stuff
        super.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        super.setToolTipText("Client Username: " + username);
        
        String info = clientData.toString();
        System.out.println(info);
        clientInfoFrame = new TextFrame(parent, parent.getIconImage(), username + " System Information", info, true);
        new Thread(this, username + " Main Client Manager Thread").start();
    }

    public void setSelected(boolean current) {
        client.setRepaint(current); //To improve performance, only update the live feed it its visible
    }

    public JSplitPane getSplitPane() {
        return split;
    }

    @Override
    public String getName() {
        return clientName;
    }

    @Override
    public Component[] getComponents() {
        return new Component[]{client, text};
    }

    public void saveCurrentShot(ImageBank bank, ScreenShotDisplayer master) {
        client.saveCurrentShot(bank, master);
    }

    /*
    public void toggleUpdate() {
        client.toggleUpdate();
    }
     */
    
    public boolean takenScreenShot() {
        return client.takenScreenShot();
    }

    public void showSavedScreenShots() {
        client.showScreenShotDisplayer();
    }

    public void showInfo() {
        clientInfoFrame.setVisible(true);
    }

    public synchronized void close(boolean serverClosedClient) {
        if (terminated.get()) { //Lock
            return;
        }

        parentTabs.remove(this);
        parentTabs = null;

        if (serverClosedClient) { //indicates wheather the server intentionally closed the client
            textConnection.sendText(CLOSE_CLIENT); //Inform client server has disconnected them
            parentConnectionHistory.addText(clientName + " disconnected by Server: " + new Date());
        }
        else {
            parentConnectionHistory.addText(clientName + " disconnected from Server: " + new Date());
        }

        //DO NOT DISPOSE HISTORY, IT IS A REFERENCE TO THE SERVER
        //SERVER WILL HANDLE IT
        parentConnectionHistory = null;

        StreamCloser.close(textConnection);
        textConnection = null;

        split.removeAll();
        split = null;

        //The Image Retriever Thread will close first, then the Manager Thread
        //will exit after this method has finished execution. Since the Render thread sleeps often
        //it is likely to be the last one to stop
        client.close();
        client = null;

        text.setEnabled(false);
        text.setVisible(false);
        text.removeAll();
        text = null;

        clientName = null;

        //Dispose all frames 
        clientInfoFrame.dispose();
        clientInfoFrame = null;

        terminated.set(true); //Unlock at the very end, to prevent many threads from missing things
        System.gc();
    }

    public synchronized void punish() {
        if (terminated.get()) { //Lock
            return;
        }

        parentTabs.remove(this);
        parentTabs = null;

        textConnection.sendText(PUNISH); //Inform client server has PUNISHED them
        parentConnectionHistory.addText(clientName + " shutdown by Server: " + new Date());

        //DO NOT DISPOSE HISTORY, IT IS A REFERENCE TO THE SERVER
        //SERVER WILL HANDLE IT
        parentConnectionHistory = null;

        StreamCloser.close(textConnection);
        textConnection = null;

        split.removeAll();
        split = null;

        //The Image Retriever Thread will close first, then the Manager Thread
        //will exit after this method has finished execution. Since the Render thread sleeps often
        //it is likely to be the last one to stop
        client.close();
        client = null;

        text.setEnabled(false);
        text.setVisible(false);
        text.removeAll();
        text = null;

        clientName = null;

        //Dispose all frames 
        clientInfoFrame.dispose();
        clientInfoFrame = null;

        terminated.set(true); //Unlock at the very end, to prevent many threads from missing things
        System.gc();
    }

    @Override
    public final void run() {
        TextSocket textStream = textConnection; //avoid getfield opcode
        while (!terminated.get()) {
            try {
                String fromClient = textStream.readText();
                if (CLIENT_EXITED.equals(fromClient)) {
                    close(false);
                    break;
                }
                else {
                    text.updateChatPanel(clientName, fromClient);
                }
            }
            catch (IOException ex) {
                close(false);
                System.err.println("Failed to recieve message from client.");
                ex.printStackTrace();
                //If the client has been forcibly terminated on their end
                //without sending the final exit message, such as from manual
                //shutdown, we must take care to destroy the client on this end as well
                break;
            }
        }
        System.out.println("Manager Exiting. Client Name Should Be Set to Null: " + clientName);
        //clientName should be set to null here, since close() has been called
    }

    /**
     * SOLVED ALREADY, was previously in Thread Loop. BIG BUG AHEAD!!! * BUG: If
     * the server requests an image from the client and suddenly stops
     * requesting, the last message will be sent as "Server Request: Screenshot"
     *
     * This causes the BufferedReader readLine() below to hold until the client
     * texts back in a conversation message or a closing notification. This
     * means that until the client communicates back, and the updateScreenShot
     * flag has been set to false, this method (and Thread) will be frozen!!!
     * Yet we still have to use readLine() for other client activities, so we
     * may have to resort to using a thread safe stack here...
     *
     * Fortunately there are no other significant problems save this one.
     *
     * SOLUTIONS: - A stupid workaround would be to instead send a message to
     * the client that the server is not requesting screenshots, and the client
     * would promptly respond back, preventing a holdup here.
     *
     * A side effect of this naive solution is that this application may take up
     * unnecessary network operation space
     *
     * For now we will use the naive solution, since I'd rather not make a
     * concurrent stack and keep accessing it repeatedly.
     *
     * UPDATE: The naive solution works, but is unstable if the host and the
     * client are constantly chatting, which clogs up the BufferedReader
     *
     * Therefore, we will implement a 2-Socket solution. This has been done.
     */
    //The following line of code is based off the naive solution
    //sendText.println(updateScreenShot.get() ? REQUEST_IMAGE : STOP_REQUEST_IMAGE);
}