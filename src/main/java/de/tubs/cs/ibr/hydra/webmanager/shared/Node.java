package de.tubs.cs.ibr.hydra.webmanager.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

public class Node implements IsSerializable {

    public enum State {
        DRAFT("draft"),
        SCHEDULED("scheduled"),
        CREATED("created"),
        CONNECTED("connected"),
        DESTROYED("destroyed"),
        ERROR("error");
        
        private String mTag;
        
        private State(String tag) {
            mTag = tag;
        }
        
        public String getTag() {
            return mTag;
        }
        
        public boolean equals(String value) {
            return mTag.equals(value);
        }
        
        @Override
        public String toString() {
            return mTag;
        }
        
        public static State fromString(String tag) {
            if (State.SCHEDULED.equals(tag)) {
                return State.SCHEDULED;
            }
            else if (State.CREATED.equals(tag)) {
                return State.CREATED;
            }
            else if (State.CONNECTED.equals(tag)) {
                return State.CONNECTED;
            }
            else if (State.DESTROYED.equals(tag)) {
                return State.DESTROYED;
            }
            else if (State.DRAFT.equals(tag)) {
                return State.DRAFT;
            }
            return State.ERROR;
        }
    };
    
    public Long id = null;
    public Long slaveId = null;
    public Long assignedSlaveId = null;
    public Long sessionId = null;
    public String name = null;
    public State state = null;
    public String address = null;
    
    /**
     * Contains the coordinates of this node.
     */
    public Coordinates position = null;
    
    /**
     * Heading and speed for movement
     */
    public double speed = 0.0;
    public double heading = 0.0;
    
    /**
     * This holds the unidirectional communication range
     * which depends on the transmitter power.
     */
    public double range = 0.0;

    @Override
    public String toString() {
        if (name != null) return name;
        if (id != null) return id.toString();
        return super.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Node) {
            Node n = (Node)obj;
            return (id == n.id);
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        if (id != null) return id.hashCode();
        return super.hashCode();
    }
}
