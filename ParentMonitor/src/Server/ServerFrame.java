package Server;

import static Server.Network.IMAGE_PORT;
import static Server.Network.TEXT_PORT;
import Util.Quotes;
import Util.ThreadSafeBoolean;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class ServerFrame extends JFrame {
    
    public static final GraphicsEnvironment GRAPHICS_ENVIRONMENT = GraphicsEnvironment.getLocalGraphicsEnvironment();
    public static final GraphicsConfiguration GRAPHICS_CONFIGURATION = GRAPHICS_ENVIRONMENT.getDefaultScreenDevice().getDefaultConfiguration();
    public static final Rectangle SCREEN_BOUNDS = GRAPHICS_ENVIRONMENT.getMaximumWindowBounds();

    private JPopupMenu popup;
    private JMenuBar menuBar;
    private JTabbedPane tabs;

    private ImageIcon icon = new ImageIcon();
    
    //These frames must be disposed when closing!
    private TextFrame connectionHistory; 
    private ScreenShotDisplayer master;

    private ParentPanel selected;
    
    private ImageBank bank = new ImageBank();

    @SuppressWarnings("Convert2Lambda")
    public ServerFrame() {
        super("Parent Monitor - Server");
        super.setLayout(new BorderLayout());

        final int width = SCREEN_BOUNDS.width; //was once half screen size
        final int height = SCREEN_BOUNDS.height;
        final Dimension frameArea = new Dimension(width, height);

        super.setSize(frameArea);
        super.setPreferredSize(frameArea);
        super.setMinimumSize(frameArea);
        super.setMaximumSize(frameArea);
        super.setResizable(false);
        
        //add Popup Menu here
        popup = new JPopupMenu();

        JMenuItem close = new JMenuItem("Disconnect Client");
        JMenuItem saveScreenShot = new JMenuItem("Capture Screenshot");
        JMenuItem showSavedScreenShots = new JMenuItem("Show Captured Screenshots");
        //JMenuItem toggleLiveRefresh = new JMenuItem("Toggle Refresh");
        JMenuItem clientInfo = new JMenuItem("Client System Info (Advanced)");
        JMenuItem punish = new JMenuItem("Shutdown Client");

        //action listener for popups
        @SuppressWarnings("Convert2Lambda")
        ActionListener popupListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                Object source = event.getSource();
                //popup responds to current selected client
                ParentPanel current = selected;
                if (current != null) {
                    if (source == close) {
                        //notify client we are closing their connection
                        String clientName = current.getName();
                        if (JOptionPane.showConfirmDialog(ServerFrame.this,
                                "Are you sure you want to disconnect " + clientName + "?", "Disconnect " + clientName + "?",
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.QUESTION_MESSAGE, icon) == JOptionPane.YES_OPTION) {
                            current.close(true);
                        }
                        //tabs.remove(current); //No need to do this here, terminate takes care of it already
                    }
                    else if (source == saveScreenShot) {
                        current.saveCurrentShot(bank, master);
                    }
                    else if (source == showSavedScreenShots) {
                        if (current.takenScreenShot()) {
                            current.showSavedScreenShots();
                        }
                        else {
                            JOptionPane.showMessageDialog(ServerFrame.this, "Error: There are no captured screenshots from " + current.getName() + " to show.", "Invalid Operation", JOptionPane.ERROR_MESSAGE, icon);
                        } 
                    }
                    /*
                    else if (source == toggleLiveRefresh) {
                        current.toggleUpdate();
                    }
                     */
                    else if (source == clientInfo) {
                        current.showInfo();
                    }
                    else {
                        String clientName = current.getName();
                        if (JOptionPane.showConfirmDialog(ServerFrame.this,
                                "Are you sure you want to shutdown " + clientName + "'s device?", "Shutdown " + clientName + "?",
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.QUESTION_MESSAGE, icon) == JOptionPane.YES_OPTION) {
                            current.punish();
                        }
                    }
                }
            }
        };

        close.addActionListener(popupListener);
        saveScreenShot.addActionListener(popupListener);
        showSavedScreenShots.addActionListener(popupListener);
        //toggleLiveRefresh.addActionListener(popupListener);
        clientInfo.addActionListener(popupListener);
        punish.addActionListener(popupListener);

        close.setHorizontalTextPosition(JMenuItem.RIGHT);
        saveScreenShot.setHorizontalTextPosition(JMenuItem.RIGHT);
        showSavedScreenShots.setHorizontalTextPosition(JMenuItem.RIGHT);
        //toggleLiveRefresh.setHorizontalTextPosition(JMenuItem.RIGHT);
        clientInfo.setHorizontalTextPosition(JMenuItem.RIGHT);
        punish.setHorizontalTextPosition(JMenuItem.RIGHT);
        
        popup.add(close);
        //popup.add(toggleLiveRefresh);
        popup.add(saveScreenShot);
        popup.add(showSavedScreenShots);
        popup.addSeparator();
        popup.add(clientInfo);
        popup.add(punish);

        super.setJMenuBar(menuBar = new JMenuBar());
        
        super.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                if (JOptionPane.showConfirmDialog(ServerFrame.this,
                        "Are you sure you want to exit?\nWarning: All unsaved data will be erased.", "Exit?",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE, icon) == JOptionPane.YES_OPTION) {
                    dispose();
                }
                else {
                    setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                }
            }
        });

        JMenu file = new JMenu("File");
        
        JMenuItem saveImages = new JMenuItem("Save Screenshots");
        saveImages.addActionListener(new ActionListener() {
            //Prevent more than 1 saving operation from happening at the same time
            @Override
            public void actionPerformed(ActionEvent event) {
                if (bank.isEmpty()) {
                    JOptionPane.showMessageDialog(ServerFrame.this, "Error: There are no captured screenshots to save.", "Invalid Operation", JOptionPane.ERROR_MESSAGE, icon);
                    return;
                }
                if (bank.showingSaveDialog()) {
                    System.out.println("Save dialog already displayed!");
                    return;
                }
                JFileChooser save = new JFileChooser();
                save.setDialogType(JFileChooser.SAVE_DIALOG);
                save.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                if (save.showSaveDialog(ServerFrame.this) == JFileChooser.APPROVE_OPTION) {
                    File directoryChosen = save.getSelectedFile();
                    if (directoryChosen.isDirectory()) {
                        bank.writeToFiles(ServerFrame.this, directoryChosen);
                    }
                    else {
                        JOptionPane.showMessageDialog(ServerFrame.this, "Error: " + directoryChosen + " is not a directory!", "Invalid Directory", JOptionPane.ERROR_MESSAGE, icon);
                    }
                }
            }
        });

        /*
        JMenuItem saveChatHistory = new JMenuItem("Save Chat History");
        saveChatHistory.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                
            }
        });
         */
        
        file.add(saveImages);
        //file.add(saveChatHistory);

        JMenuItem addClient = new JMenuItem("Connect Client");
        addClient.addMouseListener(new HoverHandler(addClient));
        addClient.addActionListener(new ActionListener() {
            @Override
            @SuppressWarnings("Convert2Lambda")
            public void actionPerformed(ActionEvent event) {
                final String hostname = (String) JOptionPane.showInputDialog(ServerFrame.this, "Enter the client IP-Address or IPv4 Address:", "Enter Client Address", JOptionPane.QUESTION_MESSAGE, icon, null, null);
                if (hostname == null || hostname.isEmpty()) {
                    return;
                }
                ParentPanel selectedPanel = selected;
                if (selectedPanel != null) {
                    selectedPanel.setSelected(true);
                }
                new Thread() {
                    @Override
                    public void run() {
                        final String remoteAddress;
                        
                        try {
                            InetAddress remoteHost = InetAddress.getByName(hostname);
                            if (Network.isLocalAddress(remoteHost)) {
                                System.out.println(remoteHost.getHostAddress() + " is a local address.");
                                remoteHost = InetAddress.getLocalHost();
                            }
                            remoteAddress = remoteHost.getHostAddress();
                        }
                        catch (UnknownHostException ex) {
                            ex.printStackTrace();
                            JOptionPane.showMessageDialog(ServerFrame.this, "Error: Could not connect to " + hostname + ".", "Connection Failed", JOptionPane.ERROR_MESSAGE, icon);
                            return;
                        }
                        
                        System.out.println("Attempting to connect to " +  remoteAddress + ".");
                        
                        TextSocket connectToClientText = new TextSocket(remoteAddress, TEXT_PORT);
                        if (!connectToClientText.isActive()) {
                            JOptionPane.showMessageDialog(ServerFrame.this, "Error: Could not connect to " + hostname + ".", "Connection Failed", JOptionPane.ERROR_MESSAGE, icon);
                            return;
                        }
                        ImageSocket connectToClientImage = new ImageSocket(remoteAddress, IMAGE_PORT);
                        if (!connectToClientImage.isActive()) {
                            JOptionPane.showMessageDialog(ServerFrame.this, "Error: Could not connect to " + hostname + ".", "Connection Failed", JOptionPane.ERROR_MESSAGE, icon);
                            return;
                        }
                        try {
                            Date connectedTime = new Date();
                            ParentPanel panel = new ParentPanel(ServerFrame.this, connectToClientText, connectToClientImage);
                            //each client gets a dedicated mouselistener so that the listener can cater
                            //to it directly
                            panel.getSplitPane().addMouseListener(new MouseAdapter() {
                                @Override
                                public void mouseReleased(MouseEvent event) {
                                    if (event.isPopupTrigger()) {
                                        ParentPanel selectedPanel = selected;
                                        if (selectedPanel != null) {
                                            String clientName = selectedPanel.getName();
                                            close.setText("Disconnect " + clientName);
                                            clientInfo.setText(clientName + " System Info (Advanced)");
                                            punish.setText("Shutdown " + clientName);
                                            //No need to reset text to original
                                        }
                                        popup.show(event.getComponent(), event.getX(), event.getY());
                                    }
                                }
                            });
                            String clientName = panel.getName();
                            tabs.addTab(clientName, panel);
                            connectionHistory.addText(clientName + " connected to Server: " + connectedTime);
                        }
                        catch (IOException ex) {
                            JOptionPane.showMessageDialog(ServerFrame.this, "Error: Could not connect to " + hostname + ".", "Connection Failed", JOptionPane.ERROR_MESSAGE, icon);
                            //No need to print stack trace here, it will be printed by ParentPanel
                            //in case of socket failure, no need to display stacktrace
                        }
                    }
                }.start();
            }
        });
        
        JMenuItem scan = new JMenuItem("Scan For Clients");
        scan.addMouseListener(new HoverHandler(scan));
        scan.addActionListener(new ActionListener() {
            
            ThreadSafeBoolean scanning = new ThreadSafeBoolean(false); 
            Map<String, NetworkScanner> cache = new TreeMap<>();
            
            @Override
            public void actionPerformed(ActionEvent event) {
                if (scanning.get()) {
                    if (isVisible()) {
                        JOptionPane.showMessageDialog(ServerFrame.this, "Error: Scanning In Progress", "Invalid Operation", JOptionPane.ERROR_MESSAGE, icon);
                    }
                    return;
                }
                
                scanning.set(true);
                
                System.out.println("Active Addresses (Before Scan): " + TextSocket.getActiveAddresses());

                Map<String, NetworkScanner> localCache = cache;
                Set<Map.Entry<String, NetworkScanner>> previousSubnets = localCache.entrySet(); //backed by the map
                
                //should always return at least 1 subnet
                //if the computer is totally disconnected, there could be a 
                //possiblitiy that this returns 0 subnets, in which case
                //we don't do anything since there is nothing to connect
                //we ignore loopback addresses
                Set<String> updatedSubnets = NetworkScanner.getLocalIPAddresses();
                
                System.out.println("Previous Subnets: " + previousSubnets);
                System.out.println("Updated Subnets: " + updatedSubnets);

                //check previous subnets
                for (Iterator<Map.Entry<String, NetworkScanner>> it = previousSubnets.iterator(); it.hasNext();) {
                    Map.Entry<String, NetworkScanner> entry = it.next();
                    String previousSubnet = entry.getKey();
                    //if a previous subnet does not appear in the new set
                    //delete it
                    if (!updatedSubnets.contains(previousSubnet)) {
                        System.out.println("Deleting: " + previousSubnet + " from cache.");
                        entry.getValue().close(); //clear memory
                        it.remove(); //will remove key-pair in the map
                    }
                }
                
                //for each updated address, if it isn't in the previous
                //cache, put it in
                for (String updatedSubnet : updatedSubnets) {
                    if (!localCache.containsKey(updatedSubnet)) {
                        System.out.println("Adding: " + updatedSubnet + " to cache.");
                        localCache.put(updatedSubnet, new NetworkScanner(updatedSubnet));
                    }
                }
                
                System.out.println("Current Subnets: " + previousSubnets);

                new Thread() {
                    @Override
                    public void run() {
                        System.out.println("ServerFrame Scanner Thread started.");
                        //Guess the last two parts of the IPv4 address, 256 * 256 possible combinations.
                        
                        ArrayList<TextSocket> reachableDevices = new ArrayList<>(0);
                        
                        for (Map.Entry<String, NetworkScanner> entry : previousSubnets) {
                            String subnet = entry.getKey();
                            System.out.println("Scanning Subnet: " + subnet);
                            List<TextSocket> sockets = entry.getValue().getReachableSockets(ServerFrame.this);
                            reachableDevices.addAll(sockets);
                            sockets.clear();
                            System.out.println("Finished Scanning Subnet: " + subnet);
                        }
                        
                        System.out.println("Active Addresses (After Scan, Before Check): " + TextSocket.getActiveAddresses());
                        System.out.println("Scanning Complete: Returning to ServerFrame Scanner Thread.");
                        
                        if (reachableDevices.isEmpty()) {
                            scanning.set(false);
                            System.out.println("No devices found.");
                            if (isVisible()) {
                                JOptionPane.showMessageDialog(ServerFrame.this, "Unable to find any clients.", "Scan Results", JOptionPane.ERROR_MESSAGE, icon);
                            }
                            System.out.println("ServerFrame Scanner Thread terminated.");
                            return;
                        }
                        
                        int count = 0;

                        for (Iterator<TextSocket> it = reachableDevices.iterator(); it.hasNext() && isEnabled();) {
                            TextSocket connectToClientText = it.next();
                            if (!connectToClientText.isActive()) {
                                System.out.println("TextSocket failed to connect.");
                                connectToClientText.close();
                                continue;
                            }
                            ImageSocket connectToClientImage = new ImageSocket(connectToClientText.getAddress(), IMAGE_PORT);
                            if (!connectToClientImage.isActive()) {
                                System.out.println("Rejected: " + connectToClientText.getAddress() + " since ImageSocket failed to connect!");
                                connectToClientText.close();
                                continue;
                            }
                            try {
                                Date connectedTime = new Date();
                                ParentPanel panel = new ParentPanel(ServerFrame.this, connectToClientText, connectToClientImage);
                                panel.getSplitPane().addMouseListener(new MouseAdapter() {
                                    @Override
                                    public void mouseReleased(MouseEvent event) {
                                        if (event.isPopupTrigger()) {
                                            ParentPanel selectedPanel = selected;
                                            if (selectedPanel != null) {
                                                String clientName = selectedPanel.getName();
                                                close.setText("Disconnect " + clientName);
                                                clientInfo.setText(clientName + " System Info (Advanced)");
                                                punish.setText("Shutdown " + clientName);
                                            }
                                            popup.show(event.getComponent(), event.getX(), event.getY());
                                        }
                                    }
                                });
                                String clientName = panel.getName();
                                tabs.addTab(clientName, panel);
                                connectionHistory.addText(clientName + " connected to Server: " + connectedTime);
                                ++count;
                            }
                            catch (IOException ex) {
                                
                            }
                        }
                        
                        reachableDevices.clear();
                        reachableDevices.trimToSize();
                        scanning.set(false);
                        
                        System.out.println("Active Addresses (After Scan, After Check): " + TextSocket.getActiveAddresses());
                        System.out.println(count + " devices found.");
                        
                        if (isVisible()) {
                            String message;
                            switch (count) {
                                case 0:
                                    message = "Unable to find any clients.";
                                    break;
                                case 1:
                                    message = "1 client connected successfully.";
                                    break;
                                default:
                                    message = count + " clients connected succesfully.";
                            }
                            JOptionPane.showMessageDialog(ServerFrame.this, message, "Scan Results", JOptionPane.ERROR_MESSAGE, icon);
                        }
                        
                        System.out.println("ServerFrame Scanner Thread terminated.");
                    }
                }.start();
            }
        });

        JMenuItem closeAll = new JMenuItem("Disconnect All Clients");
        closeAll.addMouseListener(new HoverHandler(closeAll));
        closeAll.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                int tabCount = tabs.getTabCount();
                if (tabCount == 0) {
                    JOptionPane.showMessageDialog(ServerFrame.this, "Error: There are no clients to disconnect.", "Invalid Operation", JOptionPane.ERROR_MESSAGE, icon);
                    return;
                }
                if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(ServerFrame.this, "Are you sure you want to disconnect all clients?", "Disconnect All?", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, icon)) {
                    for (int index = tabCount - 1; index >= 0; --index) { //Loop backwards to prevent this index problem
                       ((ParentPanel) tabs.getComponentAt(index)).close(true); //fail loudly, cast error should NEVER happen
                        //close will reduce size of tabs by 1, ParentPanel holds a reference
                        //close also adds text to connection history
                    }
                }
            }
        });

        JMenuItem history = new JMenuItem("Connection History");
        history.addMouseListener(new HoverHandler(history));
        history.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                TextFrame connectionHistoryReference = connectionHistory;
                if (connectionHistoryReference.getText().isEmpty()) {
                    JOptionPane.showMessageDialog(ServerFrame.this, "Error: No clients have been connected to this server yet.\nSee: " + Quotes.surroundWithDoubleQuotes("Connect Client") + " to connect a client to this server.", "Invalid Operation", JOptionPane.ERROR_MESSAGE, icon);
                    return;
                }
                connectionHistoryReference.setVisible(true);
            }
        });
        
        JMenuItem allShots = new JMenuItem("View All Screenshots");
        allShots.addMouseListener(new HoverHandler(allShots));
        allShots.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (bank.isEmpty()) {
                    JOptionPane.showMessageDialog(ServerFrame.this, "Error: There are no captured screenshots to show.", "Invalid Operation", JOptionPane.ERROR_MESSAGE, icon);
                    return;
                }
                master.setVisible(true);
            }
        });
        
        menuBar.add(file);
        menuBar.add(addClient);
        menuBar.add(scan);
        menuBar.add(closeAll);
        menuBar.add(history);
        menuBar.add(allShots);

        Container contentPane = super.getContentPane();
        contentPane.setBackground(Color.WHITE);

        tabs = new JTabbedPane();
        tabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        tabs.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent event) {
                popup.setVisible(false);
                
                //un focus the previous ParentPanel
                ParentPanel selectedPanel = selected;
                if (selectedPanel != null) {
                    selectedPanel.setSelected(false);
                }
                
                //focus the new ParentPanel
                selectedPanel = (ParentPanel) tabs.getSelectedComponent(); 
                if (selectedPanel != null) {
                    selectedPanel.setSelected(true);
                }
                
                selected = selectedPanel;
            }
        });
        super.add(tabs, BorderLayout.CENTER);

        System.out.println("Frame Bounds: " + super.getBounds());
        
        try {
            BufferedImage iconImage = ImageIO.read(ServerFrame.class.getResourceAsStream("/Images/Eye.jpg"));
            icon.setImage(iconImage);
            connectionHistory = new TextFrame(this, iconImage, "Connection History", "", true);
            super.setIconImage(iconImage);
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
        
        master = new ScreenShotDisplayer(this, "All Captured Screenshots");
        
        super.setVisible(true);
        super.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        System.out.println("Menu Bar Bounds: " + menuBar.getBounds());
        System.out.println("Content Pane Bounds: " + contentPane.getSize());

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
    }
    
    public JTabbedPane getTabs() {
        return tabs;
    }

    public Icon getIcon() {
        return icon;
    }
    
    public TextFrame getConnectionHistoryFrame() {
        return connectionHistory;
    }
    
    @Override
    public synchronized boolean isEnabled() {
        return super.isEnabled();
    }
    
    @Override
    public void dispose() {
        super.setEnabled(false);
        super.dispose(); //Make window invisible
        
        //First, disconnect all clients as we are closing
        for (int index = tabs.getTabCount() - 1; index >= 0; --index) { //Loop backwards to prevent this index problem
            ((ParentPanel) tabs.getComponentAt(index)).close(true); //fail loudly, cast error should NEVER happen
            //close will reduce size of tabs by 1, ParentPanel holds a reference
        }
        
        super.getContentPane().removeAll(); //remove all sub-components
        
        popup.removeAll();
        menuBar.removeAll();
        tabs.removeAll();
        
        popup = null;
        menuBar = null;
        tabs = null;
        
        icon = null;
        
        connectionHistory.dispose();
        connectionHistory = null;

        master.dispose();
        master = null;

        selected = null;
        bank.close();
        bank = null;

        //System.exit(0); //Allow threads to clean up
    }

    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public static void main(String[] args) {
        ImageIO.setUseCache(false); //Disk operations are too slow!!!
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
            ex.printStackTrace();
        }
        new ServerFrame();
    }

    private static final class HoverHandler extends MouseAdapter implements Recyclable {

        private JMenuItem item;

        private HoverHandler(JMenuItem menuItem) {
            item = menuItem;
        }

        @Override
        public void mouseEntered(MouseEvent event) {
            JMenuItem menuItem = item;
            menuItem.setArmed(true);
            menuItem.repaint();
        }

        @Override
        public void mouseExited(MouseEvent event) {
            JMenuItem menuItem = item;
            menuItem.setArmed(false);
            menuItem.repaint();
        }

        @Override
        public void recycle() {
            item = null;
        }
    }
}