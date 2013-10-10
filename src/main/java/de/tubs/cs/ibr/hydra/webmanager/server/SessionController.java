package de.tubs.cs.ibr.hydra.webmanager.server;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import de.tubs.cs.ibr.hydra.webmanager.server.data.Database;
import de.tubs.cs.ibr.hydra.webmanager.server.movement.MovementProvider;
import de.tubs.cs.ibr.hydra.webmanager.shared.Node;
import de.tubs.cs.ibr.hydra.webmanager.shared.Session;

public class SessionController {
    
    // movement model / controller
    MovementProvider mMovement = null;
    
    // session object
    Session mSession = null;
    
    // list of all nodes of this session
    ArrayList<Node> mNodes = null;
    
    // main executor
    ScheduledExecutorService mExecutor = Executors.newScheduledThreadPool(5);
    ScheduledFuture<?> scheduledDistribution = null;
    
    private class DistributionFailedException extends Exception {

        /**
         * serial ID
         */
        private static final long serialVersionUID = 2242070903678118836L;
        
    };
    
    public SessionController(Session s) {
        mSession = s;
    }
    
    public void initiate() {
        if (!Session.State.PENDING.equals(getSession().state)) {
            // only start when session state is pending
            return;
        }
        
        // try to distribute the session now
        mExecutor.execute(mRunnableDistribute);
    }
    
    public void terminate() {
        if (Session.State.RUNNING.equals(getSession().state)) {
            // TODO: shutdown all nodes first
        }
        
        // cancel scheduled distribution
        scheduledDistribution.cancel(false);
        
        // shutdown main executor
        mExecutor.shutdown();
        
        // wait until all task are done
        try {
            mExecutor.awaitTermination(5, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        // switch state to aborted
        setSessionState(Session.State.ABORTED);
    }
    
    private Runnable mRunnableDistribute = new Runnable() {
        @Override
        public void run() {
            try {
                // clear scheduled distribution
                scheduledDistribution = null;
                
                // try to distribute the session to slaves
                tryDistribution();
                
                // switch state to running
                setSessionState(Session.State.RUNNING);
                
            } catch (DistributionFailedException e) {
                // distribution failed
                System.err.println("Distribution of session " + mSession.id.toString() + " failed.");
                
                // check again in 10 seconds
                scheduledDistribution = mExecutor.schedule(mRunnableDistribute, 10, TimeUnit.SECONDS);
            }
        }
    };
    
    private void tryDistribution() throws DistributionFailedException {
        // get all nodes of this session
        mNodes = Database.getInstance().getNodes(mSession.id);
        
        // TODO: allocate slaves to deploy all the nodes
        
        // reset the list of nodes
        mNodes = null;
        
        // allocation failed
        throw new DistributionFailedException();
    }
    
    private Session getSession() {
        return Database.getInstance().getSession(mSession.id);
    }
    
    private void setSessionState(Session.State s) {
        Database.getInstance().setState(mSession, s);
    }
}
