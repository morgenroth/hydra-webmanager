package de.tubs.cs.ibr.hydra.webmanager.client.stats;

import com.google.gwt.core.client.JavaScriptObject;

public class DtndTimeSyncJso extends JavaScriptObject {
    protected DtndTimeSyncJso() { }
    
    public final native int getAdjusted() /*-{ return this.Adjusted; }-*/;
    public final native double getOffset() /*-{ return this.Offset; }-*/;
    public final native double getRating() /*-{ return this.Rating; }-*/;
    public final native int getTimestamp() /*-{ return this.Timestamp; }-*/;
}
