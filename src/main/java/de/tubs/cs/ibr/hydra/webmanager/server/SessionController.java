package de.tubs.cs.ibr.hydra.webmanager.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import de.tubs.cs.ibr.hydra.webmanager.server.SlaveExecutor.OperationFailedException;
import de.tubs.cs.ibr.hydra.webmanager.server.data.Configuration;
import de.tubs.cs.ibr.hydra.webmanager.server.data.Database;
import de.tubs.cs.ibr.hydra.webmanager.server.data.SessionContainer;
import de.tubs.cs.ibr.hydra.webmanager.server.movement.ContactProvider;
import de.tubs.cs.ibr.hydra.webmanager.server.movement.MovementProvider;
import de.tubs.cs.ibr.hydra.webmanager.server.movement.NullMovement;
import de.tubs.cs.ibr.hydra.webmanager.server.movement.RandomWalkMovement;
import de.tubs.cs.ibr.hydra.webmanager.server.movement.StaticMovement;
import de.tubs.cs.ibr.hydra.webmanager.shared.Coordinates;
import de.tubs.cs.ibr.hydra.webmanager.shared.Event;
import de.tubs.cs.ibr.hydra.webmanager.shared.EventType;
import de.tubs.cs.ibr.hydra.webmanager.shared.Link;
import de.tubs.cs.ibr.hydra.webmanager.shared.Node;
import de.tubs.cs.ibr.hydra.webmanager.shared.Session;
import de.tubs.cs.ibr.hydra.webmanager.shared.Slave;

public class SessionController {
    
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
    ScheduledFuture<?> scheduledFinish = null;
    ScheduledFuture<?> scheduledStatsCollector = null;
    ScheduledFuture<?> scheduledMovement = null;
    ScheduledFuture<?> scheduledTrafficGeneration = null;
    
    public SessionController(Session s) {
        mSession = s;
    }
    
