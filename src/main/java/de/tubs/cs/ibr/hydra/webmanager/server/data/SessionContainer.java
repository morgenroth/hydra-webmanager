package de.tubs.cs.ibr.hydra.webmanager.server.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidParameterException;

import de.tubs.cs.ibr.hydra.webmanager.shared.Session;

public class SessionContainer {
    private String mSessionKey = null;
    private File mPath = null;
    
    // create a static default session
    private static final SessionContainer mDefault = new SessionContainer();
    
    public static SessionContainer getDefault() {
        return mDefault;
    }
    
    private SessionContainer() {
    }
    
    private SessionContainer(Long sessionId) {
        mSessionKey = sessionId.toString();
    }
    
    public static SessionContainer getContainer(Session s) {
        if (s == null || s.id == null)
            throw new InvalidParameterException("Given session or its id is null");
        
        return new SessionContainer(s.id);
    }
    
    public synchronized void initialize(SessionContainer defaultContainer) throws IOException {
        // only initialize once
        if (mPath != null) return;
        
        // check if the session key is set
        if (mSessionKey == null) {
            // initialize default session container
            mPath = Configuration.getDefaultSessionPath();
        } else {
            // construct the container path
            mPath = new File(Configuration.getSessionPath(), mSessionKey);
        }
        
        // check if the session already exists
        if (!mPath.exists()) {
            // throw exception if no default container is specified
            if (defaultContainer == null) {
                throw new SessionContainerNotInitialized();
            }
            
            // session not available - copy the default session
            copy(defaultContainer, mPath);
        }
    }
    
    public synchronized void destroy() {
        // do not destroy the default container
        if (mSessionKey == null) return;
        
        // check if the path is set
        if (mPath == null) return;
        
        // delete the folder
        deleteFolder(mPath);
    }
    
    public void inject(Session s) {
        // TODO: inject container data into session object
    }
    
    private static void copy(SessionContainer source, File targetPath) throws IOException {
        if ((source == null) || (targetPath == null)) {
            throw new InvalidParameterException("source or target is null");
        }
        
        // first check if the target path exists
        if (!targetPath.exists()) {
            if (!targetPath.mkdirs()) {
                throw new IOException("can not create container path");
            }
        }
        
        // initialize the source
        source.initialize(null);
        
        // check if the source path exists
        File sourcePath = source.mPath;
        if ((sourcePath == null) || !sourcePath.exists()) {
            throw new IOException("source path does not exists");
        }
        
        // copy the container
        copyFolder(sourcePath, targetPath);
    }
    
    private static void copyFolder(File source, File target) throws IOException {
        if (source.isDirectory()) {
            if (!target.exists()) {
                if (!target.mkdir()) {
                    throw new IOException("can not create container path");
                }
            }
            
            // get all files in this directory
            String files[] = source.list();
            
            for (String f : files) {
                File src = new File(source, f);
                File dst = new File(target, f);
                
                // recursive copy
                copyFolder(src, dst);
            }
        } else {
            InputStream in = new FileInputStream(source);
            OutputStream out = new FileOutputStream(target);
            
            byte[] buffer = new byte[4096];
            
            int length = 0;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
            
            in.close();
            out.close();
        }
    }
    
    private static void deleteFolder(File folder) {
        if (!folder.exists()) return;
        
        File[] files = folder.listFiles();
        if (files != null) {
            for (File f: files) {
                if (f.isDirectory()) {
                    deleteFolder(f);
                } else {
                    f.delete();
                }
            }
        }
        folder.delete();
    }
}
