package de.tubs.cs.ibr.hydra.webmanager.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.cpr.BroadcasterFactory;
import org.atmosphere.cpr.DefaultBroadcasterFactory;

import com.google.web.bindery.autobean.vm.AutoBeanFactorySource;

import de.tubs.cs.ibr.hydra.webmanager.server.data.Database;
import de.tubs.cs.ibr.hydra.webmanager.shared.Event;
import de.tubs.cs.ibr.hydra.webmanager.shared.EventExtra;
import de.tubs.cs.ibr.hydra.webmanager.shared.EventFactory;
import de.tubs.cs.ibr.hydra.webmanager.shared.EventType;
import de.tubs.cs.ibr.hydra.webmanager.shared.Slave;

public class MasterServer implements ServletContextListener {
    
    private static final HashMap<Long, SlaveConnection> mConnections = new HashMap<Long, SlaveConnection>();
    
    private ServerSocket mSockServer = null;
    private Boolean mRunning = true;
    
    private static ExecutorService mTaskLoop = null;
    
    public static ArrayList<Slave> getSlaves() {
        return Database.getInstance().getSlaves();
    }
    
    public static void register(Slave s, SlaveConnection sc) {
        Slave slave = Database.getInstance().getSlave(s.name, s.address);
        
        if (slave == null) {
            // create a new slave entry
            // TODO: add right owner
            slave = Database.getInstance().createSlave(s.name, s.address, null);
        }
        
        // update slave object
        sc.setSlave(slave);
        
        synchronized(mConnections) {
            mConnections.put(slave.id, sc);
            mConnections.notifyAll();
        }
        
        // set slave to 'idle' state
        slave.state = Slave.State.IDLE;
        Database.getInstance().updateSlave(slave);
        
        List<EventExtra> entries = new ArrayList<EventExtra>();
        entries.add(createEventExtra(EventType.EXTRA_SLAVE_ID, slave.id.toString()));
        entries.add(createEventExtra(EventType.EXTRA_SLAVE_NAME, slave.name));
        entries.add(createEventExtra(EventType.EXTRA_SLAVE_ADDRESS, slave.address));
        
        broadcast(EventType.SLAVE_STATE_CHANGED, entries);
    }

    public static void unregister(Slave s) {
        synchronized(mConnections) {
            mConnections.remove(s.id);
            mConnections.notifyAll();
        }
        
        // set slave to 'disconnected' state
        s.state = Slave.State.DISCONNECTED;
        Database.getInstance().updateSlave(s);
        
        List<EventExtra> entries = new ArrayList<EventExtra>();
        entries.add(createEventExtra(EventType.EXTRA_SLAVE_ID, s.id.toString()));
        entries.add(createEventExtra(EventType.EXTRA_SLAVE_NAME, s.name));
        entries.add(createEventExtra(EventType.EXTRA_SLAVE_ADDRESS, s.address));
        
        broadcast(EventType.SLAVE_STATE_CHANGED, entries);
    }
    
    private Thread mSocketLoop = new Thread() {

        @Override
        public void run() {
            try {
                // clean-up all old slave states
                Database.getInstance().resetSlaves();
                
                mSockServer = new ServerSocket(4244);
                
                System.out.println("Master service listening...");
                
                while (mRunning) {
                    // accept a new client connection
                    Socket client = mSockServer.accept();
                    
                    // create a new SlaveConnection object
                    SlaveConnection sc = new SlaveConnection(client);
                    
                    try {
                        // do handshake in the main-loop
                        Slave s = sc.doHandshake();
                        
                        // register new slave connection
                        register(s, sc);
                        
                        // start slave connection in a separate thread
                        sc.start();
                    } catch (IOException ex) {
                        // connection failed
                    }
                }
            } catch (IOException e) {
                System.out.println("Master service terminated.");
            }
        }
    };
    
    public static void invoke(Task t) {
        mTaskLoop.execute(t);
    }
    
    public static EventExtra createEventExtra(String key, String data) {
        EventFactory factory = AutoBeanFactorySource.create(EventFactory.class);
        EventExtra e = factory.eventextra().as();
        e.setKey(key);
        e.setData(data);
        return e;
    }
    
    public static void broadcast(EventType t, List<EventExtra> extras) {
        EventFactory factory = AutoBeanFactorySource.create(EventFactory.class);
        Event event = factory.event().as();
        
        event.setType(t);
        
        if (extras != null) {
            event.setExtras(extras);
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

    @Override
    public void contextDestroyed(ServletContextEvent evt) {
        mRunning = false;
        
        try {
            // close server socket
            mSockServer.close();
            
            // wait until the main thread is terminated
            mSocketLoop.join();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        synchronized(mConnections) {
            // shutdown all clients
            for (SlaveConnection sc : mConnections.values()) {
                try {
                    // close the connection to terminate the thread
                    sc.close();
                } catch (IOException e) {
                    // close failed
                    e.printStackTrace();
                }
            }
            
            try {
                // wait until all connections closed
                while (!mConnections.isEmpty()) {
                    mConnections.wait();
                }
                
                System.out.println("all slave connections closed");
            } catch (InterruptedException e) {
                // interrupted
                e.printStackTrace();
            }
        }
        
        // shutdown task loop
        mTaskLoop.shutdown();
        
        // close the global database
        Database.getInstance().close();
    }

    @Override
    public void contextInitialized(ServletContextEvent evt) {
        mSocketLoop.start();
        mTaskLoop = Executors.newSingleThreadExecutor();
    }
}
