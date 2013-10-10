package de.tubs.cs.ibr.hydra.webmanager.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
import de.tubs.cs.ibr.hydra.webmanager.shared.Node;
import de.tubs.cs.ibr.hydra.webmanager.shared.Session;
import de.tubs.cs.ibr.hydra.webmanager.shared.Slave;

public class MasterServer implements ServletContextListener {
    
    public static class DistributionFailedException extends Exception {

        /**
         * serial ID
         */
        private static final long serialVersionUID = 2242070903678118836L;
        
    };
    
    public interface EventListener {
        void onEvent(Event evt);
    };
    
    private static final HashMap<Long, SlaveConnection> mConnections = new HashMap<Long, SlaveConnection>();
    
    private ServerSocket mSockServer = null;
    private Boolean mRunning = true;
    
    private static ExecutorService mTaskLoop = null;
    
    private static HashSet<EventListener> mEventListeners = new HashSet<EventListener>();
    
    // hash-map for all session controller, the key is the session id
    private static HashMap<Long, SessionController> mControllers = new HashMap<Long, SessionController>();
    
    // this object is used to lock the distribution methods against parallel execution
    private static Object mDistributionLock = new Object();
    
    public static ArrayList<Slave> getSlaves() {
        return Database.getInstance().getSlaves();
    }
    
    public static void registerEventListener(EventListener e) {
        synchronized(mEventListeners) {
            mEventListeners.add(e);
        }
    }
    
    public static void unregisterEventListener(EventListener e) {
        synchronized(mEventListeners) {
            mEventListeners.remove(e);
        }
    }
    
    public static void register(Slave s, SlaveConnection sc) {
        Slave slave = Database.getInstance().getSlave(s.name, s.address);
        
        if (slave == null) {
            // create a new slave entry
            slave = Database.getInstance().createSlave(s.name, s.address, s.owner, s.capacity);
        } else {
            // update slave's owner and capacity
            Database.getInstance().updateSlave(slave, s.owner, s.capacity);
        }
        
        // update slave object
        sc.setSlave(slave);
        
        synchronized(mConnections) {
            mConnections.put(slave.id, sc);
            mConnections.notifyAll();
        }
        
        // set slave to 'idle' state
        Database.getInstance().updateSlave(slave, Slave.State.IDLE);
    }

    public static void unregister(Slave s) {
        synchronized(mConnections) {
            mConnections.remove(s.id);
            mConnections.notifyAll();
        }
        
        // set slave to 'disconnected' state
        Database.getInstance().updateSlave(s, Slave.State.DISCONNECTED);
    }
    
