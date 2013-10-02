package de.tubs.cs.ibr.hydra.webmanager.server;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.cpr.BroadcasterFactory;
import org.atmosphere.cpr.DefaultBroadcasterFactory;

import com.google.web.bindery.autobean.vm.AutoBeanFactorySource;

import de.tubs.cs.ibr.hydra.webmanager.shared.Event;
import de.tubs.cs.ibr.hydra.webmanager.shared.EventEntry;
import de.tubs.cs.ibr.hydra.webmanager.shared.EventFactory;
import de.tubs.cs.ibr.hydra.webmanager.shared.EventType;
import de.tubs.cs.ibr.hydra.webmanager.shared.Slave;

public class MasterServer extends GenericServlet {
    
    private static ConcurrentHashMap<String, Slave> mSlaves = new ConcurrentHashMap<String, Slave>();
    private static ConcurrentHashMap<String, SlaveConnection> mConnections = new ConcurrentHashMap<String, SlaveConnection>();
    
    // TODO: make this configurable
    private static File mImagesPath = new File("/home/morgenro/sources/hydrasim.git/master/htdocs/dl");
    
    private ServerSocket mSockServer = null;
    private Boolean mRunning = true;
    
    private static ExecutorService mTaskLoop = null;
    
    public static ArrayList<Slave> getSlaves() {
        ArrayList<Slave> ret = new ArrayList<Slave>();
        
        for (Slave s : mSlaves.values()) {
            ret.add(s);
        }
        
        return ret;
    }
    
    public static ArrayList<String> getAvailableImages() {
        ArrayList<String> ret = new ArrayList<String>();
        
        String[] images = mImagesPath.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                if (name.endsWith(".img.gz")) return true;
                return false;
            }
        });
        
        for (String f : images) {
            ret.add(f);
        }
        
        return ret;
    }
    
    public static SlaveConnection getConnection(Slave s) {
        return mConnections.get(s.toString());
    }

    public static void register(Slave s, SlaveConnection sc) {
        mSlaves.put(s.toString(), s);
        mConnections.put(s.toString(), sc);
        
        broadcast(EventType.SLAVE_CONNECTED, null);
    }

    public static void unregister(Slave s) {
        mSlaves.remove(s.toString());
        mConnections.remove(s.toString());
        
        broadcast(EventType.SLAVE_DISCONNECTED, null);
    }
    
    private Thread mSocketLoop = new Thread() {

        @Override
        public void run() {
            try {
                mSockServer = new ServerSocket(4244);
                
                while (mRunning) {
                    // accept a new client connection
                    Socket client = mSockServer.accept();
                    
                    // create a new SlaveConnection object
                    SlaveConnection slave = new SlaveConnection(client);
                    
                    // start slave connection in a separate thread
                    slave.start();
                }
            } catch (IOException e) {
                // failed to create a server socket
                e.printStackTrace();
            } finally {
                try {
                    mSockServer.close();
                } catch (IOException e) {
                    // error while closing server socket
                }
            }
            
            // shutdown task loop
            mTaskLoop.shutdown();
        }
    };

    /**
     * Serial ID
     */
    private static final long serialVersionUID = -408760991182258654L;

    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
    }

    @Override
    public void init() throws ServletException {
        mSocketLoop.start();
        mTaskLoop = Executors.newSingleThreadExecutor();
        System.out.println("Master service initialized.");
    }
    
    public static void invoke(Task t) {
        mTaskLoop.execute(t);
    }
    
    public static void broadcast(EventType t, ArrayList<EventEntry> entries) {
        EventFactory factory = AutoBeanFactorySource.create(EventFactory.class);
        Event event = factory.event().as();
        
        event.setType(t);
        
        if (entries != null) {
            event.setEntries(entries);
        }
        
        broadcast(event);
    }
    
    public static void broadcast(Event evt) {
        // get/create atmosphere broadcast channel
        BroadcasterFactory bf = DefaultBroadcasterFactory.getDefault();
        Broadcaster channel = bf.lookup("events", true);
        
        // broadcast the event to the clients
        channel.broadcast(evt);
    }
}
