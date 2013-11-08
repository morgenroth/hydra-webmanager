package de.tubs.cs.ibr.hydra.webmanager.client.stats;

import com.google.gwt.core.client.JavaScriptObject;

public class DtndInfoJso extends JavaScriptObject {
    protected DtndInfoJso() { }
    
    public final native int getNeighbors() /*-{ return this.Neighbors; }-*/;
    public final native int getStorageSize() /*-{ return this["Storage-size"]; }-*/;
    public final native int getUptime() /*-{ return this.Uptime; }-*/;
}