    public ArrayList<Link> getLinks() {
        if (mContactProvider == null) return null;
        return mContactProvider.getLinks();
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
            System.err.println("ERROR: " + e.toString());
            // error
            error();
        }
    }
    
    public void abort() {
        // shutdown and clean-up waste
        onDestroy();
        
        // wait until all task are done
        try {
            mExecutor.awaitTermination(5, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            System.err.println("Interrupted during abort()");
        }
    }
    
    public void cancel() {
        // switch state to aborted
        setSessionState(Session.State.ABORTED);
        
        // shutdown and clean-up waste
        onDestroy();
        
        // wait until all task are done
        try {
            mExecutor.awaitTermination(5, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            System.err.println("Interrupted during cancel()");
        }
    }
    
    public void error() {
        if (isAborted()) return;
        
        // switch state to aborted
        setSessionState(Session.State.ERROR);
        
        // shutdown and clean-up waste
        onDestroy();
    }
    
    public void finished() {
        // switch state to aborted
        setSessionState(Session.State.FINISHED);
        
        // shutdown and clean-up waste
        onDestroy();
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
        
        // cancel scheduled finish
        if (scheduledFinish != null) {
            scheduledFinish.cancel(false);
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
            System.err.println("Interrupted during onDestroy(): " + e.toString());
        } catch (ExecutionException e) {
            System.err.println("Execution failed during onDestroy(): " + e.toString());
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
            System.err.println("ERROR: preparation timed out");
            error();
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
            System.err.println("ERROR: preparation failed " + e.toString());
            error();
        }
    };
    
    /**
     * This task creates the nodes on each slave
     */
    private SlaveExecutor.NodeRunnable mCreateNodesTask = new SlaveExecutor.NodeRunnable() {
        
        @Override
        public void onTimeout() {
            System.err.println("ERROR: session timed out");
            error();
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
            // schedule a preparation
            // timeout: 180 seconds
            mSlaveExecutor.execute(mSlaves, mBootUpTask, 180);
        }
        
        @Override
        public void onError(Exception e) {
            System.err.println("ERROR: " + e.toString());
            error();
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
            System.err.println("ERROR: boot-up timed out");
            error();
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
            // schedule a run task
            mExecutor.execute(mRunableBootup);
        }

        @Override
        public void onError(Exception e) {
            System.err.println("ERROR: boot-up failed " + e.toString());
            error();
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
                System.err.println("Distribution of session " + mSession.id.toString() + " failed.");
                
                // check again in 2 minutes
                scheduledDistribution = mExecutor.schedule(mRunnableDistribute, 2, TimeUnit.MINUTES);
            }
        }
    };
    
    /**
     * This task collects all the statistic data from all nodes
     */
    private SlaveExecutor.NodeRunnable mCollectStatsTask = new SlaveExecutor.NodeRunnable() {

        @Override
        public void run(SlaveConnection c, Node n) throws OperationFailedException {
            try {
                // collect stats of this node
                String stats = c.getStats(n);
                  
                // store the statistic data in the database
                Database.getInstance().putStats(n, stats);
            } catch (Exception e) {
                throw new OperationFailedException(e);
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
            System.err.println("ERROR: stats collection timed out");
            error();
        }

        @Override
        public void onError(Exception e) {
            System.err.println("ERROR: stats collection failed " + e.toString());
            error();
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
            
            // prepare movement, nodes initial position, and other parameters
            prepareSetup();
            
            // switch state to running
            setSessionState(Session.State.RUNNING);
            
            // schedule stats collection
            if (mSession.stats_interval != null) {
                scheduledStatsCollector = mExecutor.scheduleAtFixedRate(new Runnable() {
                    @Override
                    public void run() {
                        // get all nodes of this session
                        List<Node> nodes = Database.getInstance().getNodes(mSession);
                        
                        // schedule stats collection
                        // timeout: 10 seconds per node
                        mSlaveExecutor.execute(mSlaves, nodes, mCollectStatsTask, 10 * nodes.size());
                    }
                }, 0, mSession.stats_interval, TimeUnit.SECONDS);
            }
            
            // schedule traffic generation
            scheduledTrafficGeneration = mExecutor.scheduleWithFixedDelay(mTrafficGenerator, 1, 1, TimeUnit.SECONDS);
            
            // schedule movement updates
            Double msec = mSession.resolution * 1000.0;
            scheduledMovement = mExecutor.scheduleWithFixedDelay(mUpdateMovement, msec.longValue(), msec.longValue(), TimeUnit.MILLISECONDS);
            
            // schedule a finish task - if duration is specified
            Long duration = mMovement.getDuration();
            if (duration != null) {
                scheduledFinish = mExecutor.schedule(mRunableFinish, duration, TimeUnit.SECONDS);
            }
        }
    };
    
    /**
     * This task finishes the session
     */
    private Runnable mRunableFinish = new Runnable() {
        @Override
        public void run() {
            // unset global variable
            scheduledFinish = null;
            
            // switch session state to finished
            finished();
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
            System.err.println("ERROR: destroy timed out");
        }
        
        @Override
        public void onPrepare() throws OperationFailedException {
        }
        
        @Override
        public void onFinish() {
        }
        
        @Override
        public void onError(Exception e) {
            System.err.println("ERROR: destroy failed " + e.toString());
        }
    };
    
    private Session getSession() {
        return Database.getInstance().getSession(mSession.id);
    }
    
    private void setSessionState(Session.State s) {
        Database.getInstance().setState(mSession, s);
    }
    
    private boolean isAborted() {
        return Session.State.ABORTED.equals( getSession().state );
    }
    
    private void prepareSetup() {
        // create a contact provider
        mContactProvider = new ContactProvider();
        
        // create the right movement model
        switch (mSession.mobility.model) {
            case RANDOM_WALK:
                // random walk selected
                mMovement = new RandomWalkMovement(mSession.mobility);
                break;
            case STATIC:
                mMovement = new StaticMovement(mSession.mobility);
                break;
//            case THE_ONE:
//                break;
            default:
                mMovement = new NullMovement();
                break;
        }
        
        // assign contact provider as movement handler
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
    }
    
    private MovementProvider.MovementHandler mMovementHandler = new MovementProvider.MovementHandler() {
        @Override
        public void onMovement(final Node n, final Coordinates position, Double speed, Double heading) {
            // update position on the node
            mSlaveExecutor.execute(n, new SlaveExecutor.NodeRunnable() {

                @Override
                public void run(SlaveConnection c, Node n) throws OperationFailedException {
                    try {
                        c.setPosition(n, position.getX(), position.getY(), position.getZ());
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
                    // announce new position to GUI
                    System.out.println("POSITION SET on " + n + ": " + position);
                    MasterServer.firePositionUpdated(mSession, n, position);
                }

                @Override
                public void onError(Exception e) {
                    System.err.println("ERROR: position update on " + n + ", " + e);
                    error();
                }
                
            });
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
                    System.out.println("CONTACT: " + link);
                    MasterServer.fireLinkUp(mSession, link);
                }

                @Override
                public void onError(Exception e) {
                    System.err.println("ERROR: link-up " + link.toString() + " failed, " + e);
                    error();
                }
                
            });
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
                    System.out.println("SEPARATION: " + link);
                    MasterServer.fireLinkDown(mSession, link);
                }

                @Override
                public void onError(Exception e) {
                    System.err.println("ERROR: link-down " + link.toString() + " failed, " + e);
                    error();
                }
                
            });
        }
    };
    
    private Runnable mUpdateMovement = new Runnable() {
        @Override
        public void run() {
            // update movement
            mMovement.update();
        }
    };
    
    private Runnable mTrafficGenerator = new Runnable() {
        @Override
        public void run() {
            // TODO: trigger traffic generation model
        }
    };
}
