package de.tubs.cs.ibr.hydra.webmanager.shared;

import java.util.Date;

import com.google.gwt.user.client.rpc.IsSerializable;

public class DataPoint implements IsSerializable {
    
    public DataPoint() {
    }
    
    public Long node = null;
    public Date time = null;
    public String json = null;
}
