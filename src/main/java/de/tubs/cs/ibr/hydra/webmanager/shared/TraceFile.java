package de.tubs.cs.ibr.hydra.webmanager.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

public class TraceFile implements IsSerializable, Comparable<TraceFile> {
    public Long sessionId = null;
    public String filename = null;
    public long modified = 0L;
    public long size = 0L;
    
    public TraceFile() {
    }
    
    public TraceFile(Long sessionId) {
        this.sessionId = sessionId;
    }
    
    @Override
    public int compareTo(TraceFile o) {
        return filename.compareTo(o.filename);
    }
}
