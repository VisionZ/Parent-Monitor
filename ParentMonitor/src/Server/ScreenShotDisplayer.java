package Server;

import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.Date;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

//Should use similar design to parent, display images in their respective tabs
//will hold the list of saved screenshots per client
public final class ScreenShotDisplayer extends JFrame {
    
    private JTabbedPane tabs;
    
    public ScreenShotDisplayer(ServerFrame parent, String title) {
        super(title);
        super.setIconImage(parent.getIconImage());
       
        super.setSize(parent.getWidth() / 2, parent.getHeight() / 2);
        super.setLocationRelativeTo(parent);
        
        JTabbedPane tabsReference = new JTabbedPane();
        tabsReference.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        super.add(tabs = tabsReference, BorderLayout.CENTER);
        
        super.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        //Dont set visible
        //Keep this displayer alive
    }
    
    //private int count = 0; //Could've used counter instead to label files
    
    public final void addScreenShot(Date taken, ScreenShot screenShot) {
        tabs.addTab(taken.toString(), new ImagePanel(screenShot));
    }
    
    @Override
    public void dispose() {
        JTabbedPane tabsReference = tabs;
        if (tabsReference == null) {
            return;
        }
        super.dispose();
        for (int index = 0, tabCount = tabsReference.getTabCount(); index < tabCount; ++index) {
            ImagePanel panel = (ImagePanel) tabsReference.getComponentAt(index); //fail loudly, this should always work
            panel.recycle();
        }
        tabsReference.removeAll();
        tabs = null;
    }
    
    private static final class ImagePanel extends JPanel implements Recyclable {
        
        private BufferedImage capturedImage;
        
        private ImagePanel(ScreenShot shot) {
            capturedImage = shot.getImage();
            super.setToolTipText(shot.getFileName());
        }
        
        @Override
        protected void paintComponent(Graphics context) {
            super.paintComponent(context);
            context.drawImage(capturedImage, 0, 0, getWidth(), getHeight(), this);
        }

        @Override
        public void recycle() {
            super.setEnabled(false);
            BufferedImage screenShot = capturedImage;
            if (screenShot != null) {
                screenShot.flush();
                capturedImage = null;
            }
        }
    }
}