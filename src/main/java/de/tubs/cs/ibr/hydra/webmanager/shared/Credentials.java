package de.tubs.cs.ibr.hydra.webmanager.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

public class Credentials implements IsSerializable {
    private String mUsername = null;
    private String mSessionId= null;
    private long mSessionExpires = 0;
    
    public Credentials() {
    }

    public String getUsername() {
        return mUsername;
    }

    public void setUsername(String username) {
        mUsername = username;
    }

    public String getSessionId() {
        return mSessionId;
    }

    public void setSessionId(String sid) {
        mSessionId = sid;
    }

    public long getSessionExpires() {
        return mSessionExpires;
    }

    public void setSessionExpires(long expires) {
        mSessionExpires = expires;
    }
}
