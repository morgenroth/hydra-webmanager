package de.tubs.cs.ibr.hydra.webmanager.server.movement;

import java.util.HashMap;
import java.util.Random;

import de.tubs.cs.ibr.hydra.webmanager.shared.GeoCoordinates;
import de.tubs.cs.ibr.hydra.webmanager.shared.MobilityParameterSet;
import de.tubs.cs.ibr.hydra.webmanager.shared.Node;

public class RandomWalkMovement extends MovementProvider {
    
    private MobilityParameterSet mParams = null;
    private Random rand = null;
    private HashMap<Long, Double> mWalkedTime = new HashMap<Long, Double>();
    
    private Double mMoveTime = null;
    private Double mAreaWidth = null;
    private Double mAreaHeight = null;
    private Double mVelocityMax = null;
    private Double mVelocityMin = null;
    private Double mDuration = null;
    private GeoCoordinates mGeoRef = null;
    
    public RandomWalkMovement(MobilityParameterSet p) {
        mParams = p;
        
        // parse parameters
        if (p.parameters.containsKey("movetime")) {
            mMoveTime = Double.valueOf(p.parameters.get("movetime"));
        } else {
            mMoveTime = 75.0;
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
            if (n.position == null) {
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
        Double walked = mWalkedTime.get(n.id);
        
        // set walked time to max. time (direction change forced)
        if (walked == null) walked = mMoveTime;
        
        // remaining time until a direction change should happen
        Double remainTime = mMoveTime - walked;
        
        // add interval to walked time
        walked += interval;
        
        if (walked >= mMoveTime) {
            // move for the remaining time
            walk(n, remainTime);
            
            // reset the walked time
            walked -= mMoveTime;
            
            // get new random direction
            n.heading = randomUniform(0.0, 2.0 * Math.PI);
            
            // get new random speed
            n.speed = randomUniform(mVelocityMin, mVelocityMax);
            
            // walk for the rest of this interval
            walk(n, walked);
        } else {
            // walk as long as this interval last
            walk(n, interval);
        }
        
        // store the walked time
        mWalkedTime.put(n.id, walked);

        // fire moved event
        fireOnMovementEvent(n, n.position, n.speed, n.heading);
    }
    
    private void walk(Node n, Double interval) {
        // calc new moving distance
        double dist = n.speed * interval;
        double dx = Math.cos(n.heading) * dist;
        double dy = Math.sin(n.heading) * dist;
        
        // move by some distance
        n.position.move(dx, dy);
        
        // bounce on the edges
        bounce(n);
    }
    
    private void bounce(Node n) {
        boolean again = false;
        
        // handle bounds
        if (n.position.getX() < 0) {
            // hit boundary on the left
            n.heading = (-Math.PI - n.heading) % (2.0 * Math.PI);
            
            // flip x
            n.position.flipX();

            again = true;
        }
        else if (n.position.getX() >= mAreaWidth) {
            // hit boundary on the right
            n.heading = (Math.PI - n.heading) % (2.0 * Math.PI);
            
            double overflow = n.position.getX() - mAreaWidth;
            n.position.move(-2.0 * overflow, 0.0);

            again = true;
        }
        else if (n.position.getY() < 0) {
            // hit boundary at the bottom
            n.heading = (-2.0 * Math.PI - n.heading) % (2.0 * Math.PI);
            
            // flip y
            n.position.flipY();
            
            again = true;
        }
        if (n.position.getY() >= mAreaHeight) {
            // hit boundary at the top
            n.heading = (2.0 * Math.PI - n.heading) % (2.0 * Math.PI);
            
            double overflow = n.position.getY() - mAreaHeight;
            n.position.move(0.0, -2.0 * overflow);
            
            again = true;
        }
        
        // bounce again if necessary
        if (again) bounce(n);
    }
    
    private double randomUniform(Double min, Double max) {
        return min + (rand.nextDouble() * (max - min));
    }

    @Override
    public void initialize() {
        for (Node n : getNodes()) {
            if (mGeoRef != null) {
                // set map reference coordinates
                n.position.setReference(mGeoRef);
            }
        }
    }
}
