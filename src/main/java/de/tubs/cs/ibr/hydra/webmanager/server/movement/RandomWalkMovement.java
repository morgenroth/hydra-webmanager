package de.tubs.cs.ibr.hydra.webmanager.server.movement;

import de.tubs.cs.ibr.hydra.webmanager.shared.MobilityParameterSet;

public class RandomWalkMovement extends MovementProvider {
    
    private MobilityParameterSet mParams = null;
    
    public RandomWalkMovement(MobilityParameterSet p) {
        mParams = p;
    }

    @Override
    public Long getDuration() {
        if (mParams.parameters.containsKey("duration")) {
            return Long.valueOf(mParams.parameters.get("duration"));
        }
        return null;
    }

    @Override
    public void update() {
        // TODO Auto-generated method stub
        System.out.println("Interval: " + getTimeInterval());
    }

}
