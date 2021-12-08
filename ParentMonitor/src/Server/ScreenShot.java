package Server;

import static Server.Network.PNG;
import java.awt.image.BufferedImage;
import java.util.Date;
import java.util.Objects;

public final class ScreenShot implements Comparable<ScreenShot> {
    
    private final Date created;
    private final String name;
    private final BufferedImage image;
    
    public ScreenShot(Date taken, String screenShotName, BufferedImage screenShot) {
        created = new Date(taken.getTime());
        name = screenShotName;
        image = screenShot;
    }

    @Override
    public int compareTo(ScreenShot other) {
        return created.compareTo(other.created);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof ScreenShot)) {
            return false;
        }
        ScreenShot other = (ScreenShot) obj;
        return created.equals(other.created) && name.equals(other.name);
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 41 * hash + Objects.hashCode(created);
        hash = 41 * hash + Objects.hashCode(name);
        hash = 41 * hash + Objects.hashCode(image);
        return hash;
    }
    
    public Date dateTaken() {
        return new Date(created.getTime());
    }
    
    public String getName() {
        return name;
    }
    
    public String getFileName() {
        return name + "." + PNG;
    }
    
    public BufferedImage getImage() {
        return image;
    }
}