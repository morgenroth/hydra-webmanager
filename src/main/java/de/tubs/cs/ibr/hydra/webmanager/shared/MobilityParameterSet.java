package de.tubs.cs.ibr.hydra.webmanager.shared;

import java.util.HashMap;

import com.google.gwt.user.client.rpc.IsSerializable;


public class MobilityParameterSet implements IsSerializable {
    
    public enum MobilityModel {
        RANDOM_WALK("randomwalk"),
        TRACE("trace"),
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
            if (TRACE.toString().equals(tag)) {
                return TRACE;
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
