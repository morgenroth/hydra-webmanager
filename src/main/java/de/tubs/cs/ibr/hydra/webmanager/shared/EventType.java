package de.tubs.cs.ibr.hydra.webmanager.shared;


public enum EventType {
    NONE("none"),
    SESSION_STATE_CHANGED("session_state_changed"),
    SESSION_ADDED("session_added"),
    SESSION_REMOVED("session_removed"),
    SLAVE_CONNECTED("slave_connected"),
    SLAVE_DISCONNECTED("slave_disconnected"),
    NODE_STATE_CHANGED("node_state_changed");
    
    public static final String EXTRA_SESSION_ID = "session_id";
    public static final String EXTRA_NEW_STATE = "new_state";
    
    public static final String EXTRA_SLAVE_NAME = "slave_name";
    public static final String EXTRA_SLAVE_ADDRESS = "slave_address";
    
    private String mCode = null;
    
    EventType(String code) {
        mCode = code;
    }
    
    public String getCode() {
        return mCode;
    }
    
    public boolean equals(Event evt) {
        if (evt == null) return false;
        return this.equals(evt.getType());
    }

    @Override
    public String toString() {
        return mCode;
    }
    
    public static EventType fromCode(String data) {
        if (SESSION_STATE_CHANGED.getCode().equals(data)) {
            return SESSION_STATE_CHANGED;
        }
        else if (SLAVE_CONNECTED.getCode().equals(data)) {
            return SLAVE_CONNECTED;
        }
        else if (SLAVE_DISCONNECTED.getCode().equals(data)) {
            return SLAVE_DISCONNECTED;
        }
        else if (NODE_STATE_CHANGED.getCode().equals(data)) {
            return NODE_STATE_CHANGED;
        }
        else if (SESSION_REMOVED.getCode().equals(data)) {
            return SESSION_REMOVED;
        }
        else if (SESSION_ADDED.getCode().equals(data)) {
            return SESSION_ADDED;
        }
        
        return NONE;
    }
}
