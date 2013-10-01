package de.tubs.cs.ibr.hydra.webmanager.shared;

import java.io.Serializable;
import java.util.Date;

public class Session implements Serializable {
    
    /**
     * Serial ID
     */
    private static final long serialVersionUID = -3798279065466924743L;
    
    public enum Action {
        ABORT("Abort"),
        RESET("Reset"),
        QUEUE("Queue");
        
        private String mValue;
        
        private Action(String value) {
            mValue = value;
        }
        
        @Override
        public String toString() {
            return mValue;
        }
        
        public static Action fromString(String value) {
            if (value.equals("Queue")) {
                return Action.QUEUE;
            }
            else if (value.equals("Reset")) {
                return Action.RESET;
            }
            return Action.ABORT;
        }
    };

    public enum State {
        DRAFT("draft"),
        PENDING("pending"),
        RUNNING("running"),
        FINISHED("finished"),
        ABORTED("aborted");
        
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
            if (tag.equals("pending")) {
                return State.PENDING;
            }
            else if (tag.equals("running")) {
                return State.RUNNING;
            }
            else if (tag.equals("finished")) {
                return State.FINISHED;
            }
            else if (tag.equals("aborted")) {
                return State.ABORTED;
            }
            return State.DRAFT;
        }
    };
    
    public Long id = null;
    public Long userid = null;
    public String username = null;
    public String name = null;
    public State state = null;
    
    public Date created = null;
    public Date started = null;
    public Date aborted = null;
    public Date finished = null;

    public Session() {
    }
}
