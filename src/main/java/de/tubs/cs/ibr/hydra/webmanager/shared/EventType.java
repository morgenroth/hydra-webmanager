package de.tubs.cs.ibr.hydra.webmanager.shared;


public enum EventType {
    NONE("none"),
    SESSION_DATA_UPDATED("session_data_updated"),
    SESSION_STATE_CHANGED("session_state_changed"),
    SESSION_ADDED("session_added"),
    SESSION_REMOVED("session_removed"),
    SESSION_STATS_UPDATED("session_data_updated"),
    SLAVE_STATE_CHANGED("slave_state_changed"),
    NODE_STATE_CHANGED("node_state_changed");
    
    public static final String EXTRA_SESSION_ID = "session_id";
    public static final String EXTRA_NEW_STATE = "new_state";
    
    public static final String EXTRA_SLAVE_ID = "slave_id";
    public static final String EXTRA_SLAVE_STATE = "slave_state";
    
    public static final String EXTRA_NODE_ID = "node_id";
    public static final String EXTRA_NODE_STATE = "node_state";
    
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
        else if (SLAVE_STATE_CHANGED.getCode().equals(data)) {
            return SLAVE_STATE_CHANGED;
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
        else if (SESSION_DATA_UPDATED.getCode().equals(data)) {
            return SESSION_DATA_UPDATED;
        }
        else if (SESSION_STATS_UPDATED.getCode().equals(data)) {
            return SESSION_STATS_UPDATED;
        }
        
        return NONE;
    }
}
