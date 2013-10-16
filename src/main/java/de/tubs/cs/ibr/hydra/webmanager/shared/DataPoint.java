package de.tubs.cs.ibr.hydra.webmanager.shared;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;

public class DataPoint implements Serializable {

    /**
     * serial ID
     */
    private static final long serialVersionUID = 3576056043026632672L;
    
    public Date time = null;
    public Coordinates coord = null;
    
    public static class BundleStats implements Serializable {
        /**
         * serial ID
         */
        private static final long serialVersionUID = 6829702738763071921L;
        
        public long received = 0L;
        public long generated = 0L;
        public long transmitted = 0L;
        public long aborted = 0L;
        public long requeued = 0L;
        public long expired = 0L;
        public long queued = 0L;
        public long stored = 0L;
    };
    public BundleStats bundlestats = new BundleStats();
    
    public static class DtnInfo implements Serializable {
        /**
         * serial ID
         */
        private static final long serialVersionUID = 8242728632238027848L;
        
        public long neighbors = 0L;
        public long uptime = 0L;
        public long storage_size = 0L;
    };
    public DtnInfo dtninfo = new DtnInfo();
    
    public static class IfaceStats implements Serializable {
        /**
         * serial ID
         */
        private static final long serialVersionUID = -2802269830343751039L;
        
        public String name = null;
        public long tx = 0L;
        public long rx = 0L;
    };
    public HashMap<String, IfaceStats> ifaces = new HashMap<String, IfaceStats>();
    
    public static class ClockStats implements Serializable {
        /**
         * serial ID
         */
        private static final long serialVersionUID = 5232523728158269835L;
        
        public double delay = 0.0;
        public double offset = 0.0;
        public long timex_tick = 0L;
        public long timex_offset = 0L;
        public long timex_freq = 0L;
    };
    public ClockStats clock = new ClockStats();
}
