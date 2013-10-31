package de.tubs.cs.ibr.hydra.webmanager.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

public class DataFile implements IsSerializable, Comparable<DataFile> {
    public Long sessionId = null;
    public String filename = null;
    public long modified = 0L;
    public long size = 0L;
    
    public DataFile() {
    }
    
    public DataFile(Long sessionId) {
        this.sessionId = sessionId;
    }
    
    @Override
    public int compareTo(DataFile o) {
        return filename.compareTo(o.filename);
    }
}
