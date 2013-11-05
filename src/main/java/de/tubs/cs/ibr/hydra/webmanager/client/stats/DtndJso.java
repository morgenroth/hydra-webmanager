package de.tubs.cs.ibr.hydra.webmanager.client.stats;

import com.google.gwt.core.client.JavaScriptObject;

public class DtndJso extends JavaScriptObject {
    protected DtndJso() { }
    
    public final native DtndBundlesJso getBundles() /*-{ return this.bundles; }-*/;
    public final native DtndInfoJso getInfo() /*-{ return this.info; }-*/;
    public final native DtndTimeSyncJso getTimeSync() /*-{ return this.timesync; }-*/;
}
