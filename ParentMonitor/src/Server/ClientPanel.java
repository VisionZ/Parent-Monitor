package Server;

import Util.StreamCloser;
import Util.ThreadSafeBoolean;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import javax.swing.JPanel;

public class ClientPanel extends JPanel implements Runnable {
    
    private final ThreadSafeBoolean terminated = new ThreadSafeBoolean(false);
    private final ThreadSafeBoolean repaint = new ThreadSafeBoolean(true);

    //stream variable
    private ImageSocket imageConnection;
    
    //Rendering variables
    private BufferedImage buffer;
    private Graphics2D graphics;

    private BufferedImage previousScreenShot;
    private ScreenShotDisplayer displayer;
    private boolean screenShotTaken = false;

    private String clientName;

    //private Semaphore repaintControl = new Semaphore(1, true);
    
    //Any IOExceptions should be thrown and passed up to the ParentPanel
    //which will pass any of its own IOExceptions to the ServerFrame, allowing
    //the server frame to display a error dialog
    @SuppressWarnings("CallToThreadStartDuringObjectConstruction")
    public ClientPanel(ServerFrame parent, String client, ImageSocket clientImageConnection) {
        imageConnection = clientImageConnection;
        displayer = new ScreenShotDisplayer(parent, "Screenshots Taken From " + (clientName = client));
        
        super.setBackground(Color.RED);
        super.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                //Everytime this component is resized, change 
                if (isDisplayable()) { //Avoid possible NPE, since createImage only works while this component is displayable
                    graphics = (buffer = (BufferedImage) createImage(getWidth(), getHeight())).createGraphics();
                }
            }
        });

        new Thread(this, client + " Client Image Render Thread").start();
        new ImageRetrieverWorkerThread().start(); //formerly assigned this to variable worker
    }
    
    private final class ImageRetrieverWorkerThread extends Thread {
        
        //Shared and modified by multiple threads, to avoid using ThreadSafeBoolean
        //Could make a Object to syncrhonize lock on when accessing booleans
        //private ThreadSafeBoolean updateScreenShot = new ThreadSafeBoolean(true);
        //CPU USAGE GOES UP WHEN WE ARE [NOT] READING CLIENT IMAGE!!!
        
        private ImageRetrieverWorkerThread() {
            super(clientName + " Client Image Retriever Worker Thread");
        }
        
        @Override
        public final void run() {
            ImageSocket imageStream = imageConnection; //avoid getfield opcode
            ThreadSafeBoolean update = terminated; //avoid getfield opcode
            ThreadSafeBoolean updateScreen = repaint; //avoid getfield opcode
            while (!update.get()) { //Loop breaks automatically AFTER close() has been called and finished execution
                if (updateScreen.get()) { //formerly we also checked updateScreenShot.get()
                    //send.println(REQUEST_IMAGE);
                    //Instead of sending a signal to the client to provide a screenshot
                    //We just wait for a screenshot and update as necessary
                    //This does put more work on the client however, but less on us.
                    try {
                        //Will block until the image has been completely read
                        previousScreenShot = imageStream.readImage();
                    }
                    catch (IOException ex) {
                        System.err.println("Failed to retreve image from client!");
                        //If the client has been forcibly terminated on their end
                        //without sending the final exit message, such as from manual
                        //shutdown, we must take care to destroy the client on this end
                        //as well, this is taken care of in the ParentPanel
                        ex.printStackTrace();
                    }
                }
            }
            //updateScreenShot = null;
            System.out.println("Image Retriever Exiting. Client Name Should Be Set to Null: " + clientName);
            //clientName should be set to null here, since close() has been called
        }
    }

    /*
    public boolean isUpdating() {
        return worker.updateScreenShot.get();
    }
    
    public void setUpdate(boolean update) {
        worker.updateScreenShot.set(update);
    }
    
    public void toggleUpdate() {
        worker.updateScreenShot.invert();
    }
     */
    
    public void saveCurrentShot(ImageBank bank, ScreenShotDisplayer master) {
        if (previousScreenShot != null) {
            Date taken = new Date();
            ScreenShot screenShot = new ScreenShot(taken, clientName + " Screenshot [" + taken.getTime() + "]", previousScreenShot);
            displayer.addScreenShot(taken, screenShot);
            master.addScreenShot(taken, screenShot);
            bank.addScreenShot(screenShot);
            screenShotTaken = true;
        }
    }
    
    public boolean takenScreenShot() {
        return screenShotTaken;
    }

    public void showScreenShotDisplayer() {
        displayer.setVisible(true);
    }

    @Override
    public void paintComponent(Graphics context)  {
        super.paintComponent(context);
        
        final int width = getWidth();
        final int height = getHeight();
        
        if (graphics == null) {
            //Should be null only once, except when close() is called
            if (isDisplayable()) { //Avoid possible NPE, since createImage only works while this component is displayable
                graphics = (buffer = (BufferedImage) createImage(width, height)).createGraphics();
            }
        }

        //Prints out max window bounds
        //System.out.println(previousScreenShot.getWidth());
        //System.out.println(previousScreenShot.getHeight());
        //System.out.println();
        
        //Does not throw NPE
        graphics.drawImage(previousScreenShot, 0, 0, width, height, null);
        
        //Prints out current panel size
        //System.out.println("Width: " + width + " Height: " + height);
        
        context.drawImage(buffer, 0, 0, null);
    }
    
    //No need to notify client we are exiting, ParentFrame takes care of that
    public synchronized final void close() {
        if (terminated.get()) { //Lock
            return;
        }
     
        repaint.set(false);
        
        StreamCloser.close(imageConnection);
        imageConnection = null;
        
        buffer = null;
        graphics = null;
        
        previousScreenShot = null;
        displayer.dispose();
        displayer = null;
        
        clientName = null;

        super.setEnabled(false);
        super.setVisible(false);
        
        terminated.set(true); //Unlock
    }
    
    public void setRepaint(boolean shouldRepaint) {
        repaint.set(shouldRepaint);
    }

    @Override
    @SuppressWarnings("SleepWhileInLoop")
    public final void run() {
        ThreadSafeBoolean update = terminated; //avoid getfield opcode
        ThreadSafeBoolean repaintScreen = repaint; //avoid getfield opcode
        while (!update.get()) {
            if (repaintScreen.get()) {
                repaint();
            }
            try {
                TimeUnit.MILLISECONDS.sleep(50);
            }
            catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
        System.out.println("Render Exiting. Client Name Should Be Set to Null: " + clientName);
        //clientName should be set to null here, since close() has been called
    }
}