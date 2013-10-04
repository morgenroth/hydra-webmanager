package de.tubs.cs.ibr.hydra.webmanager.server.data;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

public class Configuration {
    private static boolean mPropertiesLoaded = false;
    private static Properties mProperties = new Properties();
    
    public static File getSessionPath() throws IOException {
        return new File(getHomePath(), "sessions");
    }
    
    public static File getDefaultSessionPath() throws IOException {
        return new File(getSessionPath(), "default");
    }
    
    public static File getHomePath() throws HomePathNotSetException {
        // get hydra home path
        String home_path = System.getProperty("config.hydra");
        
        if (home_path == null)
            throw new HomePathNotSetException();
        
        return new File(home_path);
    }
    
    public static String getWebLocation() throws IOException {
        String ret = null;
        
        Properties p = getProperties();
        String webLocation = p.getProperty("web.location");
        
        if (webLocation == null) {
            ret = "http://localhost/hydra";
        } else {
            ret = webLocation;
        }
        
        return ret;
    }
    
    public static File getWebDirectory() throws IOException {
        File ret = null;
        
        Properties p = getProperties();
        String webLocation = p.getProperty("web.dir");
        
        if (webLocation == null) {
            ret = new File(getHomePath(), "htdocs");
        } else {
            ret = new File(webLocation);
        }
        
        // create directory is it does not exists
        if (!ret.exists()) {
            ret.mkdirs();
        }
        
        return ret;
    }
    
    public static Properties getProperties() throws IOException {
        if (!mPropertiesLoaded) {
            synchronized(mProperties) {
                if (mPropertiesLoaded) return mProperties;
                BufferedInputStream stream = new BufferedInputStream(new FileInputStream(new File(getHomePath(), "config.properties")));
                mProperties.load(stream);
                stream.close();
                mPropertiesLoaded = true;
            }
        }
        
        return mProperties;
    }
    
    private static File getImagesPath() throws IOException {
        File imagePath = new File(getWebDirectory(), "dl");
        
        if (!imagePath.exists()) {
            imagePath.mkdirs();
        }
        
        return imagePath;
    }
    
    public static ArrayList<String> getAvailableImages() throws IOException {
        ArrayList<String> ret = new ArrayList<String>();
        
        String[] images = getImagesPath().list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                if (name.endsWith(".img.gz")) return true;
                return false;
            }
        });
        
        if (images == null) {
            // can not read directory
            return ret;
        }
        
        for (String f : images) {
            ret.add(f);
        }
        
        return ret;
    }
}
