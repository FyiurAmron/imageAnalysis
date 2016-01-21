package vax.gfx;

import java.awt.Image;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

/**

 @author toor
 */
public class ImageLabel extends JLabel {
    private final ImageIcon icon;

    public ImageLabel ( Image image ) {
        super();
        icon = new ImageIcon( image );
        setIcon( icon );
    }

    public void setImage ( Image image ) {
        icon.setImage( image );
    }
}
