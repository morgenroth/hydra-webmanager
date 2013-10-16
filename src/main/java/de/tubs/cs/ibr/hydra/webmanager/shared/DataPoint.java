package de.tubs.cs.ibr.hydra.webmanager.shared;

import java.util.Date;
import java.util.HashMap;

import com.google.gwt.user.client.rpc.IsSerializable;

public class DataPoint implements IsSerializable {
    
    public DataPoint() {
    }
    
    public Date time = null;
    public Coordinates coord = null;
    
    public BundleStats bundlestats = new BundleStats();
    public DtnInfoStats dtninfo = new DtnInfoStats();
    public HashMap<String, InterfaceStats> ifaces = new HashMap<String, InterfaceStats>();
    public ClockStats clock = new ClockStats();
    
    public static final class InterfaceStats implements IsSerializable {
        public InterfaceStats() {
        }
        
        public String name = null;
        public long tx = 0L;
        public long rx = 0L;
    }
    
    public static final class DtnInfoStats implements IsSerializable {
        public DtnInfoStats() {
        }
        
        public long neighbors = 0L;
        public long uptime = 0L;
        public long storage_size = 0L;
    }

    public static final class ClockStats implements IsSerializable {
        public ClockStats() {
        }
        
        public double delay = 0.0;
        public double offset = 0.0;
        public long timex_tick = 0L;
        public long timex_offset = 0L;
        public long timex_freq = 0L;
    }
    
    public static final class BundleStats implements IsSerializable {
        public BundleStats() {
        }
        
        public long received = 0L;
        public long generated = 0L;
        public long transmitted = 0L;
        public long aborted = 0L;
        public long requeued = 0L;
        public long expired = 0L;
        public long queued = 0L;
        public long stored = 0L;
    }
}
