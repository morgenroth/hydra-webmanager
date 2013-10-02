package de.tubs.cs.ibr.hydra.webmanager.shared;


public enum EventType {
    NONE("none"),
    SESSION_STATE_CHANGED("session_state_changed"),
    SLAVE_CONNECTED("slave_connected"),
    SLAVE_DISCONNECTED("slave_disconnected"),
    NODE_STATE_CHANGED("node_state_changed");
    
    private String mCode = null; 
    
    EventType(String code) {
        mCode = code;
    }
    
    public String getCode() {
        return mCode;
    }
    
    public boolean equals(Event evt) {
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
        
        return NONE;
    }
}
