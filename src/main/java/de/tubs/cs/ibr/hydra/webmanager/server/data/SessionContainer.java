package de.tubs.cs.ibr.hydra.webmanager.server.data;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
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
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.xeustechnologies.jtar.TarEntry;
import org.xeustechnologies.jtar.TarOutputStream;

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
            // load session configuration
            sessionConf.load(new File(mPath, "config.properties"));
            
            // load network configuration
            s.minaddr = sessionConf.get("network", "min_address");
            s.maxaddr = sessionConf.get("network", "max_address");
            s.netmask = sessionConf.get("network", "netmask");
            
            // set network 'default' parameters
            if ((s.minaddr == null) || (s.maxaddr == null) || (s.netmask == null)) {
                s.minaddr = "10.242.2.0";
                s.maxaddr = "10.242.255.254";
                s.netmask = "255.255.0.0";
            }
            
            // load stats configuration
            String si = sessionConf.get("stats", "collect_interval");
            s.stats_interval = (si == null) ? null : Long.valueOf(si);
            
            // load global simulation configuration
            String resolution = sessionConf.get("global", "resolution");
            s.resolution = (resolution == null) ? null : Double.valueOf(resolution);
            
            String range = sessionConf.get("global", "range");
            s.range = (range == null) ? null : Double.valueOf(range);
            
            // load mobility configuration
            MobilityParameterSet m = new MobilityParameterSet();
            if (sessionConf.hasOption("global", "mobility")) {
                m.model = MobilityModel.fromString(sessionConf.get("global", "mobility"));
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
        
        // save session properties
        IniEditor sessionConf = new IniEditor();
        File sessionFile = new File(mPath, "config.properties");
        
        try {
            // load configuration of 'base'
            sessionConf.load(sessionFile);
            
            // apply network configuration
            if (!sessionConf.hasSection("network")) sessionConf.addSection("network");
            
            if (s.minaddr != null) {
                if (s.minaddr.length() == 0) {
                    sessionConf.remove("network", "min_address");
                } else {
                    sessionConf.set("network", "min_address", s.minaddr.toString());
                }
            }
            
            if (s.maxaddr != null) {
                if (s.maxaddr.length() == 0) {
                    sessionConf.remove("network", "max_address");
                } else {
                    sessionConf.set("network", "max_address", s.maxaddr.toString());
                }
            }
            
            if (s.netmask != null) {
                if (s.netmask.length() == 0) {
                    sessionConf.remove("network", "netmask");
                } else {
                    sessionConf.set("network", "netmask", s.netmask.toString());
                }
            }
            
            // apply stats configuration
            if (!sessionConf.hasSection("stats")) sessionConf.addSection("stats");
            
            if (s.stats_interval != null) {
                if (s.stats_interval <= 0) {
                    sessionConf.remove("stats", "collect_interval");
                } else {
                    sessionConf.set("stats", "collect_interval", s.stats_interval.toString());
                }
            }
            
            // apply global simulation parameters
            if (!sessionConf.hasSection("global")) sessionConf.addSection("global");
            
            if (s.resolution != null) {
                if (s.resolution <= 0) {
                    sessionConf.remove("global", "resolution");
                } else {
                    sessionConf.set("global", "resolution", s.resolution.toString());
                }
            }
            
            if (s.range != null) {
                if (s.range <= 0) {
                    sessionConf.remove("global", "range");
                } else {
                    sessionConf.set("global", "range", s.range.toString());
                }
            }
            
            // apply movement parameters
            if (s.mobility != null) {
                String section = null;
                
                // set mobility mode
                if (!sessionConf.hasSection("global")) sessionConf.addSection("global");
                
                sessionConf.set("global", "mobility", s.mobility.model.toString());
                
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
                    if (!sessionConf.hasSection(section)) sessionConf.addSection(section);
                    
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
                }
            }
            
            // store the configuration
            sessionConf.save(sessionFile);
        } catch (IOException e) {
            // can not load / save configuration
            e.printStackTrace();
        }
    }
    
    public void deployArchive() throws IOException {
        // we need a valid base path
        if (mBasePath == null) return;
        
        // get the web-directory for this session
        File downloadDir = new File(Configuration.getWebDirectory(), "dl");
        File sessionDir = new File(downloadDir, mSessionKey);
        if (!sessionDir.exists()) sessionDir.mkdirs();
        
        // this is the target tar file
        File baseFile = new File(sessionDir, "base.tar.gz");
        
        // delete previous archive
        if (baseFile.exists()) baseFile.delete();
        
        // output file stream
        FileOutputStream dest = new FileOutputStream(baseFile);

        // create a TarOutputStream
        TarOutputStream out = new TarOutputStream( new BufferedOutputStream( dest ) );

        // collect files to tar
        List<File> list = new LinkedList<File>();
        
        // list all files in the base dir
        listFiles(mBasePath, list);
        
        for (File f : list) {
            String path = f.getCanonicalPath().replaceFirst(mBasePath.getCanonicalPath(), ".");
            
           out.putNextEntry(new TarEntry(f, path));
           BufferedInputStream origin = new BufferedInputStream(new FileInputStream( f ));

           int count;
           byte data[] = new byte[2048];
           while((count = origin.read(data)) != -1) {
              out.write(data, 0, count);
           }

           out.flush();
           origin.close();
        }

        out.close();
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
    
    private static void listFiles(File dir, List<File> list) {
        File[] files = dir.listFiles();
        
        for (File f : files) {
            if (f.isDirectory()) {
                listFiles(f, list);
            } else {
                list.add(f);
            }
        }
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
