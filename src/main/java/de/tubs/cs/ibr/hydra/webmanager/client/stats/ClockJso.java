package de.tubs.cs.ibr.hydra.webmanager.client.stats;

import com.google.gwt.core.client.JavaScriptObject;

public class ClockJso extends JavaScriptObject {
    protected ClockJso() { }
    
    public final native double getDelay() /*-{ return this.Delay; }-*/;
    public final native double getOffset() /*-{ return this.Offset; }-*/;
    public final native int getTimexFreq() /*-{ return this["Timex-freq"]; }-*/;
    public final native int getTimexOffset() /*-{ return this["Timex-offset"]; }-*/;
    public final native int getTimexTick() /*-{ return this["Timex-tick"]; }-*/;
}
