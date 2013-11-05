package de.tubs.cs.ibr.hydra.webmanager.client.stats;

import com.google.gwt.core.client.JavaScriptObject;

public class DtndBundlesJso extends JavaScriptObject {
    protected DtndBundlesJso() { }
    
    public final native int getAborted() /*-{ return this.Aborted; }-*/;
    public final native int getExpired() /*-{ return this.Expired; }-*/;
    public final native int getGenerated() /*-{ return this.Generated; }-*/;
    public final native int getQueued() /*-{ return this.Queued; }-*/;
    public final native int getReceived() /*-{ return this.Received; }-*/;
    public final native int getRequeued() /*-{ return this.Requeued; }-*/;
    public final native int getStored() /*-{ return this.Stored; }-*/;
    public final native int getTransmitted() /*-{ return this.Transmitted; }-*/;
}
