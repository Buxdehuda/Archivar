package de.buxdehuda.archivar;

import java.awt.Image;
import java.io.IOException;
import java.sql.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

public class NoResultPicture extends Picture {
    
    public NoResultPicture() {
        super("Nichts");
        date = new Date(0);
        persons = "";
        place = "Keine Ergebnisse";
    }
    
    @Override
    public Image getImage() {
        try {
            return ImageIO.read(getClass().getResource("/res/nothing.jpg"));
        } catch (IOException ex) {
            Logger.getLogger(NoResultPicture.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
}
