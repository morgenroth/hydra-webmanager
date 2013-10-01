package de.tubs.cs.ibr.hydra.webmanager.shared;

import java.io.Serializable;

public class Slave implements Serializable {

    /**
     * Serial ID
     */
    private static final long serialVersionUID = 1090658748223324674L;
    
    public enum State {
        IDLE("idle"),
        ASSIGNED("assigned"),
        PREPARING("preparing"),
        PREPARED("prepared"),
        RUNNING("running"),
        CLEANUP("clean-up");
        
        private String mTag;
        
        private State(String tag) {
            mTag = tag;
        }
        
        public String getTag() {
            return mTag;
        }
        
        @Override
        public String toString() {
            return mTag;
        }
        
        public boolean equals(String value) {
            return mTag.equals(value);
        }
        
        public static State fromString(String tag) {
            if (CLEANUP.equals(tag)) {
                return State.CLEANUP;
            }
            else if (ASSIGNED.equals(tag)) {
                return State.ASSIGNED;
            }
            else if (PREPARING.equals(tag)) {
                return State.PREPARING;
            }
            else if (PREPARED.equals(tag)) {
                return State.PREPARED;
            }
            else if (RUNNING.equals(tag)) {
                return State.RUNNING;
            }
            return State.IDLE;
        }
    };

    public String name = null;
    public String address = null;
    public State state = State.IDLE;
    public String session = null;
    
    public Slave() {
    }
    
    public Slave(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        if (this.name != null) return this.name;
        return super.toString();
    }
}
