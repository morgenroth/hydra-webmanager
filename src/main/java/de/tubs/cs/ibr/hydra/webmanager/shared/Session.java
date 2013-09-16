package de.tubs.cs.ibr.hydra.webmanager.shared;

import java.io.Serializable;
import java.util.Date;

public class Session implements Serializable {
    
    /**
     * Serial ID
     */
    private static final long serialVersionUID = -3798279065466924743L;

    public enum State {
        IDLE,
        RUNNING,
        ABORTED
    };
    
    public Long id = null;
    public Long userid;
    public String username;
    public String name;
    public State state;
    public Date created;

    public Session() {
    }
}
