package de.tubs.cs.ibr.hydra.webmanager.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.cpr.BroadcasterFactory;
import org.atmosphere.cpr.DefaultBroadcasterFactory;

import de.tubs.cs.ibr.hydra.webmanager.shared.Event;
import de.tubs.cs.ibr.hydra.webmanager.shared.Slave;
import de.tubs.cs.ibr.hydra.webmanager.shared.Event.EventType;

public class MasterServer extends GenericServlet {
    
    private static ConcurrentHashMap<String, Slave> mSlaves = new ConcurrentHashMap<String, Slave>();
    private static ConcurrentHashMap<String, SlaveConnection> mConnections = new ConcurrentHashMap<String, SlaveConnection>();
    
    private ServerSocket mSockServer = null;
    private Boolean mRunning = true;
    
    public static ArrayList<Slave> getSlaves() {
        ArrayList<Slave> ret = new ArrayList<Slave>();
        
        for (Slave s : mSlaves.values()) {
            ret.add(s);
        }
        
        return ret;
    }
    
    public static SlaveConnection getConnection(Slave s) {
        return mConnections.get(s.toString());
    }

    public static void register(Slave s, SlaveConnection sc) {
        mSlaves.put(s.toString(), s);
        mConnections.put(s.toString(), sc);
        
        broadcast(new Event(EventType.SLAVE_CONNECTED));
    }

    public static void unregister(Slave s) {
        mSlaves.remove(s.toString());
        mConnections.remove(s.toString());
        
        broadcast(new Event(EventType.SLAVE_DISCONNECTED));
    }
    
    private Thread mMasterLoop = new Thread() {

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
        mMasterLoop.start();
        System.out.println("Master service initialized.");
    }
    
    public static void broadcast(Event evt) {
        // get/create atmosphere broadcast channel
        BroadcasterFactory bf = DefaultBroadcasterFactory.getDefault();
        Broadcaster channel = bf.lookup("events", true);
        
        // broadcast the event to the clients
        channel.broadcast(evt);
    }
}
