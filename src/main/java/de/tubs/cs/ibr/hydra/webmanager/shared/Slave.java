package de.tubs.cs.ibr.hydra.webmanager.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

public class Slave implements IsSerializable {
    
    public enum State {
        DISCONNECTED("disconnected"),
        CONNECTED("connected");
        
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
            if (DISCONNECTED.equals(tag)) {
                return State.DISCONNECTED;
            }
            return State.CONNECTED;
        }
    };

    public Long id = null;
    public String name = null;
    public String address = null;
    public State state = State.DISCONNECTED;
    public String session = null;
    public Long owner = null;
    public Long capacity = null;
    
    public Slave() {
    }
    
    public Slave(Long id) {
        this.id = id;
    }
    
    public Slave(String name, Long owner, Long capacity) {
        this.name = name;
        this.owner = owner;
        this.capacity = capacity;
    }

    @Override
    public String toString() {
        if (this.name != null) return this.name;
        return super.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Slave) {
            Slave s = (Slave)obj;
            if ((s.id != null) && (id != null)) return s.id == id;
            return (s.name == name) && (s.address == address);
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        if (id == null) {
            return (name.hashCode() ^ address.hashCode());
        } else {
            return id.hashCode();
        }
    }
}
