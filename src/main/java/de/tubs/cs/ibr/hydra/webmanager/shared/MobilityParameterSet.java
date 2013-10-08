package de.tubs.cs.ibr.hydra.webmanager.shared;

import java.io.Serializable;
import java.util.HashMap;


public class MobilityParameterSet implements Serializable {

    /**
     * serial ID
     */
    private static final long serialVersionUID = 4299944841566525510L;
    
    public enum MobilityModel {
        RANDOM_WALK("randomwalk"),
        THE_ONE("theone"),
        STATIC("static"),
        NONE("none");
        
        private String tag = null;
        
        @Override
        public String toString() {
            return tag;
        }
        
        private MobilityModel(String tag) {
            this.tag = tag;
        }
        
        public static MobilityModel fromString(String tag) {
            if (RANDOM_WALK.toString().equals(tag)) {
                return RANDOM_WALK;
            }
            if (THE_ONE.toString().equals(tag)) {
                return THE_ONE;
            }
            if (STATIC.toString().equals(tag)) {
                return STATIC;
            }
            return NONE;
        }
    }

    public MobilityModel model = null;
    public HashMap<String, String> parameters = new HashMap<String, String>();
}
