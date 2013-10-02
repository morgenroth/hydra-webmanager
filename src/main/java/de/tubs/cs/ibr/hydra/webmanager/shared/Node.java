package de.tubs.cs.ibr.hydra.webmanager.shared;

import java.io.Serializable;

public class Node implements Serializable {

    /**
     * Serial ID
     */
    private static final long serialVersionUID = 2954357776414155391L;

    public enum State {
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
        
        @Override
        public String toString() {
            return mTag;
        }
        
        public static State fromString(String tag) {
            if (tag.equals("scheduled")) {
                return State.SCHEDULED;
            }
            else if (tag.equals("created")) {
                return State.CREATED;
            }
            else if (tag.equals("connected")) {
                return State.CONNECTED;
            }
            else if (tag.equals("destroyed")) {
                return State.DESTROYED;
            }
            return State.ERROR;
        }
    };
    
    public Long id = null;
    public Long slaveId = null;
    public String slaveName = null;
    public String sessionKey = null;
    public String name = null;
    public State state = null;
    public String address = null;
}
