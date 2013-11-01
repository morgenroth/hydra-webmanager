package de.tubs.cs.ibr.hydra.webmanager.server.movement;

import de.tubs.cs.ibr.hydra.webmanager.shared.MobilityParameterSet;

public class TraceMovement extends MovementProvider {

    private MobilityParameterSet mParams = null;
    
    public TraceMovement(MobilityParameterSet p) {
        mParams = p;
    }

    @Override
    public void update() {
        // get the time passed since the last call
        Double interval = getTimeInterval();
        
    }

    @Override
    public Long getDuration() {
        return null;
    }
}
