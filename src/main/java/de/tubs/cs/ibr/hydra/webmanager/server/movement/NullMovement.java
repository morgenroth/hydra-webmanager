package de.tubs.cs.ibr.hydra.webmanager.server.movement;


public class NullMovement extends MovementProvider {

    @Override
    public void update() throws MovementFinishedException {
        // nobody moves!
    }

    @Override
    public void initialize() {
        // nothing to initialize
    }
    
}
