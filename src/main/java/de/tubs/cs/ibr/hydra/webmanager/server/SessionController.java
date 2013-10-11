package de.tubs.cs.ibr.hydra.webmanager.server;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import de.tubs.cs.ibr.hydra.webmanager.server.SlaveConnection.SessionNotFoundException;
import de.tubs.cs.ibr.hydra.webmanager.server.data.Configuration;
import de.tubs.cs.ibr.hydra.webmanager.server.data.Database;
import de.tubs.cs.ibr.hydra.webmanager.server.movement.MovementProvider;
import de.tubs.cs.ibr.hydra.webmanager.shared.Event;
import de.tubs.cs.ibr.hydra.webmanager.shared.EventType;
import de.tubs.cs.ibr.hydra.webmanager.shared.Node;
import de.tubs.cs.ibr.hydra.webmanager.shared.Session;
import de.tubs.cs.ibr.hydra.webmanager.shared.Slave;

public class SessionController {
    
    // movement model / controller
    MovementProvider mMovement = null;
    
    // session object
    Session mSession = null;
    
    // list of all slaves used for this session
    Set<Slave> mSlaves = null;
    
    // main executor
    ScheduledExecutorService mMainExecutor = Executors.newSingleThreadScheduledExecutor();
    ExecutorService mPooledExecutor = Executors.newCachedThreadPool();
    
    ScheduledFuture<?> scheduledDistribution = null;
    
    public SessionController(Session s) {
        mSession = s;
    }
    
    private MasterServer.EventListener mEventListener = new MasterServer.EventListener() {
        
        @Override
        public void onEvent(Event evt) {
            if (EventType.SLAVE_STATE_CHANGED.equals(evt)) {
                // if we are current try to distribute this session
                if (scheduledDistribution != null) {
                    // cancel scheduled distribution
                    scheduledDistribution.cancel(false);
                    
                    // try to distribute the session now
                    mMainExecutor.execute(mRunnableDistribute);
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
        
        // try to distribute the session now
        mMainExecutor.execute(mRunnableDistribute);
    }
    
    public void abort() {
        if (Session.State.RUNNING.equals(getSession().state)) {
            // TODO: shutdown all nodes first
        }
        
        // shutdown and clean-up waste
        onDestroy();
    }
    
    public void cancel() {
        if (Session.State.RUNNING.equals(getSession().state)) {
            // TODO: shutdown all nodes first
        }
        
        // switch state to aborted
        setSessionState(Session.State.ABORTED);
        
        // shutdown and clean-up waste
        onDestroy();
    }
    
    public void error() {
        if (Session.State.RUNNING.equals(getSession().state)) {
            // TODO: shutdown all nodes first
        }
        
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
        
        // cancel scheduled distribution
        if (scheduledDistribution != null)
            scheduledDistribution.cancel(false);
        
        // shutdown main executor
        mMainExecutor.shutdown();
        mPooledExecutor.shutdown();
        
        // wait until all task are done
        try {
            mMainExecutor.awaitTermination(5, TimeUnit.MINUTES);
            mPooledExecutor.awaitTermination(5, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        // clean-up from MasterServer
        MasterServer.onSessionFinished(mSession);
    }
    
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
                
                // schedule a prepare task
                mMainExecutor.execute(mRunablePrepare);
            } catch (MasterServer.DistributionFailedException e) {
                // distribution failed
                System.err.println("Distribution of session " + mSession.id.toString() + " failed.");
                
                // check again in 2 minutes
                scheduledDistribution = mMainExecutor.schedule(mRunnableDistribute, 2, TimeUnit.MINUTES);
            }
        }
    };
    
    /**
     * This task prepares all nodes
     */
    private Runnable mRunablePrepare = new Runnable() {
        @Override
        public void run() {
            Database db = Database.getInstance();
            
            // get all nodes of this session
            List<Node> nodes = db.getNodes(mSession);
            
            // prepare a latch
            CountDownLatch latch = new CountDownLatch(nodes.size());
            
            // get all nodes on this slave
            for (Slave s : mSlaves) {
                // schedule prepare task for this slave
                mPooledExecutor.execute( new PrepareTask(latch, s, nodes) );
            }
            
            try {
                // wait until all slaves are prepared
                // maximum one minute per node
                if (latch.await(nodes.size(), TimeUnit.MINUTES)) {
                    // schedule a run task
                    mMainExecutor.execute(mRunableBootup);
                }
                else {
                    // timeout
                    error();
                }
            } catch (InterruptedException e) {
                // error
                e.printStackTrace();
            }
        }
    };
    
    private class PrepareTask implements Runnable {
        
        CountDownLatch mLatch = null;
        Slave mSlave = null;
        List<Node> mNodes = null;
        
        
        public PrepareTask(CountDownLatch latch, Slave slave, List<Node> nodes) {
            mLatch = latch;
            mSlave = slave;
            mNodes = nodes;
        }

        @Override
        public void run() {
            SlaveConnection conn = MasterServer.getSlaveConnection(mSlave);
            
            try {
                // create the session on the slave
                conn.createSession(mSession, Configuration.getWebLocation());
                
                // prepare the session
                conn.prepareSession(mSession);
                
                // add all nodes
                for (Node n : mNodes) {
                    if (n.slaveId != mSlave.id) continue;
                    
                    // create the node on the slave
                    conn.createNode(n);
                    
                    // decrement latch for each processed node
                    mLatch.countDown();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (SessionNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * This task boot-up all nodes
     */
    private Runnable mRunableBootup = new Runnable() {
        @Override
        public void run() {
            // switch state to running
            setSessionState(Session.State.RUNNING);
            
            // schedule a finish task
            // TODO: add right time
            mMainExecutor.schedule(mRunableFinish, 60, TimeUnit.SECONDS);
        }
    };
    
    /**
     * This task finishes the session
     */
    private Runnable mRunableFinish = new Runnable() {
        @Override
        public void run() {
            // prepare a latch
            CountDownLatch latch = new CountDownLatch(mSlaves.size());
            
            // get all nodes on this slave
            for (Slave s : mSlaves) {
                // schedule prepare task for this slave
                mPooledExecutor.execute( new FinishTask(latch, s) );
            }
            
            try {
                // wait until all slaves are prepared, maximum 5 minutes
                if (latch.await(5, TimeUnit.MINUTES)) {
                    // finish the session
                    finished();
                }
                else {
                    // timeout
                    error();
                }
            } catch (InterruptedException e) {
                // error
                e.printStackTrace();
                error();
            }
        }
    };
    
    private class FinishTask implements Runnable {
        
        CountDownLatch mLatch = null;
        Slave mSlave = null;

        public FinishTask(CountDownLatch latch, Slave slave) {
            mLatch = latch;
            mSlave = slave;
        }

        @Override
        public void run() {
            SlaveConnection conn = MasterServer.getSlaveConnection(mSlave);
            
            try {
                // stop all nodes
                conn.stopSession(mSession);
                
                // destroy the session
                conn.destroySession(mSession);
                
                // decrement the latch
                mLatch.countDown();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (SessionNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
    
    private Session getSession() {
        return Database.getInstance().getSession(mSession.id);
    }
    
    private void setSessionState(Session.State s) {
        Database.getInstance().setState(mSession, s);
    }
}
