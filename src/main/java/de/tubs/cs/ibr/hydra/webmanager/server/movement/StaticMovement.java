package de.tubs.cs.ibr.hydra.webmanager.server.movement;

import java.util.logging.Logger;

import de.tubs.cs.ibr.hydra.webmanager.shared.Coordinates;
import de.tubs.cs.ibr.hydra.webmanager.shared.MobilityParameterSet;
import de.tubs.cs.ibr.hydra.webmanager.shared.Node;

public class StaticMovement extends MovementProvider {
    
    static final Logger logger = Logger.getLogger(StaticMovement.class.getSimpleName());
    
    private MobilityParameterSet mParams = null;
    private boolean initialized = false;
    
    public StaticMovement(MobilityParameterSet p) {
        mParams = p;
    }

    @Override
    public void update() {
        if (!initialized) {
            // put all nodes in line
            Double lastpos = null;
            
            for (Node n : getNodes()) {
                // add new coordinates
                if (n.position == null)
                    n.position = new Coordinates();
                
                if (lastpos == null) {
                    // set the first one to (0.0, 0.0)
                    n.position.setLocation(0.0, 0.0);
                } else {
                    n.position.setLocation(lastpos + n.range, 0.0);
                }
                
                // store previous position
                lastpos = n.position.getX();
                
                logger.finer("placed " + n + " on " + n.position);
                
                // fire movement event
                fireOnMovementEvent(n, n.position, 0.0, 0.0);
            }
            
            initialized = true;
        }
    }

    @Override
    public Long getDuration() {
        if (mParams.parameters.containsKey("duration")) {
            return Long.valueOf(mParams.parameters.get("duration"));
        }
        return null;
    }

}
