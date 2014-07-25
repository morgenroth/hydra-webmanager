package de.tubs.cs.ibr.hydra.webmanager.server.movement;

import java.util.HashMap;
import java.util.Random;

import de.tubs.cs.ibr.hydra.webmanager.shared.Coordinates;
import de.tubs.cs.ibr.hydra.webmanager.shared.GeoCoordinates;
import de.tubs.cs.ibr.hydra.webmanager.shared.MobilityParameterSet;
import de.tubs.cs.ibr.hydra.webmanager.shared.Node;

public class RandomWaypointMovement extends MovementProvider {

    private MobilityParameterSet mParams = null;
    private Random rand = null;
    
    private class State {
    	boolean pause = true;
    	Double remainingTime = 0.0;
    }
    
    private HashMap<Long, State> mStates = new HashMap<Long, State>();
    
    private Double mWaitTime = null;
    private Double mAreaWidth = null;
    private Double mAreaHeight = null;
    private Double mVelocityMax = null;
    private Double mVelocityMin = null;
    private Double mDuration = null;
    private GeoCoordinates mGeoRef = null;
    
    public RandomWaypointMovement(MobilityParameterSet p) {
        mParams = p;
        
        // parse parameters
        if (p.parameters.containsKey("waittime")) {
        	mWaitTime = Double.valueOf(p.parameters.get("waittime"));
        } else {
        	mWaitTime = 75.0;
        }
        
        if (p.parameters.containsKey("width")) {
            mAreaWidth = Double.valueOf(p.parameters.get("width"));
        } else {
            mAreaWidth = 5000.0;
        }
        
        if (p.parameters.containsKey("height")) {
            mAreaHeight = Double.valueOf(p.parameters.get("height"));
        } else {
            mAreaHeight = 5000.0;
        }
        
        if (p.parameters.containsKey("vmax")) {
            mVelocityMax = Double.valueOf(p.parameters.get("vmax"));
        } else {
            mVelocityMax = 10.0;
        }
        
        if (p.parameters.containsKey("vmin")) {
            mVelocityMin = Double.valueOf(p.parameters.get("vmin"));
        } else {
            mVelocityMin = 10.0;
        }
        
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
        
        // initialize random number generator
        rand = new Random();
    }
    
	@Override
	public void update() throws MovementFinishedException {
        // check if the movement has been completed
        if ((mDuration != null) && (getElapsedTime() > mDuration))
            throw new MovementFinishedException();
        
        // get the time passed since the last call
        Double interval = getTimeInterval();
        
        // iterate through all nodes
        for (Node n : this.getNodes()) {
            // if no position is set
            if (n.position.isInvalid()) {
                // set initial random coordinates
                double x = randomUniform(0.0, mAreaWidth);
                double y = randomUniform(0.0, mAreaHeight);
                n.position.setLocation(x, y);
            }
            
            // move the node
            move(n, interval);
        }
	}
	
    /**
     * Move node for a defined interval
     * @param n
     * @param interval
     */
    private void move(Node n, Double interval) {
    	State state = mStates.get(n.id);
    	
    	if (state.pause) {
    		// we are in pause mode - do not move
    		if (state.remainingTime >= interval) {
    			// do not move in this interval
    			state.remainingTime -= interval;
    			
    	    	// store state
    	    	mStates.put(n.id, state);
    		} else {
    			// do not move until the remaining time is over
    			interval -= state.remainingTime;
    			
    			// stop pause mode
    			state.pause = false;
    			
                // get new random speed
                n.speed = randomUniform(mVelocityMin, mVelocityMax);
                
                // get new random location
                double x = randomUniform(0.0, mAreaWidth);
                double y = randomUniform(0.0, mAreaHeight);
                Coordinates location = new Coordinates(x, y);
                
                // set new heading and move-time based on a new random way-point
                n.heading = Math.atan2(y - n.position.getY(), x - n.position.getX());
                
                double distance = n.position.distance(location);
                
                // assign estimated time
                state.remainingTime = distance / n.speed;

                // store state
            	mStates.put(n.id, state);
    			
    			// recursively call move to process the remaining interval
    			move(n, interval);
    			return;
    		}
    	} else {
    		if (state.remainingTime >= interval) {
    			// move node for 'interval' time
    			travel(n, state, interval);
    			state.remainingTime -= interval;
    			
    	    	// store state
    	    	mStates.put(n.id, state);
    		} else {
    			// move node for 'state.remainingTime' time
    			travel(n, state, state.remainingTime);
    			interval -= state.remainingTime;
    			
    			// switch to pause mode
    			state.remainingTime = mWaitTime;
    			state.pause = true;
    			
    	    	// store state
    	    	mStates.put(n.id, state);
    			
    			// recursively call move to process the remaining interval
    			move(n, interval);
    			return;
    		}
    	}
    	
        // fire moved event
        fireOnMovementEvent(n, n.position, n.speed, n.heading);
    }
    
    private void travel(Node n, State s, Double interval) {
        // calc new moving distance
        double dist = n.speed * interval;
        double dx = Math.cos(n.heading) * dist;
        double dy = Math.sin(n.heading) * dist;
        
        // move by some distance
        n.position.move(dx, dy);
        
        // safety-check, never cross boundaries!
        if (n.position.getX() < 0) n.position.setLocation(0.0, n.position.getY());
        if (n.position.getY() < 0) n.position.setLocation(n.position.getX(), 0.0);
    }
	
    private double randomUniform(Double min, Double max) {
        return min + (rand.nextDouble() * (max - min));
    }

	@Override
	public void initialize() {
		mStates.clear();
        for (Node n : getNodes()) {
            if (mGeoRef != null) {
                // set map reference coordinates
                n.position.setReference(mGeoRef);
            }
            
            // add state object
            mStates.put(n.id, new State());
        }
	}

}
