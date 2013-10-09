package de.tubs.cs.ibr.hydra.webmanager.server.data;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidParameterException;
import java.util.Map.Entry;

import com.nikhaldimann.inieditor.IniEditor;

import de.tubs.cs.ibr.hydra.webmanager.shared.MobilityParameterSet;
import de.tubs.cs.ibr.hydra.webmanager.shared.MobilityParameterSet.MobilityModel;
import de.tubs.cs.ibr.hydra.webmanager.shared.Session;

public class SessionContainer {
    private String mSessionKey = null;
    private File mPath = null;
    private File mBasePath = null;
    
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
        
        File p = null;
        
        // check if the session key is set
        if (mSessionKey == null) {
            // initialize default session container
            p = Configuration.getDefaultSessionPath();
        } else {
            // construct the container path
            p = new File(Configuration.getSessionPath(), mSessionKey);
        }
        
        // check if the session already exists
        if (!p.exists()) {
            // throw exception if no default container is specified
            if (defaultContainer == null) {
                throw new SessionContainerNotInitialized();
            }
            
            // session not available - copy the default session
            copy(defaultContainer, p);
        }
        
        mPath = p;
        mBasePath = new File(mPath, "base");
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
        if (mPath == null) return;
        
        // inject container data into session object
        IniEditor base = new IniEditor();
        
        try {
            // load configuration of 'base'
            base.load(new File(mBasePath, "config.properties"));
            
            // get selected image file
            s.image = base.get("image", "file");
        } catch (IOException e) {
            // can not load configuration
            e.printStackTrace();
        }
        
        // load package repository
        File repo_file = new File(mBasePath, "opkg.external");
        if (repo_file.exists()) {
            try {
                BufferedReader in = new BufferedReader(new FileReader(repo_file));
                try {
                    String data = in.readLine();
                    String[] fragments = data.split(" ");
                    if (fragments.length >= 3) {
                        s.repository = fragments[2];
                    }
                } finally {
                    in.close();
                }
            } catch (IOException e) {
                // can not read repository
                e.printStackTrace();
            }
        }
        
        // load packages
        File packages_file = new File(mBasePath, "packages.install");
        s.packages = loadFile(packages_file);
        
        // load monitor nodes
        File monitor_file = new File(mBasePath, "monitor-nodes.txt");
        s.monitor_nodes = loadFile(monitor_file);
        
        // load qemu template
        File qemu_template = new File(mBasePath, "node-template.qemu.xml");
        s.qemu_template = loadFile(qemu_template);
        
        // load vbox template
        File vbox_template = new File(mBasePath, "node-template.vbox.xml");
        s.vbox_template = loadFile(vbox_template);
        
        /**
         * Read session configuration
         */
        IniEditor sessionConf = new IniEditor();
        
