package de.tubs.cs.ibr.hydra.webmanager.server.movement;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.logging.Logger;

import de.tubs.cs.ibr.hydra.webmanager.shared.GeoCoordinates;
import de.tubs.cs.ibr.hydra.webmanager.shared.MobilityParameterSet;
import de.tubs.cs.ibr.hydra.webmanager.shared.Node;

public class StaticMovement extends MovementProvider {
    
    static final Logger logger = Logger.getLogger(StaticMovement.class.getSimpleName());
    
    private MobilityParameterSet mParams = null;
    private Double mDuration = null;
    private GeoCoordinates mGeoRef = null;
    
    public StaticMovement(MobilityParameterSet p) {
        mParams = p;
        
        if (p.parameters.containsKey("duration")) {
            mDuration = Double.valueOf(mParams.parameters.get("duration"));
        }
        
        if (p.parameters.containsKey("lat") && p.parameters.containsKey("lng")) {
            Double lat = Double.valueOf(mParams.parameters.get("lat"));
            Double lon = Double.valueOf(mParams.parameters.get("lng"));
            
            // create geo reference from settings
            mGeoRef = new GeoCoordinates(lat, lon);
        } else {
            // create default geo reference from settings
            mGeoRef = new GeoCoordinates(52.123456, 10.123456);
        }
    }

    @Override
    public void update() throws MovementFinishedException {
        // check if the movement has been completed
        if ((mDuration != null) && (getElapsedTime() > mDuration))
            throw new MovementFinishedException();
    }

    private void arrangeInLine() {
        // put all nodes in line
        Double lastpos = null;
        
        for (Node n : getNodes()) {
            if (mGeoRef != null) {
                // set map reference coordinates
                n.position.setReference(mGeoRef);
            }
            
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
    }

    @Override
    public void initialize() {
        if (mParams.parameters.containsKey("positions")) {
            String positions = mParams.parameters.get("positions");
            
            if ((positions != null) && (!positions.isEmpty())) {
                // set nodes using defined positions
                BufferedReader posReader = new BufferedReader(new StringReader(positions));
                
                try {
                    for (Node n : getNodes()) {
                        String position = posReader.readLine();
                        
                        // abort if there are no more positions
                        if (position == null) break;
                        
                        String[] data = position.split(" ");
                        if (data.length >= 3) {
                            Double x = Double.valueOf(data[0]);
                            Double y = Double.valueOf(data[1]);
                            Double z = Double.valueOf(data[2]);
                            
                            if (z == 0.0) {
                                n.position.setLocation(x, y);
                            } else {
                                n.position.setLocation(x, y, z);
                            }

                            logger.finer("placed " + n + " on " + n.position);
                            
                            // fire movement event
                            fireOnMovementEvent(n, n.position, 0.0, 0.0);
                        }
                    }
                } catch (IOException e) {
                    // error
                }
            } else {
                arrangeInLine();
            }
        } else {
            arrangeInLine();
        }
    }
}
