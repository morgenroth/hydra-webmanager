package de.tubs.cs.ibr.hydra.webmanager.server;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import de.tubs.cs.ibr.hydra.webmanager.server.SlaveExecutor.OperationFailedException;
import de.tubs.cs.ibr.hydra.webmanager.server.data.Configuration;
import de.tubs.cs.ibr.hydra.webmanager.server.data.Database;
import de.tubs.cs.ibr.hydra.webmanager.server.data.SessionContainer;
import de.tubs.cs.ibr.hydra.webmanager.server.movement.ContactProvider;
import de.tubs.cs.ibr.hydra.webmanager.server.movement.MovementProvider;
import de.tubs.cs.ibr.hydra.webmanager.server.movement.MovementProvider.MovementFinishedException;
import de.tubs.cs.ibr.hydra.webmanager.server.movement.NullMovement;
import de.tubs.cs.ibr.hydra.webmanager.server.movement.RandomWalkMovement;
import de.tubs.cs.ibr.hydra.webmanager.server.movement.RandomWaypointMovement;
import de.tubs.cs.ibr.hydra.webmanager.server.movement.StaticMovement;
import de.tubs.cs.ibr.hydra.webmanager.server.movement.TraceMovement;
import de.tubs.cs.ibr.hydra.webmanager.shared.Coordinates;
import de.tubs.cs.ibr.hydra.webmanager.shared.Event;
import de.tubs.cs.ibr.hydra.webmanager.shared.EventType;
import de.tubs.cs.ibr.hydra.webmanager.shared.GeoCoordinates;
import de.tubs.cs.ibr.hydra.webmanager.shared.Link;
import de.tubs.cs.ibr.hydra.webmanager.shared.Node;
import de.tubs.cs.ibr.hydra.webmanager.shared.Session;
import de.tubs.cs.ibr.hydra.webmanager.shared.Slave;

public class SessionController {
    
    // logger for this session
    Logger mLogger = null;
    
    // movement model / controller
    MovementProvider mMovement = null;
    
    // contact provider
    ContactProvider mContactProvider = null;
    
    // session object
    Session mSession = null;
    
    // session container (configuration)
    SessionContainer mContainer = null;
    
    // list of all slaves used for this session
    Set<Slave> mSlaves = new HashSet<Slave>();
    
    // main executor
    ScheduledExecutorService mExecutor = Executors.newSingleThreadScheduledExecutor();
    
    // executor for distribution of slave tasks
    SlaveExecutor mSlaveExecutor = new SlaveExecutor(mExecutor);
    
    ScheduledFuture<?> scheduledDistribution = null;
    ScheduledFuture<?> scheduledStatsCollector = null;
    ScheduledFuture<?> scheduledMovement = null;
    ScheduledFuture<?> scheduledTrafficGeneration = null;
    
    // trace recorder
    TraceRecorder mRecorderContact = null;
    TraceRecorder mRecorderMovement = null;
    
    public SessionController(Session s) {
        mLogger = Logger.getLogger(SessionController.class.getSimpleName() + "[" +s.id.toString() + "]");
        mSession = s;
    }
    
    public synchronized ArrayList<Link> getLinks() {
        if (mContactProvider == null) return null;
        return mContactProvider.getLinks();
    }
    
    public synchronized Collection<Node> getNodes() {
        if (mMovement == null) return null;
        return mMovement.getNodes();
    }
    
    public synchronized Coordinates getPosition(Node n) {
        if (mMovement == null) return null;
        return mMovement.getPosition(n);
    }
    
    private MasterServer.EventListener mEventListener = new MasterServer.EventListener() {
        
        @Override
        public void onEvent(Event evt) {
            if (evt.equals(EventType.SLAVE_STATE_CHANGED)) {
                // if we are current try to distribute this session
                if (scheduledDistribution != null) {
                    // cancel scheduled distribution
                    scheduledDistribution.cancel(false);
                    
                    // try to distribute the session now
                    mExecutor.execute(mRunnableDistribute);
                }
            }
        }
    };
    
    public void initiate() {
        if (!Session.State.PENDING.equals(getSession().state)) {
            // only start when session state is pending
            return;
        }
        
        // register event listener
        MasterServer.registerEventListener(mEventListener);
        
        // create an archive and deploy it to the webserver directory
        try {
            mContainer = SessionContainer.getContainer(mSession);
            
            // trigger initialization of the session
            mContainer.initialize(null);

            // inject container parameters
            mContainer.inject(mSession);
            
            // try to distribute the session now
            mExecutor.execute(mRunnableDistribute);
        } catch (IOException e) {
            mLogger.severe(e.toString());
            
            // switch state to error
            setSessionState(Session.State.ERROR);
        }
    }
    
