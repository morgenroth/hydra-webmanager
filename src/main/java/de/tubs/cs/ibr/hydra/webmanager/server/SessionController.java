package de.tubs.cs.ibr.hydra.webmanager.server;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

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
    
    // list of all nodes of this session
    ArrayList<Node> mNodes = null;
    
    // list of all slaves used for this session
    Set<Slave> mSlaves = null;
    
    // main executor
    ScheduledExecutorService mExecutor = Executors.newScheduledThreadPool(5);
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
        
        // try to distribute the session now
        mExecutor.execute(mRunnableDistribute);
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
    
    private void onDestroy() {
        // un-register event listener
        MasterServer.unregisterEventListener(mEventListener);
        
        // cancel scheduled distribution
        if (scheduledDistribution != null)
            scheduledDistribution.cancel(false);
        
        // shutdown main executor
        mExecutor.shutdown();
        
        // wait until all task are done
        try {
            mExecutor.awaitTermination(5, TimeUnit.MINUTES);
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
                mExecutor.execute(mRunablePrepare);
            } catch (MasterServer.DistributionFailedException e) {
                // distribution failed
                System.err.println("Distribution of session " + mSession.id.toString() + " failed.");
                
                // check again in 2 minutes
                scheduledDistribution = mExecutor.schedule(mRunnableDistribute, 2, TimeUnit.MINUTES);
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
            
            // get all nodes on this slave
            for (Slave s: mSlaves) {
                ArrayList<Node> nodes = db.getNodes(mSession, s);
            }
            
            // TODO: schedule a run task
            mExecutor.execute(mRunableBootup);
        }
    };
    
    /**
     * This task boot-up all nodes
     */
    private Runnable mRunableBootup = new Runnable() {
        @Override
        public void run() {
            Database db = Database.getInstance();

            // switch state to running
            setSessionState(Session.State.RUNNING);
        }
    };
    
    private Session getSession() {
        return Database.getInstance().getSession(mSession.id);
    }
    
    private void setSessionState(Session.State s) {
        Database.getInstance().setState(mSession, s);
    }
}
