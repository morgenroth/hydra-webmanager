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
        QUEUE("Queue"),
        REMOVE("Remove");
        
        private String mValue;
        
        private Action(String value) {
            mValue = value;
        }
        
        @Override
        public String toString() {
            return mValue;
        }
        
        public boolean equals(String value) {
            return mValue.equals(value);
        }
        
        public static Action fromString(String value) {
            if (QUEUE.equals(value)) {
                return Action.QUEUE;
            }
            else if (RESET.equals(value)) {
                return Action.RESET;
            }
            else if (REMOVE.equals(value)) {
                return Action.REMOVE;
            }
            return Action.ABORT;
        }
    };

    public enum State {
        INITIAL("initial"),
        DRAFT("draft"),
        PENDING("pending"),
        RUNNING("running"),
        FINISHED("finished"),
        ABORTED("aborted"),
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
        
        public boolean equals(String value) {
            return mTag.equals(value);
        }
        
        public static State fromString(String tag) {
            if (PENDING.equals(tag)) {
                return State.PENDING;
            }
            else if (RUNNING.equals(tag)) {
                return State.RUNNING;
            }
            else if (FINISHED.equals(tag)) {
                return State.FINISHED;
            }
            else if (ABORTED.equals(tag)) {
                return State.ABORTED;
            }
            else if (INITIAL.equals(tag)) {
                return State.INITIAL;
            }
            else if (ERROR.equals(tag)) {
                return State.ERROR;
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
    
    // base settings
    public String image = null;
    public String repository = null;
    public String packages = null;
    public String monitor_nodes = null;
    public String qemu_template = null;
    public String vbox_template = null;
    
    // movement settings
    

    public Session() {
    }
}
