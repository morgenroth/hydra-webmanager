package de.tubs.cs.ibr.hydra.webmanager.server.movement;

import de.tubs.cs.ibr.hydra.webmanager.shared.Node;

public abstract class MovementProvider {
    public interface MovementListener {
        public void onInRange(Node observer, Node target);
        public void onOutOfRange(Node observer, Node target);
        public void onMovement(Node n, Coordinates position, Double speed, Double heading);
    }
    
    private Double mResolution = 1.0;
    
    protected void setResolution(Double resolution) {
        mResolution = resolution;
    }
    
    public Double getResolution() {
        return mResolution;
    }
    
    /**
     * Returns the duration of the simulation in seconds
     * @return Duration in seconds
     */
    public abstract Long getDuration();
}
