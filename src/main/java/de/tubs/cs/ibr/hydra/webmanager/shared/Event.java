package de.tubs.cs.ibr.hydra.webmanager.shared;

public class Event {
    public enum EventType {
        NONE("none"),
        SESSION_STATE_CHANGED("session_state_changed"),
        SLAVE_CONNECTED("slave_connected"),
        SLAVE_DISCONNECTED("slave_disconnected");
        
        private String mCode = null; 
        
        EventType(String code) {
            mCode = code;
        }
        
        public String getCode() {
            return mCode;
        }

        @Override
        public String toString() {
            return mCode;
        }
        
        public static EventType fromString(String data) {
            if (SESSION_STATE_CHANGED.getCode().equals(data)) {
                return SESSION_STATE_CHANGED;
            }
            else if (SLAVE_CONNECTED.getCode().equals(data)) {
                return SLAVE_CONNECTED;
            }
            else if (SLAVE_DISCONNECTED.getCode().equals(data)) {
                return SLAVE_DISCONNECTED;
            }
            
            return NONE;
        }
    };
    
    private EventType mType = EventType.NONE;
    
    public static Event decode(String data) {
        return new Event(EventType.fromString(data));
    }
    
    public static String encode(Event evt) {
        return evt.mType.toString();
    }
    
    public Event(EventType t) {
        mType = t;
    }

    public EventType getType() {
        return mType;
    }

    public void setType(EventType type) {
        mType = type;
    }

    @Override
    public String toString() {
        return mType.toString();
    }
}
