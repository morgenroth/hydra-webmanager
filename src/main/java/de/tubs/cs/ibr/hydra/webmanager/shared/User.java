package de.tubs.cs.ibr.hydra.webmanager.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

public class User implements IsSerializable {
    public Long id = null;
    public String name = null;
    
    public User(Long id) {
        this.id = id;
    }
}
