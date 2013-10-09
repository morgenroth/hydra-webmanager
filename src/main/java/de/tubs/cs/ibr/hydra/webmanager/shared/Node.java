package de.tubs.cs.ibr.hydra.webmanager.shared;

import java.io.Serializable;

public class Node implements Serializable {

    /**
     * Serial ID
     */
    private static final long serialVersionUID = 2954357776414155391L;

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
    public Long sessionId = null;
    public String name = null;
    public State state = null;
    public String address = null;
}
