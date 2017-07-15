package de.buxdehuda.archivar;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;

public class Archivar {
    
    public static final Preferences PREFS = Preferences.userNodeForPackage(Archivar.class);
    public static final String FOLDER = PREFS.get("folder", System.getProperty("user.home") + File.separator + "Archivar") + File.separator;
    public static final String SPLIT = Pattern.quote("+");
    
    Connection conn;
    private List<Picture> rawPictures;
    private List<Picture> readyPictures;
    
    public Archivar() {
    	rawPictures = generateRawPictures();
    	conn = connect();
        createDatabase();
        
    	readyPictures = generateReadyPictures();
    }
    
    public Connection connect() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(Archivar.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            return DriverManager.getConnection("jdbc:sqlite:" + FOLDER + "bilder.db");
        }   catch (SQLException ex) {
            Logger.getLogger(Archivar.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
        
    private void createDatabase() {
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS pics(name TEXT PRIMARY KEY NOT NULL, place TEXT, reason TEXT, persons TEXT, coordinates TEXT, time NUMERIC)");
        } catch (SQLException ex) {
            Logger.getLogger(Archivar.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public List<Picture> search(String place, String time, String reason, String persons) {
        List<Picture> pics = new ArrayList<>();
        StringBuilder query = new StringBuilder("SELECT * FROM pics WHERE ");
        int count = 0;
        if (!place.isEmpty()) {
            query.append("LOWER(place) = '");
            query.append(place.toLowerCase());
            query.append("'");
            count++;
        }
        if (!reason.isEmpty()){
            if (count < 0) {
                query.append(" AND ");
            }
            query.append("LOWER(reason) = '");
            query.append(reason.toLowerCase());
            query.append("'");
            count++;
        }
        if (!time.isEmpty()) {
            if (count < 0) {
                query.append(" AND ");
            }
            if (time.length() == 4) {
                int year = Integer.parseInt(time);
                System.out.println("Jahr = " + year);
                query.append("time >= '");
                query.append(new GregorianCalendar(year, 1, 1).getTimeInMillis());
                query.append("' AND time < '");
                query.append(new GregorianCalendar(year + 1, 1, 1).getTimeInMillis());
                query.append("'");
            } else {
                query.append("time = '");
                try {
                    Date d = Picture.FORMAT.parse(time);                    
                    query.append(d.getTime());
                } catch (ParseException ex) {
                    Logger.getLogger(Archivar.class.getName()).log(Level.SEVERE, null, ex);
                    query.append(0);
                }
                query.append("'");
            }
            count++;
        }
        if (!persons.isEmpty()) {
            if (count < 0) {
                query.append(" AND ");
            }
            String[] split;
            if (persons.contains("+")) {
                split = persons.split(Archivar.SPLIT);
                query.append("LOWER(persons) LIKE '%");
                for (String person : split) {
                    query.append(person);
                    query.append("%' AND LOWER(persons) LIKE '%");
                }
                int end = query.length();
                query.delete(end - 27, end);
            } else {
                query.append("LOWER(persons) LIKE '%");
                query.append(persons.toLowerCase());
                query.append("%'");
            }
            count++;
        }
        if (count == 0) {
            return getReadyPictures();
        }
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(query.toString());
            while (rs.next()) {
                pics.add(new Picture(rs.getString("name"), rs.getString("place"), rs.getString("reason"), rs.getString("persons"), rs.getString("coordinates"), rs.getDate("time")));
            }
            rs.close();
        } catch (SQLException ex) {
            Logger.getLogger(Archivar.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (pics.isEmpty()) {
            pics.add(new NoResultPicture());
        }
        return pics;
    }
    
    public List<Picture> getRawPictures() {
        return this.rawPictures;
    }
    
    public List<Picture> getReadyPictures() {
        return this.readyPictures;
    }

    private List<Picture> generateRawPictures() {
        File dir = new File(Picture.FOLDER_REMAINING);
        dir.mkdirs();
        List<Picture> pics = new ArrayList<>();
        for (File pic : dir.listFiles()) {
            pics.add(new Picture(pic.getPath()));
        }
        return pics;
    }
    
    private List<Picture> generateReadyPictures() {
        File dir = new File(Picture.FOLDER_READY);
        dir.mkdir();
        
        List<Picture> pics = new ArrayList<>();
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT * FROM pics");
            while (rs.next()) {
                pics.add(new Picture(rs.getString("name"), rs.getString("place"), rs.getString("reason"), rs.getString("persons"), rs.getString("coordinates"), rs.getDate("time")));
            }
            rs.close();
        } catch (SQLException ex) {
            Logger.getLogger(Archivar.class.getName()).log(Level.SEVERE, null, ex);
        }
        return pics;
    }
    
    public Dimension calculateSize(int origHeight, int origWidth, int displayHeight, int displayWidth) {
        int width, height;
        double factor;
        if (origWidth > origHeight) {
            width = displayWidth;
            factor = (double)width / origWidth;
            height =(int) (origHeight * factor);
        } else {
            height = displayHeight;
            factor = (double)height / origHeight;
            width = (int) (origWidth * factor);
        }
        return new Dimension(width, height);
    }
    
    /* 
     * by Thomas on StackOverflow
     * https://stackoverflow.com/questions/5895829/resizing-image-in-java/5895864#5895864 
     */
    public Image scaleImage(Dimension d, Image img) {
        BufferedImage thumbImage = new BufferedImage(d.width, d.height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = thumbImage.createGraphics(); //create a graphics object to paint to
        g.setBackground(Color.WHITE);
        g.setPaint(Color.WHITE);
        g.fillRect(0, 0, d.width, d.height);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(img, 0, 0, d.width, d.height, null); //draw the image scaled
        g.dispose();
        return thumbImage;
    }

    public Connection getConnection() {
        return conn;
    }
    
}
