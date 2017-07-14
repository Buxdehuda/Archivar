package de.buxdehuda.archivar;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

public class Picture {
    
    public static final String FOLDER_REMAINING = Archivar.FOLDER + Archivar.PREFS.get("folder-raw", "unbeschriftet") + File.separator;
    public static final String FOLDER_READY = Archivar.FOLDER + Archivar.PREFS.get("folder-ready", "bearbeitet") + File.separator;
    public static final SimpleDateFormat FORMAT = new SimpleDateFormat(Archivar.PREFS.get("format", "dd.MM.yyyy"));
    public static final Color COLOR = Color.decode(Archivar.PREFS.get("color", "#FF0000"));
    
    String fileName;
    boolean isEdited;
    String place;
    String reason;
    String persons;
    Date date;
    List<RelativeCoordinate> coordinates;

    Picture(String path) {
        this.fileName = path.substring(path.lastIndexOf(File.separator) + 1);
        this.isEdited = false;
        this.coordinates = new ArrayList<>();
    }

    public Picture(String fileName, String place, String reason, String persons, String locations, Date date) {
        this.fileName = fileName;
        this.isEdited = true;
        this.place = place;
        this.reason = reason;
        this.persons = persons;
        this.date = date;
        this.coordinates = new ArrayList<>();
        if (!locations.contains("/")) { //at least one
            return;
        }
        for (String s : locations.split(Archivar.SPLIT)) {
            this.coordinates.add(new RelativeCoordinate(s));
        }
    }
    
    public Image getImage() {
        URI uri = Paths.get(this.getPath()).toUri();
        try {
            BufferedImage img = ImageIO.read(new File(uri));
            return img;
        } catch (IOException ex) {
            Logger.getLogger(Picture.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    public void drawCoordinates(Image img, Dimension d) {
        Graphics g = img.getGraphics();
        g.setColor(COLOR);
        int i = 1;
        for (RelativeCoordinate rc : this.getCoordinates()) {
            g.drawString(Integer.toString(i), (int) (rc.getX() * d.width), (int) (rc.getY() * d.height));
            i++;
        }
        g.dispose();
    }
    
    public void insertData(String place, String reason, String persons, String date) {
        this.isEdited = true;
        this.place = place;
        this.reason = reason;
        this.persons = persons;
        try {
            this.date = new Date(Picture.FORMAT.parse(date).getTime());
        } catch (ParseException ex) {
            Logger.getLogger(Picture.class.getName()).log(Level.SEVERE, null, ex);
            this.date = new Date(0);
        }
    }
    
    public void save(Connection conn) {
        File move = new File(FOLDER_READY + fileName);
        int i = 1;
        String origName = fileName;
        while (move.exists()) {
            fileName = origName + "_" + i;
            move = new File(FOLDER_READY + fileName);
        }
        try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO pics (name, place, reason, time, persons, coordinates) VALUES(?, ?, ?, ?, ?, ?)")) {
            stmt.setString(1, fileName);
            stmt.setString(2, place);
            stmt.setString(3, reason);
            stmt.setDate(4, date);
            stmt.setString(5, persons);
            stmt.setString(6, serializeCoordinates());
            stmt.execute();
        } catch (SQLException ex) {
            Logger.getLogger(Picture.class.getName()).log(Level.SEVERE, null, ex);
        }
        //move to other folder
        (new File(FOLDER_REMAINING + fileName)).renameTo(move);
    }
    @Override
    public String toString() {
        return this.fileName;
    }
    
    public String getPath() {
        return (this.isEdited ? FOLDER_READY : FOLDER_REMAINING) + fileName;
    }

    public List<RelativeCoordinate> getCoordinates() {
        return coordinates;
    }
    
    private String serializeCoordinates() {
        if (coordinates.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(coordinates.get(0));
        for (int i = 1; i < coordinates.size(); i++) {
            sb.append('+');
            sb.append(coordinates.get(i));
        }
        return sb.toString();
    }
    
    public String listPersons() {
        if (persons.isEmpty()) {
            return "";
        }
        String[] list = persons.split(Archivar.SPLIT);
        if (list.length > 0) {
            StringBuilder sb = new StringBuilder();
            sb.append("1. ");
            sb.append(list[0]);
            for (int i = 1; i < list.length; i++) {
                sb.append(", ");
                sb.append(i + 1);
                sb.append(". ");
                sb.append(list[i]);
            }
            return sb.toString();
        }
        return "";
    }
    
}
