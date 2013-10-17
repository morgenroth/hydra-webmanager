package de.tubs.cs.ibr.hydra.webmanager.server;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import de.tubs.cs.ibr.hydra.webmanager.shared.Node;
import de.tubs.cs.ibr.hydra.webmanager.shared.Slave;

public class SlaveExecutor {
    
    private ExecutorService mPooledExecutor = Executors.newCachedThreadPool();
    private ExecutorService mExecutor = null;
    
    private boolean mAborted = false;
    
    public SlaveExecutor(ExecutorService e) {
        mExecutor = e;
    }
    
    public static class OperationFailedException extends Exception {
        /**
         * serial ID
         */
        private static final long serialVersionUID = -6497370327834890900L;
        
        public OperationFailedException(String what) {
            super(what);
        }
        
        public OperationFailedException() {
            super();
        }
        
        public OperationFailedException(Exception e) {
            super(e);
        }
    };
    
    public interface SlaveRunnable {
        public void run(SlaveConnection c, Slave s) throws OperationFailedException;
        public void onPrepare() throws OperationFailedException;
        public void onFinish();
        public void onTimeout();
        public void onError(Exception e);
    }
    
    public interface NodeRunnable {
        public void run(SlaveConnection c, Node n) throws OperationFailedException;
        public void onPrepare() throws OperationFailedException;
        public void onFinish();
        public void onTimeout();
        public void onError(Exception e);
    }

    public void execute(Set<Slave> slaves, SlaveRunnable r, int timeout) {
        mExecutor.execute(new Distributor(slaves, r, timeout));
    }
    
    public void execute(Set<Slave> slaves, List<Node> nodes, NodeRunnable r, int timeout) {
        mExecutor.execute(new Distributor(slaves, nodes, r, timeout));
    }
    
    public void execute(Node n, NodeRunnable r) {
        mExecutor.execute(new SingleDistributor(n, r));
    }
    
    public Future<Integer> submit(Set<Slave> slaves, SlaveRunnable r, int timeout) {
        return mExecutor.submit(new Distributor(slaves, r, timeout), 0);
    }
    
    public Future<Integer> submit(Set<Slave> slaves, List<Node> nodes, NodeRunnable r, int timeout) {
        return mExecutor.submit(new Distributor(slaves, nodes, r, timeout), 0);
    }
    
    public Future<Integer> submit(Node n, NodeRunnable r) {
        return mExecutor.submit(new SingleDistributor(n, r), 0);
    }

    
    public void awaitShutdown() throws InterruptedException {
        // set aborted to true
        mAborted = true;
        
        // shutdown pooled executor
        mPooledExecutor.shutdown();
        
        // wait until all task are done
        mPooledExecutor.awaitTermination(5, TimeUnit.MINUTES);
    }
    
    private class SingleDistributor implements Runnable {
        private Node mNode = null;
        private NodeRunnable mRunnable = null;
        
        public SingleDistributor(Node n, NodeRunnable r) {
            mNode = n;
            mRunnable = r;
        }

        @Override
        public void run() {
            // abort process if state changed to aborted
            if (mAborted) return;
            
            try {
                mRunnable.onPrepare();
            } catch (OperationFailedException e) {
                mRunnable.onError(e);
                return;
            }
            
            // fake slave object
            Slave s = new Slave();
            s.id = mNode.slaveId;
            SlaveConnection conn = MasterServer.getSlaveConnection(s);
            
            try {
                // execute node task
                mRunnable.run(conn, mNode);
                mRunnable.onFinish();
            } catch (OperationFailedException e) {
                // mark globally as error
                mRunnable.onError(e);
            }
        }
    }
    
    private class Distributor implements Runnable {
        
        private int mTimeout = 60;
        private CountDownLatch mLatch = null;
        private List<Node> mNodes = null;
        private Set<Slave> mSlaves = null;
        private NodeRunnable mNodeRunnable = null;
        private SlaveRunnable mSlaveRunnable = null;
        
        private Exception mError = null;
        
        public Distributor(Set<Slave> slaves, SlaveRunnable r, int timeout) {
            mSlaves = slaves;
            mSlaveRunnable = r;
            mTimeout = timeout;
        }
        
        public Distributor(Set<Slave> slaves, List<Node> nodes, NodeRunnable r, int timeout) {
            mSlaves = slaves;
            mNodes = nodes;
            mNodeRunnable = r;
            mTimeout = timeout;
        }
        
        @Override
        public final void run() {
            // abort process if state changed to aborted
            if (mAborted) return;
            
            try {
                if (mSlaveRunnable != null) mSlaveRunnable.onPrepare();
                if (mNodeRunnable != null) mNodeRunnable.onPrepare();
            } catch (OperationFailedException e) {
                if (mSlaveRunnable != null) mSlaveRunnable.onError(e);
                if (mNodeRunnable != null) mNodeRunnable.onError(e);
                return;
            }

            // prepare a latch
            mLatch = new CountDownLatch(mSlaves.size());
            
            // get all nodes on this slave
            for (Slave s : mSlaves) {
                // schedule prepare task for this slave
                mPooledExecutor.execute( new Distributor.Task(s) );
            }
            
            try {
                // wait until all tasks are done
                if (!mLatch.await(mTimeout, TimeUnit.SECONDS)) {
                    // timeout
                    if (mSlaveRunnable != null) mSlaveRunnable.onTimeout();
                    if (mNodeRunnable != null) mNodeRunnable.onTimeout();
                } else {
                    if (mError != null) {
                        if (mSlaveRunnable != null) mSlaveRunnable.onError(mError);
                        if (mNodeRunnable != null) mNodeRunnable.onError(mError);
                    } else {                    
                        if (mSlaveRunnable != null) mSlaveRunnable.onFinish();
                        if (mNodeRunnable != null) mNodeRunnable.onFinish();
                    }
                }
            } catch (InterruptedException e) {
                if (mSlaveRunnable != null) mSlaveRunnable.onError(e);
                if (mNodeRunnable != null) mNodeRunnable.onError(e);
            }
        }
        
        private class Task implements Runnable {
            private Slave mSlave = null;
            
            public Task(Slave s) {
                mSlave = s;
            }
            
            @Override
            public void run() {
                if (!mAborted) {
                    SlaveConnection conn = MasterServer.getSlaveConnection(mSlave);
                    
                    try {
                        if (mNodes == null) {
                            // execute slave task
                            mSlaveRunnable.run(conn, mSlave);
                        } else {
                            // iterate over all nodes
                            for (Node n : mNodes) {
                                if (n.assignedSlaveId != mSlave.id) continue;
                                
                                // execute node task
                                mNodeRunnable.run(conn, n);
                            }
                        }
                    } catch (OperationFailedException e) {
                        // mark globally as error
                        if (mError == null) mError = e;
                    }
                }
                
                // decrement latch for each processed slave
                mLatch.countDown();
            }
        };
    };
}
