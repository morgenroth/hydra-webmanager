package de.tubs.cs.ibr.hydra.webmanager.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.cpr.BroadcasterFactory;
import org.atmosphere.cpr.DefaultBroadcasterFactory;

import de.tubs.cs.ibr.hydra.webmanager.server.data.Database;
import de.tubs.cs.ibr.hydra.webmanager.server.data.NodeAddress;
import de.tubs.cs.ibr.hydra.webmanager.server.data.ServerEvent;
import de.tubs.cs.ibr.hydra.webmanager.server.data.SessionContainer;
import de.tubs.cs.ibr.hydra.webmanager.server.data.SlaveAllocation;
import de.tubs.cs.ibr.hydra.webmanager.shared.Coordinates;
import de.tubs.cs.ibr.hydra.webmanager.shared.Event;
import de.tubs.cs.ibr.hydra.webmanager.shared.EventType;
import de.tubs.cs.ibr.hydra.webmanager.shared.Link;
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
    
    private static class AddressPoolExhausedException extends DistributionFailedException {

        /**
         * serial ID
         */
        private static final long serialVersionUID = 6322066557716014141L;
        
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
        
        // set slave to 'connected' state
        Database.getInstance().updateSlave(slave, Slave.State.CONNECTED);
    }

    public static void unregister(Slave s) {
        synchronized(mConnections) {
            mConnections.remove(s.id);
            mConnections.notifyAll();
        }
        
        // set slave to 'disconnected' state
        Database.getInstance().updateSlave(s, Slave.State.DISCONNECTED);
    }
    
    
    public static SlaveConnection getSlaveConnection(Slave slave) {
        synchronized(mConnections) {
            return mConnections.get(slave.id);
        }
    }
    
    public static Set<Slave> tryDistribution(Session session) throws DistributionFailedException {
        // distribute all nodes of the session over the available slaves
        synchronized(mDistributionLock) {
            Database db = Database.getInstance();
            
            // get all nodes of this session
            ArrayList<Node> nodes = db.getNodes(session);
            
            // get allocations
            List<SlaveAllocation> allocs = db.getSlaveAllocation();
            
            // get sum of free slots
            Long sum = 0L;
            for (SlaveAllocation a : allocs) {
                sum += a.getAvailableSlots();
            }
            
            // check if distribution is possible without changing any assignment
            if (sum < nodes.size()) {
                throw new DistributionFailedException();
            }
            
            /**
             * we found enough free slots to start this session
             * assign the nodes to available slaves
             */
            
            // get IP range of this session
            final NodeAddress baseAddr = new NodeAddress(session.minaddr, session.netmask);
            final NodeAddress maxAddr = new NodeAddress(session.maxaddr, session.netmask);
            
            try {
                // return the slaves used for this session
                Set<Slave> allocSlaves = new HashSet<Slave>();
                
                // put all nodes into a queue
                LinkedList<Node> nodeQueue = new LinkedList<Node>(nodes);
                
                // get all allocated addresses
                Set<String> allocAddresses = db.getAddressAllocation();
                
                // first try to allocate pinned assignments
                Iterator<Node> iter = nodeQueue.iterator();
                while (iter.hasNext()) {
                    Node n = iter.next();
                    
                    // if this node has a pinned assignment
                    if (n.slaveId == null) continue;
                    
                    // search for the allocation of this slave
                    for (SlaveAllocation a : allocs) {
                        if (n.slaveId != a.slaveId) continue;
                        
                        // if the pinned slave is exhausted, skip this allocation
                        if (a.isExhausted()) break;
                        
                        // get the slave object
                        Slave s = db.getSlave(a.slaveId);
                        
                        // assign the slave to the node
                        db.assignNode(n, s);
                        
                        // assign unique IP addresses
                        db.updateNode(n, findFreeAddress(allocAddresses, baseAddr, maxAddr));
                        
                        // set the new node state
                        db.updateNode(n, Node.State.SCHEDULED);
                        
                        // add one element to the allocation of this slave
                        a.allocation++;
                        
                        // remove this node
                        iter.remove();
                    }
                }
                
                // sort allocation data by number of occupied slots, but prefer large capacities
                Collections.sort(allocs, new Comparator<SlaveAllocation>() {
                    @Override
                    public int compare(SlaveAllocation o1, SlaveAllocation o2) {
                        int allocComp = o1.allocation.compareTo(o2.allocation);
                        
                        if (allocComp == 0) {
                            return (o1.capacity.compareTo(o2.capacity) * -1);
                        }
                        
                        return allocComp;
                    }
                });
                
                // prefer slaves with a small number of running nodes
                for (SlaveAllocation a : allocs) {
                    if (!a.isExhausted()) {
                        // abort if all nodes are allocated
                        if (nodeQueue.isEmpty()) break;
                        
                        // get the slave object
                        Slave s = db.getSlave(a.slaveId);
                        
                        // add this slave to the allocated slaves
                        allocSlaves.add(s);
                     
                        while (!nodeQueue.isEmpty() && !a.isExhausted()) {
                            // get the next node object
                            Node n = nodeQueue.poll();
                            
                            // assign the slave to the node
                            db.assignNode(n, s);
                            
                            // assign unique IP addresses
                            db.updateNode(n, findFreeAddress(allocAddresses, baseAddr, maxAddr));
                            
                            // set the new node state
                            db.updateNode(n, Node.State.SCHEDULED);
                            
                            // add one element to the allocation of this slave
                            a.allocation++;
                        }
                    }
                }
                
                return allocSlaves;
            } catch (AddressPoolExhausedException e) {
                // error - address pool exhausted
                System.err.println("ERROR - address pool is exhausted");
                
                // clear made assignments
                db.clearAssignment(session);
                
                // update views
                MasterServer.fireNodeStateChanged(null);
                
                // re-throw the exception
                throw e;
            }
        }
    }
    
    private static String findFreeAddress(Set<String> allocAddresses, final NodeAddress baseAddr, final NodeAddress maxAddr) throws AddressPoolExhausedException {
        for (long n = baseAddr.getLongAddress() + 1; n < maxAddr.getLongAddress(); n++) {
            NodeAddress newAddr = new NodeAddress(n, baseAddr.getLongNetmask());
            String addr = newAddr.toString();
            if (!allocAddresses.contains(addr)) {
                // add new address to the set of allocated addresses
                allocAddresses.add(addr);

                // return the new address
                return addr; 
            }
        }
        
        // no more addresses available
        throw new AddressPoolExhausedException();
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
    
    public static void broadcast(final Event evt) {
        // distribute event
        mTaskLoop.execute(new Task() {
            @Override
            public void run() {
                try {
                    // notify all event listener
                    synchronized(mEventListeners) {
                        for (EventListener l : mEventListeners) {
                            l.onEvent(evt);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        
        // get/create atmosphere broadcast channel
        BroadcasterFactory bf = DefaultBroadcasterFactory.getDefault();
        
        if (bf != null) {
            Broadcaster channel = bf.lookup("/events", true);
            
            // broadcast the event to the clients
            channel.broadcast(evt.getEventData());
        }
    }
    
    private static void broadcast(Long sessionId, final Event evt) {
        // get/create atmosphere broadcast channel
        BroadcasterFactory bf = DefaultBroadcasterFactory.getDefault();
        
        if (bf != null) {
            Broadcaster channel = bf.lookup("/session/" + sessionId.toString(), true);
            
            // broadcast the event to the clients
            channel.broadcast(evt.getEventData());
        }
    }
    
    public static void firePositionUpdated(final Session s, final Node n, final Coordinates position) {
        // generate a new event
        Event e = new ServerEvent(EventType.SESSION_NODE_MOVED);
        
        e.setExtra(EventType.EXTRA_SESSION_ID, s.id.toString());
        e.setExtra(EventType.EXTRA_NODE_ID, n.id.toString());
        e.setExtra(EventType.EXTRA_POSITION_X, Double.toString(position.getX()));
        e.setExtra(EventType.EXTRA_POSITION_Y, Double.toString(position.getY()));
        
        // broadcast session change
        MasterServer.broadcast(s.id, e);
    }
    
    public static void fireLinkUp(final Session s, final Link l) {
        // generate a new event
        Event e = new ServerEvent(EventType.SESSION_LINK_UP);

        e.setExtra(EventType.EXTRA_SESSION_ID, s.id.toString());
        e.setExtra(EventType.EXTRA_LINK_SOURCE_ID, l.source.id.toString());
        e.setExtra(EventType.EXTRA_LINK_TARGET_ID, l.target.id.toString());
        
        // broadcast session change
        MasterServer.broadcast(s.id, e);
    }
    
    public static void fireLinkDown(final Session s, final Link l) {
        // generate a new event
        Event e = new ServerEvent(EventType.SESSION_LINK_DOWN);
        
        e.setExtra(EventType.EXTRA_SESSION_ID, s.id.toString());
        e.setExtra(EventType.EXTRA_LINK_SOURCE_ID, l.source.id.toString());
        e.setExtra(EventType.EXTRA_LINK_TARGET_ID, l.target.id.toString());
        
        // broadcast session change
        MasterServer.broadcast(s.id, e);
    }
    
    public static void fireSessionDataUpdated(final Session s) {
        // generate a new event
        Event e = new ServerEvent(EventType.SESSION_DATA_UPDATED);
        
        e.setExtra(EventType.EXTRA_SESSION_ID, s.id.toString());
        
        // broadcast session change
        MasterServer.broadcast(e);
    }
    
    public static void fireSessionStatsUpdated(final Session s) {
        // generate a new event
        Event e = new ServerEvent(EventType.SESSION_STATS_UPDATED);
        
        e.setExtra(EventType.EXTRA_SESSION_ID, s.id.toString());
        
        // broadcast session change
        MasterServer.broadcast(e);
    }
    
    public static void fireNodeStateChanged(final Node n) {
        // generate a new event
        Event e = new ServerEvent(EventType.NODE_STATE_CHANGED);
        
        if (n != null) {
            e.setExtra(EventType.EXTRA_NODE_ID, n.id.toString());
            e.setExtra(EventType.EXTRA_SESSION_ID, n.sessionId.toString());
            e.setExtra(EventType.EXTRA_NODE_STATE, n.state.toString());
        }
        
        MasterServer.broadcast(e);
    }
    
    public static void fireSlaveStateChanged(final Slave s) {
        // generate a new event
        Event e = new ServerEvent(EventType.SLAVE_STATE_CHANGED);
        
        e.setExtra(EventType.EXTRA_SLAVE_ID, s.id.toString());
        e.setExtra(EventType.EXTRA_SLAVE_STATE, s.state.toString());
        
        MasterServer.broadcast(e);
    }
    
    public static void fireSessionAdded(final Session s) {
        // generate a new event
        Event e = new ServerEvent(EventType.SESSION_ADDED);
        
        e.setExtra(EventType.EXTRA_SESSION_ID, s.id.toString());
        
        MasterServer.broadcast(e);
    }
    
    public static void fireSessionRemoved(final Session s) {
        // generate a new event
        Event e = new ServerEvent(EventType.SESSION_REMOVED);
        
        e.setExtra(EventType.EXTRA_SESSION_ID, s.id.toString());
        
        MasterServer.broadcast(e);
    }
    
    public static void fireSessionStateChanged(final Session s) {
        // generate a new event
        Event e = new ServerEvent(EventType.SESSION_STATE_CHANGED);
        
        e.setExtra(EventType.EXTRA_SESSION_ID, s.id.toString());
        e.setExtra(EventType.EXTRA_NEW_STATE, s.state.toString());
        
        // broadcast session change
        MasterServer.broadcast(e);
        
        /*** TRIGGER LOCAL ACTIONS ***/
        if (Session.State.PENDING.equals(s.state)) {
            mTaskLoop.execute(new Task() {
                @Override
                public void run() {
                    SessionController sc = null;
                    
                    synchronized(mControllers) {
                        // check if the session controller for this session already exists
                        if (mControllers.containsKey(s.id)) {
                            // ERROR
                            System.err.println("Session changed to PENDING, but there is already an active session controller.");
                            return;
                        }
                        
                        // create a session controller
                        sc = new SessionController(s);
                        
                        // add the new session controller 
                        mControllers.put(s.id, sc);
                    }
                    
                    // initiate the session controller
                    sc.initiate();
                }
            });
        }
        else if (Session.State.CANCELLED.equals(s.state)) {
            mTaskLoop.execute(new Task() {
                @Override
                public void run() {
                    SessionController sc = null;
                    
                    synchronized(mControllers) {
                        // check if the session controller for this session already exists
                        if (!mControllers.containsKey(s.id)) {
                            // ERROR
                            System.err.println("Session changed to CANCELLED, but no session controller found.");
                            return;
                        }
                        
                        // create a session controller
                        sc = mControllers.get(s.id);
                    }

                    // cancel the session
                    sc.cancel();
                }
            });
        }
        else if (Session.State.ABORTED.equals(s.state)) {
            mTaskLoop.execute(new Task() {
                @Override
                public void run() {
                    SessionController sc = null;
                    
                    synchronized(mControllers) {
                        // check if the session controller for this session already exists
                        if (!mControllers.containsKey(s.id)) {
                            // ERROR
                            System.err.println("Session changed to ABORTED, but no session controller found.");
                            return;
                        }
                        
                        // create a session controller
                        sc = mControllers.get(s.id);
                    }
                    
                    // abort the session
                    sc.abort();
                }
            });
        }
    }
    
    public static void onSessionFinished(Session s) {
        // clear all assignments of this session
        Database.getInstance().clearAssignment(s);
        
        // update views
        MasterServer.fireNodeStateChanged(null);
        
        synchronized(mControllers) {
            // remove the controller
            mControllers.remove(s.id);
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
        
        // reset all sessions
        Database.getInstance().resetSessions();
        
        // clear all node assignments
        Database.getInstance().clearAssignment(null);
    }
    
    private static SessionController getController(Long sessionId) {
        synchronized(mControllers) {
            // check if the session controller for this session already exists
            if (!mControllers.containsKey(sessionId)) {
                // ERROR
                System.err.println("No session controller found for " + sessionId.toString());
                return null;
            }
            
            // retrieve the session controller
            return mControllers.get(sessionId);
        }
    }
    
    public static ArrayList<Link> getLinks(Long sessionId) {
        SessionController sc = getController(sessionId);
        
        if (sc == null) return new ArrayList<Link>();

        // get session links
        return sc.getLinks();
    }
    
    public static ArrayList<Node> getNodes(Session session) {
        // get session controller
        SessionController sc = getController(session.id);
        
        if (sc != null) {
            Collection<Node> nodes = sc.getNodes();
            if (nodes != null) {
                return new ArrayList<Node>(nodes);
            }
        }
        
        ArrayList<Node> nodes = Database.getInstance().getNodes(session);
        
        // get up-to-date session object
        Session s = getSession(session.id);
        
        for (Node n : nodes) {
            // add session specific default values
            n.range = s.range;
        }
        
        return nodes;
    }
    
    public static Session getSession(Long id) {
        final Session s = Database.getInstance().getSession(id);
        
        // get a session container
        final SessionContainer sc = SessionContainer.getContainer(s);
        
        try {
            // initialize the container
            sc.initialize(null);
            
            // inject container parameters
            sc.inject(s);
        } catch (IOException e) {
            if (!Session.State.INITIAL.equals(s.state)) {
                // container not ready
                // set session state to INITIAL
                Database.getInstance().setState(s, Session.State.INITIAL);
                
                MasterServer.invoke(new Task() {
                    @Override
                    public void run() {
                        try {
                            // trigger initialization of the session
                            sc.initialize(SessionContainer.getDefault());
                            
                            // set session state to DRAFT
                            Database.getInstance().setState(s, Session.State.DRAFT);
                        } catch (IOException e) {
                            // set session state to ERROR
                            Database.getInstance().setState(s, Session.State.ERROR);
                        }
                    }
                });
            }
        }
        
        return s;
    }
}