        try {
            // load configuration of 'base'
            sessionConf.load(new File(mPath, "config.properties"));
            
            MobilityParameterSet m = new MobilityParameterSet();
            if (sessionConf.hasOption("mobility", "model")) {
                m.model = MobilityModel.fromString(sessionConf.get("mobility", "model"));
                String section = null;
                
                // clear all parameters
                m.parameters.clear();
                
                switch (m.model) {
                    case NONE:
                        break;
                    case RANDOM_WALK:
                        section = "randomwalk";
                        break;
                    case STATIC:
                        section = "staticconnections";
                        
                        // read connections from file
                        if (sessionConf.hasOption(section, "connections")) {
                            File f = new File(mPath, sessionConf.get(section, "connections"));
                            String data = SessionContainer.loadFile(f);
                            
                            if (data != null) {
                                m.parameters.put("connections", data);
                            }
                        }
                        
                        break;
                    case THE_ONE:
                        section = "onetrace";
                        
                        break;
                    default:
                        break;
                }
                
                if ((section != null) && sessionConf.hasSection(section)) {
                    for (String name : sessionConf.optionNames(section)) {
                        // only set parameter if not already set
                        if (!m.parameters.containsKey(name)) {
                            m.parameters.put(name, sessionConf.get(section, name));
                        }
                    }
                }
            }
            
            // assign mobility parameters
            s.mobility = m;
        } catch (IOException e) {
            // can not load configuration
            e.printStackTrace();
        }
    }
    
    public void apply(Session s) {
        // save configuration
        IniEditor base = new IniEditor();
        
        try {
            File conf = new File(mBasePath, "config.properties");
            
            // read configuration of 'base'
            base.load(conf);
            
            // assign new parameters
            if (s.image != null) {
                base.set("image", "file", s.image);
            }
            
            if (s.repository != null) {
                // apply repository URL
                File repo_file = new File(mBasePath, "opkg.external");
                String repository = null;
                
                if (repo_file.exists()) {
                    try {
                        BufferedReader in = new BufferedReader(new FileReader(repo_file));
                        try {
                            String data = in.readLine();
                            String[] fragments = data.split(" ");
                            if (fragments.length >= 3) {
                                // prepare line to write
                                repository = fragments[0] + " " + fragments[1];
                            }
                        } finally {
                            in.close();
                        }
                        
                        // store new data in the file
                        storeFile(repo_file, repository + " " + s.repository);
                    } catch (IOException e) {
                        // can not read/write repository data
                        e.printStackTrace();
                    }
                }
            }
            
            if (s.packages != null) {
                // apply properties
                File packages_file = new File(mBasePath, "packages.install");
                storeFile(packages_file, s.packages);
            }
            
            if (s.monitor_nodes != null) {
                // apply properties
                File monitor_file = new File(mBasePath, "monitor-nodes.txt");
                storeFile(monitor_file, s.monitor_nodes);
            }
            
            if (s.qemu_template != null) {
                // apply properties
                File qemu_template = new File(mBasePath, "node-template.qemu.xml");
                storeFile(qemu_template, s.qemu_template);
            }
            
            if (s.vbox_template != null) {
                // apply properties
                File vbox_template = new File(mBasePath, "node-template.vbox.xml");
                storeFile(vbox_template, s.vbox_template);
            }
            
            // write configuration of 'base'
            base.save(conf);
        } catch (IOException e) {
            // can not load / save configuration
            e.printStackTrace();
        }
        
        // apply movement parameters
        if (s.mobility != null) {
            IniEditor sessionConf = new IniEditor();
            
            File sessionFile = new File(mPath, "config.properties");
            
            try {
                // load configuration of 'base'
                sessionConf.load(sessionFile);
                
                String section = null;
                
                // set mobility mode
                sessionConf.set("mobility", "model", s.mobility.model.toString());
                
                switch (s.mobility.model) {
                    case NONE:
                        break;
                    case RANDOM_WALK:
                        section = "randomwalk";
                        break;
                    case STATIC:
                        section = "staticconnections";
    
                        // store connections to file
                        if (s.mobility.parameters.containsKey("connections")) {
                            File f = new File(mPath, sessionConf.get(section, "connections"));
                            SessionContainer.storeFile(f, s.mobility.parameters.get("connections"));
                        }
                        break;
                    case THE_ONE:
                        section = "onetrace";
                        break;
                    default:
                        break;
                }
                
                if (section != null) {
                    for (Entry<String, String> e : s.mobility.parameters.entrySet()) {
                        // exclude 'connections'
                        if ("connections".equals(e.getKey()))
                            continue;
                        
                        if ((e.getValue() == null) || (e.getValue().length() == 0)) {
                            // remove parameter
                            sessionConf.remove(section, e.getKey());
                        } else {
                            // set parameter
                            sessionConf.set(section, e.getKey(), e.getValue());
                        }
                    }
                    
                    // store the configuration
                    sessionConf.save(sessionFile);
                }
            } catch (IOException e) {
                // can not load / save configuration
                e.printStackTrace();
            }
        }
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
    
    private static String loadFile(File f) {
        String ret = null;
        if (f.exists()) {
            ret = "";
            try {
                BufferedReader in = new BufferedReader(new FileReader(f));
                try {
                    String data = null;
                    while ((data = in.readLine()) != null) {
                        ret += data + "\n";
                    }
                } finally {
                    in.close();
                }
            } catch (IOException e) {
                // can not read monitor nodes
                e.printStackTrace();
                
                return null;
            }
        }
        return ret;
    }
    
    private static void storeFile(File f, String data) {
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(f));
            try {
                out.write(data);
                out.newLine();
                out.flush();
            } finally {
                out.close();
            }
        } catch (IOException e) {
            // can not read monitor nodes
            e.printStackTrace();
        }
    }
}