    public static boolean tryDistribution(Session s) throws DistributionFailedException {
        // distribute all nodes of the session over the available slaves
        synchronized(mDistributionLock) {
            Database db = Database.getInstance();
            
            // get all nodes of this session
            ArrayList<Node> nodes = db.getNodes(s.id);
            
            // TODO: check if distribution is possible without changing any assignment
            throw new DistributionFailedException();
            
            // TODO: assign the nodes to available slaves
            
            // TODO: assign unique IP addresses
        }
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
    
    private static EventExtra createEventExtra(String key, String data) {
        EventFactory factory = AutoBeanFactorySource.create(EventFactory.class);
        EventExtra e = factory.eventextra().as();
        e.setKey(key);
        e.setData(data);
        return e;
    }
    
    private static void broadcast(EventType t, List<EventExtra> extras) {
        EventFactory factory = AutoBeanFactorySource.create(EventFactory.class);
        Event event = factory.event().as();
        
        event.setType(t);
        
        if (extras != null) {
            event.setExtras(extras);
        }
        
        broadcast(event);
    }
    
    private static void broadcast(final Event evt) {
        // distribute event
        mTaskLoop.execute(new Task() {
            @Override
            public void run() {
                // notify all event listener
                synchronized(mEventListeners) {
                    for (EventListener l : mEventListeners) {
                        l.onEvent(evt);
                    }
                }
            }
        });
        
        // get/create atmosphere broadcast channel
        BroadcasterFactory bf = DefaultBroadcasterFactory.getDefault();
        Broadcaster channel = bf.lookup("events", true);
        
        // broadcast the event to the clients
        channel.broadcast(evt);
    }
    
    public static void fireSessionDataUpdated(final Session s) {
        List<EventExtra> entries = new ArrayList<EventExtra>();
        entries.add(MasterServer.createEventExtra(EventType.EXTRA_SESSION_ID, s.id.toString()));
        
        // broadcast session change
        MasterServer.broadcast(EventType.SESSION_DATA_UPDATED, entries);
    }
    
    public static void fireNodeStateChanged(final Node n) {
        List<EventExtra> entries = null;
        
        if (n != null) {
            entries = new ArrayList<EventExtra>();
            entries.add(MasterServer.createEventExtra(EventType.EXTRA_NODE_ID, n.id.toString()));
            entries.add(MasterServer.createEventExtra(EventType.EXTRA_SESSION_ID, n.sessionId.toString()));
            entries.add(MasterServer.createEventExtra(EventType.EXTRA_NODE_STATE, n.state.toString()));
        }
        
        MasterServer.broadcast(EventType.NODE_STATE_CHANGED, entries);
    }
    
    public static void fireSlaveStateChanged(final Slave s) {
        List<EventExtra> entries = new ArrayList<EventExtra>();
        entries.add(MasterServer.createEventExtra(EventType.EXTRA_SLAVE_ID, s.id.toString()));
        entries.add(MasterServer.createEventExtra(EventType.EXTRA_SLAVE_STATE, s.state.toString()));
        
        MasterServer.broadcast(EventType.SLAVE_STATE_CHANGED, entries);
    }
    
    public static void fireSessionAdded(final Session s) {
        List<EventExtra> entries = new ArrayList<EventExtra>();
        entries.add(MasterServer.createEventExtra(EventType.EXTRA_SESSION_ID, s.id.toString()));
        
        MasterServer.broadcast(EventType.SESSION_ADDED, entries);
    }
    
    public static void fireSessionRemoved(final Session s) {
        List<EventExtra> entries = new ArrayList<EventExtra>();
        entries.add(MasterServer.createEventExtra(EventType.EXTRA_SESSION_ID, s.id.toString()));
        
        MasterServer.broadcast(EventType.SESSION_REMOVED, entries);
    }
    
    public static void fireSessionStateChanged(final Session s) {
        List<EventExtra> entries = new ArrayList<EventExtra>();
        entries.add(MasterServer.createEventExtra(EventType.EXTRA_SESSION_ID, s.id.toString()));
        entries.add(MasterServer.createEventExtra(EventType.EXTRA_NEW_STATE, s.state.toString()));
        
        // broadcast session change
        MasterServer.broadcast(EventType.SESSION_STATE_CHANGED, entries);
        
        /*** TRIGGER LOCAL ACTIONS ***/
        if (Session.State.PENDING.equals(s.state)) {
            mTaskLoop.execute(new Task() {
                @Override
                public void run() {
                    // check if the session controller for this session already exists
                    if (mControllers.containsKey(s.id)) {
                        // ERROR
                        System.err.println("Session changed to PENDING, but there is already an active session controller.");
                        return;
                    }
                    
                    // create a session controller
                    SessionController sc = new SessionController(s);
                    
                    // add the new session controller 
                    mControllers.put(s.id, sc);
                    
                    // initiate the session controller
                    sc.initiate();
                }
            });
        }
        else if (Session.State.CANCELLED.equals(s.state)) {
            mTaskLoop.execute(new Task() {
                @Override
                public void run() {
                    // check if the session controller for this session already exists
                    if (!mControllers.containsKey(s.id)) {
                        // ERROR
                        System.err.println("Session changed to CANCELLED, but no session controller found.");
                        return;
                    }
                    
                    // create a session controller
                    SessionController sc = mControllers.get(s.id);

                    // shutdown the session controller
                    sc.terminate();
                    
                    // remove the controller
                    mControllers.remove(s.id);
                }
            });
        }
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
