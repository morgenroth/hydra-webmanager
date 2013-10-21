package de.tubs.cs.ibr.hydra.webmanager.shared;


public enum EventType {
    NONE("none"),
    SESSION_DATA_UPDATED("session_data_updated"),
    SESSION_STATE_CHANGED("session_state_changed"),
    SESSION_ADDED("session_added"),
    SESSION_REMOVED("session_removed"),
    SESSION_STATS_UPDATED("session_data_updated"),
    SLAVE_STATE_CHANGED("slave_state_changed"),
    NODE_STATE_CHANGED("node_state_changed"),
    SESSION_LINK_UP("session_link_up"),
    SESSION_LINK_DOWN("session_link_down"),
    SESSION_NODE_MOVED("session_node_moved");
    
    public static final String EXTRA_SESSION_ID = "session_id";
    public static final String EXTRA_NEW_STATE = "new_state";
    
    public static final String EXTRA_SLAVE_ID = "slave_id";
    public static final String EXTRA_SLAVE_STATE = "slave_state";
    
    public static final String EXTRA_NODE_ID = "node_id";
    public static final String EXTRA_NODE_STATE = "node_state";
    
    public static final String EXTRA_LINK_SOURCE_ID = "link_source_id";
    public static final String EXTRA_LINK_TARGET_ID = "link_target_id";
    
    public static final String EXTRA_POSITION_X = "position_x";
    public static final String EXTRA_POSITION_Y = "position_y";
    
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
        else if (SESSION_LINK_UP.getCode().equals(data)) {
            return SESSION_LINK_UP;
        }
        else if (SESSION_LINK_DOWN.getCode().equals(data)) {
            return SESSION_LINK_DOWN;
        }
        else if (SESSION_NODE_MOVED.getCode().equals(data)) {
            return SESSION_NODE_MOVED;
        }
        
        return NONE;
    }
}
