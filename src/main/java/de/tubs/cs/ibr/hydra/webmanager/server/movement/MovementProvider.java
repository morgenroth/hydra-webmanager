package de.tubs.cs.ibr.hydra.webmanager.server.movement;

import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import de.tubs.cs.ibr.hydra.webmanager.shared.Coordinates;
import de.tubs.cs.ibr.hydra.webmanager.shared.Node;

public abstract class MovementProvider {
    public interface MovementHandler {
        public void onMovement(Node n, Coordinates position, Double speed, Double heading);
    }
    
    private Long mLastUpdate = null;
    private HashSet<Node> mNodes = new HashSet<Node>();
    private HashSet<MovementHandler> mListener = new HashSet<MovementHandler>();
    
    protected HashSet<Node> getNodes() {
        return mNodes;
    }
    
    protected void fireOnMovementEvent(Node n, Coordinates position, Double speed, Double heading) {
        synchronized(mListener) {
            for (MovementHandler m : mListener) {
                m.onMovement(n, position, speed, heading);
            }
        }
    }
    
    /**
     * Calculated the time interval since the last call
     * @return
     */
    protected Double getTimeInterval() {
        Double ret = 0.0;
        
        Long now = System.nanoTime();
        
        if (mLastUpdate != null) {
            ret = Double.valueOf(now - mLastUpdate) / Double.valueOf(TimeUnit.SECONDS.toNanos(1));
        }

        mLastUpdate = now;
        
        return ret;
    }
    
    public void addMovementHandler(MovementHandler m) {
        synchronized(mListener) {
            mListener.add(m);
        }
    }
    
    public void removeMovementHandler(MovementHandler m) {
        synchronized(mListener) {
            mListener.remove(m);
        }
    }
    
    /**
     * Add a node to this movement
     * @param n
     */
    public void add(Node n) {
        synchronized(mNodes) {
            mNodes.add(n);
            
            // TODO: set communication range
            n.range = 0.0;
        }
    }
    
    /**
     * Removes a node from this movement
     * @param n
     */
    public void remove(Node n) {
        synchronized(mNodes) {
            mNodes.remove(n);
        }
    }
    
    /**
     * Updates the positions of all nodes according to the
     * time past since the last call
     */
    public abstract void update();
    
    /**
     * Returns the duration of the simulation in seconds
     * @return Duration in seconds
     */
    public abstract Long getDuration();
}
