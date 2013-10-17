package de.tubs.cs.ibr.hydra.webmanager.server.movement;

public class NullMovement extends MovementProvider {

    @Override
    public Long getDuration() {
        return null;
    }

    @Override
    public void step(Double interval) {
        // nobody moves!
    }

}
