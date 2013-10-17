package de.tubs.cs.ibr.hydra.webmanager.server.movement;

import java.util.HashSet;

import de.tubs.cs.ibr.hydra.webmanager.shared.Coordinates;
import de.tubs.cs.ibr.hydra.webmanager.shared.Node;

public abstract class MovementProvider {
    public interface MovementHandler {
        public void onMovement(Node n, Coordinates position, Double speed, Double heading);
    }
    
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
     * Increment the past time by the given value
     * and updates the positions of all nodes.
     */
    public abstract void step(Double interval);
    
    /**
     * Returns the duration of the simulation in seconds
     * @return Duration in seconds
     */
    public abstract Long getDuration();
}