    public void close() {
        // shutdown and clean-up waste
        onDestroy();
        
        // wait until all task are done
        try {
            mExecutor.awaitTermination(5, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            mLogger.warning("Interrupted during cancel()");
        }
    }
    
    private void onDestroy() {
        // un-register event listener
        MasterServer.unregisterEventListener(mEventListener);
        
        // cancel stats collection
        if (scheduledStatsCollector != null) {
            scheduledStatsCollector.cancel(false);
        }
        
        // cancel movement updates
        if (scheduledMovement != null) {
            scheduledMovement.cancel(false);
        }
        
        // cancel traffic generation
        if (scheduledTrafficGeneration != null) {
            scheduledTrafficGeneration.cancel(false);
        }
        
        // cancel scheduled distribution
        if (scheduledDistribution != null) {
            // we are in distribution phase
            scheduledDistribution.cancel(false);
        }
        
        try {
            // schedule destroy task to clean-up all slaves
            // timeout: 30 seconds per slave
            Future<Integer> destroy = mSlaveExecutor.submit(mSlaves, mDestroyTask, 30 * mSlaves.size());
            
            // wait until the destroy task has been completed
            destroy.get();
            
            // shutdown main executor
            mExecutor.shutdown();
            
            // shutdown slave executor
            mSlaveExecutor.awaitShutdown();
        } catch (InterruptedException e) {
            mLogger.warning("Interrupted during onDestroy(): " + e.toString());
        } catch (ExecutionException e) {
            mLogger.severe("Execution failed during onDestroy(): " + e.toString());
        }
        
        // close trace recorder
        try {
            if (mRecorderContact != null) {
                mRecorderContact.close();
                mRecorderContact = null;
            }
            
            if (mRecorderMovement != null) {
                mRecorderMovement.close();
                mRecorderMovement = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        // close movement
        if (mMovement instanceof Closeable) {
            try {
                ((Closeable)mMovement).close();
            } catch (IOException e) {
                // error on close
                e.printStackTrace();
            }
        }
        
        // clean-up from MasterServer
        MasterServer.onSessionFinished(mSession);
    }
    
    /**
     * This task prepares the sessions on the slaves
     */
    private SlaveExecutor.SlaveRunnable mPrepareTask = new SlaveExecutor.SlaveRunnable() {
        
        @Override
        public void run(SlaveConnection c, Slave s) throws OperationFailedException {
            try {
                // create the session on the slave
                c.createSession(mSession, Configuration.getWebLocation());
                
                // prepare the session
                c.prepareSession(mSession);
            } catch (Exception e) {
                throw new OperationFailedException(e);
            }
        }
        
        @Override
        public void onTimeout() {
            mLogger.warning("preparation timed out");
            
            // switch state to error
            setSessionState(Session.State.ERROR);
        }
        
        @Override
        public void onPrepare() throws OperationFailedException {
            // check if not aborted
            if (isAborted()) {
                throw new OperationFailedException("prepare aborted due to aborted session");
            }
            
            // create an archive and deploy it to the webserver directory
            try {
                // deploy the web archive
                mContainer.deployArchive();
            } catch (IOException e) {
                throw new OperationFailedException(e);
            }
        }
        
        @Override
        public void onFinish() {
            mLogger.fine("prepared");
            
            // get the database object
            Database db = Database.getInstance();
            
            // get all nodes of this session
            List<Node> nodes = db.getNodes(mSession);
            
            // schedule a preparation
            // timeout: 30 seconds per node
            mSlaveExecutor.execute(mSlaves, nodes, mCreateNodesTask, 10 * nodes.size());
        }

        @Override
        public void onError(Exception e) {
            mLogger.severe("preparation failed " + e.toString());

            // switch state to error
            setSessionState(Session.State.ERROR);
        }
    };
    
    /**
     * This task creates the nodes on each slave
     */
    private SlaveExecutor.NodeRunnable mCreateNodesTask = new SlaveExecutor.NodeRunnable() {
        
        @Override
        public void onTimeout() {
            mLogger.warning("session timed out");
            
            // switch state to error
            setSessionState(Session.State.ERROR);
        }
        
        @Override
        public void onPrepare() throws OperationFailedException {
            // check if not aborted
            if (isAborted()) {
                throw new OperationFailedException("node creation aborted due to aborted session");
            }
        }
        
        @Override
        public void onFinish() {
            mLogger.fine("nodes created");
            
            // schedule a preparation
            // timeout: 180 seconds
            mSlaveExecutor.execute(mSlaves, mBootUpTask, 180);
        }
        
        @Override
        public void onError(Exception e) {
            mLogger.severe(e.toString());

            // switch state to error
            setSessionState(Session.State.ERROR);
        }

        @Override
        public void run(SlaveConnection c, Node n) throws OperationFailedException {
            try {
                // create the node on the slave
                c.createNode(n);
                
                // set new node state
                Database.getInstance().updateNode(n, Node.State.CREATED);
            } catch (Exception e) {
                throw new OperationFailedException(e);
            }
        }
    };
    
    /**
     * This task boot the nodes and wait until they are up
     */
    private SlaveExecutor.SlaveRunnable mBootUpTask = new SlaveExecutor.SlaveRunnable() {
        
        @Override
        public void run(SlaveConnection c, Slave s) throws OperationFailedException {
            try {
                // run the session on the slave
                c.runSession(mSession);
            } catch (Exception e) {
                throw new OperationFailedException(e);
            }
            
            // get all nodes of this session
            List<Node> nodes = Database.getInstance().getNodes(mSession);
            
            // mark nodes as up
            for (Node n : nodes) {
                if (n.assignedSlaveId != s.id) continue;
                
                // set new node state
                Database.getInstance().updateNode(n, Node.State.CONNECTED);
            }
        }
        
        @Override
        public void onTimeout() {
            mLogger.warning("boot-up timed out");
            
            // switch state to error
            setSessionState(Session.State.ERROR);
        }
        
        @Override
        public void onPrepare() throws OperationFailedException {
            // check if not aborted
            if (isAborted()) {
                throw new OperationFailedException("boot-up aborted due to aborted session");
            }
        }
        
        @Override
        public void onFinish() {
            mLogger.fine("boot-up complete");
            
            // schedule a run task
            mExecutor.execute(mRunableBootup);
        }

        @Override
        public void onError(Exception e) {
            mLogger.warning("boot-up failed " + e.toString());
            
            // switch state to error
            setSessionState(Session.State.ERROR);
        }
    };
    
    /**
     * This task tries to distribute all nodes across the slaves
     */
    private Runnable mRunnableDistribute = new Runnable() {
        @Override
        public void run() {
            try {
                // clear scheduled distribution
                scheduledDistribution = null;
                
                // try to distribute the session to slaves
                mSlaves = MasterServer.tryDistribution(mSession);
                
                // schedule a preparation
                // timeout: 120 seconds per slave
                mSlaveExecutor.execute(mSlaves, mPrepareTask, 120 * mSlaves.size());
            } catch (MasterServer.DistributionFailedException e) {
                // distribution failed
                mLogger.warning("Distribution of session " + mSession.id.toString() + " failed.");
                
                // check again in 2 minutes
                scheduledDistribution = mExecutor.schedule(mRunnableDistribute, 2, TimeUnit.MINUTES);
            }
        }
    };
    
    /**
     * This task collects all the statistic data from all nodes
     */
    private SlaveExecutor.SlaveRunnable mCollectStatsTask = new SlaveExecutor.SlaveRunnable() {
        @Override
        public void run(SlaveConnection c, Slave s) throws OperationFailedException {
            try {
                mLogger.fine("collect all stats");
                
                // collect stats of this node
                String stats = c.getStats(mSession);
                  
                // store the statistic data in the database
                Database.getInstance().putStats(mSession, stats);
            } catch (Exception e) {
                // stats collection has failed, this is no reason to shutdown the session
                // but it should be logged
                mLogger.severe("stats collection failed: " + e.toString());
            }
        }

        @Override
        public void onPrepare() throws OperationFailedException {
            // check if not aborted
            if (isAborted()) {
                throw new OperationFailedException("stats collection aborted due to aborted session");
            }
        }

        @Override
        public void onFinish() {
            // notify visualizations
            MasterServer.fireSessionStatsUpdated(mSession);
        }

        @Override
        public void onTimeout() {
            mLogger.warning("stats collection timed out");
            
            // switch state to error
            setSessionState(Session.State.ERROR);
        }

        @Override
        public void onError(Exception e) {
            mLogger.warning("stats collection failed " + e.toString());
            
            // switch state to error
            setSessionState(Session.State.ERROR);
        }
        
    };
    
    /**
     * This task boot-up all nodes
     */
    private Runnable mRunableBootup = new Runnable() {
        @Override
        public void run() {
            // abort process if state changed to aborted
            if (isAborted()) return;
            
            // create trace recorders
            if (mSession.stats_record_contact) {
                File f = mContainer.createDataFile("traces", "contact_", ".trace.gz");
                try {
                    mRecorderContact = new TraceRecorder(f);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            
            if (mSession.stats_record_movement) {
                File f = mContainer.createDataFile("traces", "movement_", ".trace.gz");
                try {
                    mRecorderMovement = new TraceRecorder(f);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            
            // prepare movement, nodes initial position, and other parameters
            prepareSetup();
            
            // switch state to running
            setSessionState(Session.State.RUNNING);
            
            // schedule stats collection
            if (mSession.stats_interval != null) {
                scheduledStatsCollector = mExecutor.scheduleWithFixedDelay(new Runnable() {
                    @Override
                    public void run() {
                        // get all nodes of this session
                        List<Node> nodes = Database.getInstance().getNodes(mSession);
                        
                        String nodes_list = "";
                        for (Node n : nodes) {
                            nodes_list += n.toString() + " ";
                        }
                        
                        mLogger.fine("initiate stats collection for " + nodes_list);
                        
                        // schedule stats collection
                        // timeout: 10 seconds per node
                        mSlaveExecutor.execute(mSlaves, mCollectStatsTask, 10 * nodes.size());
                    }
                }, 0, mSession.stats_interval, TimeUnit.SECONDS);
            }
            
            // schedule traffic generation
            scheduledTrafficGeneration = mExecutor.scheduleWithFixedDelay(mTrafficGenerator, 1, 1, TimeUnit.SECONDS);
            
            // schedule movement updates
            Double msec = mSession.resolution * 1000.0;
            scheduledMovement = mExecutor.scheduleWithFixedDelay(mUpdateMovement, msec.longValue(), msec.longValue(), TimeUnit.MILLISECONDS);
            
            mLogger.fine("running");
        }
    };
    
    private SlaveExecutor.SlaveRunnable mDestroyTask = new SlaveExecutor.SlaveRunnable() {
        @Override
        public void run(SlaveConnection c, Slave s) throws OperationFailedException {
            try {
                // stop all nodes
                c.stopSession(mSession);
                
                // destroy the session
                c.destroySession(mSession);
            } catch (Exception e) {
                throw new OperationFailedException(e);
            }
        }
        
        @Override
        public void onTimeout() {
            mLogger.warning("destroy timed out");
        }
        
        @Override
        public void onPrepare() throws OperationFailedException {
        }
        
        @Override
        public void onFinish() {
        }
        
        @Override
        public void onError(Exception e) {
            mLogger.severe("destroy failed " + e.toString());
        }
    };
    
    private Session getSession() {
        return Database.getInstance().getSession(mSession.id);
    }
    
    private void setSessionState(Session.State s) {
        Database.getInstance().setState(mSession, s);
    }
    
    private boolean isAborted() {
        return Session.State.ABORTED.equals( getSession().state ) || Session.State.CANCELLED.equals( getSession().state );
    }
    
    private synchronized void prepareSetup() {
        // create a contact provider
        mContactProvider = new ContactProvider();
        
        // create the right movement model
        switch (mSession.mobility.model) {
            case RANDOM_WAYPOINT:
                // random waypoint selected
                mMovement = new RandomWaypointMovement(mSession.mobility);
                break;
            case RANDOM_WALK:
                // random walk selected
                mMovement = new RandomWalkMovement(mSession.mobility);
                break;
            case STATIC:
                mMovement = new StaticMovement(mSession.mobility);
                break;
            case TRACE:
                mMovement = new TraceMovement(mSession.mobility, mContainer);
                break;
            default:
                mMovement = new NullMovement();
                break;
        }
        
        // assign  provider as movement handler
        mMovement.addMovementHandler(mContactProvider);
        
        // register as movement receiver
        mMovement.addMovementHandler(mMovementHandler);
        
        // assign local object as contact handler
        mContactProvider.addContactHandler(mContactHandler);
        
        Database db = Database.getInstance();
        List<Node> nodes = db.getNodes(mSession);
        
        for (Node n : nodes) {
            // set initial communication range
            n.range = (mSession.range != null) ? mSession.range : 0.0;
            
            // add node to the movement model
            mMovement.add(n);
            
            // add node to the contact provider
            mContactProvider.add(n);
        }
        
        // initialize the movement provider
        mMovement.initialize();
    }
    
    private MovementProvider.MovementHandler mMovementHandler = new MovementProvider.MovementHandler() {
        @Override
        public void onMovement(final Node n, final Coordinates position, Double speed, Double heading) {
            final GeoCoordinates coord = position.getGeoCoordinates();
            
            // update position on the node
            mSlaveExecutor.execute(n, new SlaveExecutor.NodeRunnable() {

                @Override
                public void run(SlaveConnection c, Node n) throws OperationFailedException {
                    try {
                        if (coord == null) {
                            // set invisible
                            c.setPosition(n, 0.0, 0.0, 0.0);
                        } else {
                            c.setPosition(n, coord.getLat(), coord.getLon(), 0.0);
                        }
                    } catch (Exception e) {
                        throw new OperationFailedException(e);
                    }
                }

                @Override
                public void onTimeout() {
                    // timeouts won't happen here
                }
                
                @Override
                public void onPrepare() throws OperationFailedException {
                    // check if not aborted
                    if (isAborted()) {
                        throw new OperationFailedException("position update aborted due to aborted session");
                    }
                }
                
                @Override
                public void onFinish() {
                }

                @Override
                public void onError(Exception e) {
                    mLogger.warning("position update on " + n + " failed, " + e);
                    
                    // switch state to error
                    setSessionState(Session.State.ERROR);
                }
                
            });
            
            // store move in trace recorder
            if (mRecorderMovement != null) {
                try {
                    mRecorderMovement.logMovement(n, position, speed, heading);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    };
    
    private ContactProvider.ContactHandler mContactHandler = new ContactProvider.ContactHandler() {
        @Override
        public void onContact(final Link link) {
            // allow reception on the target
            mSlaveExecutor.execute(link.target, new SlaveExecutor.NodeRunnable() {

                @Override
                public void run(SlaveConnection c, Node n) throws OperationFailedException {
                    try {
                        c.connectionUp(n, link.source);
                    } catch (Exception e) {
                        throw new OperationFailedException(e);
                    }
                }

                @Override
                public void onTimeout() {
                    // timeouts won't happen here
                }
                
                @Override
                public void onPrepare() throws OperationFailedException {
                    // check if not aborted
                    if (isAborted()) {
                        throw new OperationFailedException("link operation aborted due to aborted session");
                    }
                }
                
                @Override
                public void onFinish() {
                    // announce link-up to GUI
                    mLogger.fine("link up " + link);
                }

                @Override
                public void onError(Exception e) {
                    mLogger.warning("link-up " + link.toString() + " failed, " + e);
                    
                    // switch state to error
                    setSessionState(Session.State.ERROR);
                }
                
            });
            
            // store contact in trace recorder
            if (mRecorderContact != null) {
                try {
                    mRecorderContact.logContact(link, "UP");
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }

        @Override
        public void onSeparation(final Link link) {
            // deny reception on the target
            mSlaveExecutor.execute(link.target, new SlaveExecutor.NodeRunnable() {

                @Override
                public void run(SlaveConnection c, Node n) throws OperationFailedException {
                    try {
                        c.connectionDown(n, link.source);
                    } catch (Exception e) {
                        throw new OperationFailedException(e);
                    }
                }

                @Override
                public void onTimeout() {
                    // timeouts won't happen here
                }
                
                @Override
                public void onPrepare() throws OperationFailedException {
                    // check if not aborted
                    if (isAborted()) {
                        throw new OperationFailedException("link operation aborted due to aborted session");
                    }
                }
                
                @Override
                public void onFinish() {
                    // announce link-down to GUI
                    mLogger.fine("link down " + link);
                }

                @Override
                public void onError(Exception e) {
                    mLogger.warning("link-down " + link.toString() + " failed, " + e);
                    
                    // switch state to error
                    setSessionState(Session.State.ERROR);
                }
                
            });
            
            // store contact in trace recorder
            if (mRecorderContact != null) {
                try {
                    mRecorderContact.logContact(link, "DOWN");
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    };
    
    private Runnable mUpdateMovement = new Runnable() {
        @Override
        public void run() {
            // update movement
            try {
                mMovement.update();
            } catch (MovementFinishedException e) {
                // switch session state to finished
                setSessionState(Session.State.FINISHED);
            }
        }
    };
    
    private Runnable mTrafficGenerator = new Runnable() {
        @Override
        public void run() {
            // TODO: trigger traffic generation model
        }
    };
}
